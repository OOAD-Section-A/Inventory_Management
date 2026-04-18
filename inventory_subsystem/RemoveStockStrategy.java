package inventory_subsystem;

import java.util.*;

public class RemoveStockStrategy implements StockOperationStrategy {

    @Override
    public void execute(String sku, String locationId, int quantity,
                        InventoryDataStore repository,
                        InventoryExceptionSource exceptionSource,
                        IssuingPolicy policy) {

        InventoryItem item = repository.find(sku, locationId);

        if (item == null) {
            exceptionSource.fireResourceNotFound(
                    166, "InventoryItem", sku + "_" + locationId);
            return;
        }

        item.getBatches().removeIf(
                b -> b.getExpiryTime() != null &&
                     b.getExpiryTime().isBefore(java.time.LocalDateTime.now())
        );

        if (item.getTotalQuantity() < quantity) {
            exceptionSource.fireResourceExhausted(
                    167, "Stock", sku + "_" + locationId,
                    quantity, item.getTotalQuantity());
            return;
        }

        InventoryItem latest = repository.find(sku, locationId);

        if (latest.getVersion() != item.getVersion()) {
            exceptionSource.fireConflict(
                    110, "InventoryItem",
                    sku + "_" + locationId,
                    "Concurrent update detected");
            return;
        }

        if (policy == IssuingPolicy.FEFO) {
            item.getBatches().sort(
                    Comparator.comparing(
                            b -> b.getExpiryTime() != null
                                    ? b.getExpiryTime()
                                    : b.getArrivalTime()
                    )
            );
        } else {
            item.getBatches().sort(
                    Comparator.comparing(InventoryBatch::getArrivalTime)
            );
        }

        int remaining = quantity;

        Iterator<InventoryBatch> iterator = item.getBatches().iterator();

        while (iterator.hasNext() && remaining > 0) {

            InventoryBatch batch = iterator.next();

            int available = batch.getQuantity();
            int deduct = Math.min(available, remaining);

            batch.setQuantity(available - deduct);
            remaining -= deduct;

            if (batch.getQuantity() == 0) {
                iterator.remove();
            }
        }

        item.setVersion(item.getVersion() + 1);

        repository.save(item);
    }
}