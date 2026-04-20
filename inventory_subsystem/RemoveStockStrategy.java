package inventory_subsystem;

import com.scm.exceptions.InventorySubsystem;

import java.time.LocalDateTime;
import java.util.*;

public class RemoveStockStrategy implements StockOperationStrategy {

    private final InventorySubsystem exceptions = InventorySubsystem.INSTANCE;

    @Override
    public void execute(String productId, String locationId,
                        int quantity,
                        InventoryDataStore repository,
                        IssuingPolicy policy) {

        InventoryItem item = repository.find(productId, locationId);

        if (item == null) {
            exceptions.onItemNotFound(productId);
            return;
        }

        // Validate if sufficient quantity is available
        if (item.getTotalQuantity() < quantity) {
            exceptions.onInsufficientStock(productId, quantity, item.getTotalQuantity());
            return;
        }

        // Before removing the stock, automatically clean up expired batches
        item.getBatches().removeIf(
            b -> b.getExpiryTime() != null &&
                b.getExpiryTime().isBefore(LocalDateTime.now())
        );

        // Sort the batches based on which should be consumed first
        item.getBatches().sort(
            policy == IssuingPolicy.FEFO
                ? Comparator.comparing(
                    b -> b.getExpiryTime() != null
                        ? b.getExpiryTime()
                        : b.getArrivalTime()
                )
                : Comparator.comparing(InventoryBatch::getArrivalTime)
        );

        int remaining = quantity;

        // Loop through sorted batches and deduct quantity one batch at 
        // a time until the required quantity is fulfilled.
        for (InventoryBatch batch : item.getBatches()) {
            if (remaining <= 0) break;

            int deduct = Math.min(batch.getQuantity(), remaining);
            batch.setQuantity(batch.getQuantity() - deduct);
            remaining -= deduct;

            item.deductQuantity(deduct);

            if (repository instanceof InventoryRepository repo) {
                repo.recordTransaction(
                    new StockTransaction(
                        productId,
                        batch.getBatchId(),
                        locationId,
                        -deduct,
                        "REMOVE",
                        "ORDER",
                        "AUTO"
                    )
                );
            }
        }

        // clean up empty batches
        item.getBatches().removeIf(b -> b.getQuantity() == 0);

        // save to db
        repository.save(item);
    }
}
