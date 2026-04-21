package inventory_subsystem;

import com.scm.subsystems.InventorySubsystem;

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

        if (item.getAvailableQuantity() < quantity) {
            exceptions.onInsufficientStock(productId, quantity, item.getAvailableQuantity());
            return;
        }

        // remove expired
        item.getBatches().removeIf(
            b -> b.getExpiryTime() != null &&
                 b.getExpiryTime().isBefore(LocalDateTime.now())
        );

        item.recomputeTotalQuantity();

        // sort
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

        for (InventoryBatch batch : item.getBatches()) {
            if (remaining <= 0) break;

            int deduct = Math.min(batch.getQuantity(), remaining);
            batch.setQuantity(batch.getQuantity() - deduct);
            remaining -= deduct;

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

        item.getBatches().removeIf(b -> b.getQuantity() == 0);

        item.recomputeTotalQuantity();

        repository.save(item);
    }
}
