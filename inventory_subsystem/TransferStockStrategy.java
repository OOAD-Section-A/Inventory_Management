package inventory_subsystem;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;

public class TransferStockStrategy {

    public void executeTransfer(String sku,
                                String fromLocation,
                                String toLocation,
                                int quantity,
                                InventoryDataStore repository,
                                InventoryExceptionSource exceptionSource,
                                IssuingPolicy policy) {

        InventoryItem source = repository.find(sku, fromLocation);

        if (source == null) {
            exceptionSource.fireResourceNotFound(
                    166, "InventoryItem", sku + "_" + fromLocation);
            return;
        }

        source.getBatches().removeIf(
                b -> b.getExpiryTime() != null &&
                     b.getExpiryTime().isBefore(LocalDateTime.now())
        );

        if (source.getTotalQuantity() < quantity) {
            exceptionSource.fireResourceExhausted(
                    167, "Stock", sku + "_" + fromLocation,
                    quantity, source.getTotalQuantity());
            return;
        }

        if (policy == IssuingPolicy.FEFO) {
            source.getBatches().sort(
                    Comparator.comparing(
                            b -> b.getExpiryTime() != null
                                    ? b.getExpiryTime()
                                    : b.getArrivalTime()
                    )
            );
        } else {
            source.getBatches().sort(
                    Comparator.comparing(InventoryBatch::getArrivalTime)
            );
        }

        int remaining = quantity;

        Iterator<InventoryBatch> iterator = source.getBatches().iterator();

        while (iterator.hasNext() && remaining > 0) {
            InventoryBatch batch = iterator.next();
            int deduct = Math.min(batch.getQuantity(), remaining);

            batch.setQuantity(batch.getQuantity() - deduct);
            remaining -= deduct;

            if (batch.getQuantity() == 0) {
                iterator.remove();
            }
        }

        InventoryItem destination = repository.find(sku, toLocation);

        if (destination == null) {
            destination = new InventoryItem(sku, toLocation, 0);
        }

        destination.addBatch(new InventoryBatch(
                quantity,
                LocalDateTime.now(),
                null
        ));

        source.setVersion(source.getVersion() + 1);
        destination.setVersion(destination.getVersion() + 1);

        repository.save(source);
        repository.save(destination);
    }
}