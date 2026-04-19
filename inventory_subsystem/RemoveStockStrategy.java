package inventory_subsystem;

import java.time.LocalDateTime;
import java.util.*;

public class RemoveStockStrategy implements StockOperationStrategy {

    @Override
    public void execute(String productId, String locationId,
                        int quantity,
                        InventoryDataStore repository,
                        InventoryExceptionSource exceptionSource,
                        IssuingPolicy policy) {

        InventoryItem item = repository.find(productId, locationId);

        if (item == null) return;

        if (item.getTotalQuantity() < quantity) {
            exceptionSource.fireResourceExhausted(
                167,
                "Stock",
                productId + "_" + locationId,
                quantity,
                item.getTotalQuantity()
            );
            return;
        }

        item.getBatches().removeIf(
            b -> b.getExpiryTime() != null &&
                b.getExpiryTime().isBefore(LocalDateTime.now())
        );

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

            item.deductQuantity(deduct);
        }

        item.getBatches().removeIf(b -> b.getQuantity() == 0);

        if (repository instanceof InventoryRepository repo) {
            repo.recordTransaction(
                new StockTransaction(
                    productId,
                    null,
                    locationId,
                    -quantity,
                    "REMOVE",
                    "ORDER",
                    "AUTO"
                )
            );
        }

        repository.save(item);
    }
}
