package inventory_subsystem;

import com.scm.exceptions.InventorySubsystem;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

public class TransferStockStrategy {

    private final InventorySubsystem exceptions = InventorySubsystem.INSTANCE;

    public void transfer(String productId, String fromLocation,
                         String toLocation, int quantity,
                         InventoryDataStore repository,
                         IssuingPolicy policy) {

        InventoryItem source = repository.find(productId, fromLocation);
        if (source == null) {
            exceptions.onItemNotFound(productId);
            return;
        }

        if (fromLocation.equals(toLocation)) {
            repository.save(source);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int expiredQuantity = 0;
        Iterator<InventoryBatch> expiryIterator = source.getBatches().iterator();

        while (expiryIterator.hasNext()) {
            InventoryBatch batch = expiryIterator.next();
            if (batch.getExpiryTime() != null && batch.getExpiryTime().isBefore(now)) {
                expiredQuantity += batch.getQuantity();
                expiryIterator.remove();
            }
        }

        if (expiredQuantity > 0) {
            source.deductQuantity(expiredQuantity);
        }

        if (source.getTotalQuantity() < quantity) {
            exceptions.onInsufficientStock(productId, quantity, source.getTotalQuantity());
            return;
        }

        source.getBatches().sort(
            policy == IssuingPolicy.FEFO
                ? Comparator.comparing(
                    batch -> batch.getExpiryTime() != null
                        ? batch.getExpiryTime()
                        : batch.getArrivalTime()
                )
                : Comparator.comparing(InventoryBatch::getArrivalTime)
        );

        InventoryItem dest = repository.find(productId, toLocation);
        if (dest == null) {
            dest = new InventoryItem(productId, toLocation);
        }

        int remaining = quantity;
        Iterator<InventoryBatch> iterator = source.getBatches().iterator();

        while (iterator.hasNext() && remaining > 0) {
            InventoryBatch batch = iterator.next();
            int available = batch.getQuantity();
            int moveQty = Math.min(available, remaining);
            String batchIdForTx;

            if (moveQty == available) {
                iterator.remove();
                dest.addBatch(batch);
                batchIdForTx = batch.getBatchId();
            } else {
                batch.setQuantity(available - moveQty);

                InventoryBatch newBatch = new InventoryBatch(
                    UUID.randomUUID().toString(),
                    batch.getProductId(),
                    batch.getSupplierId(),
                    moveQty,
                    batch.getArrivalTime(),
                    batch.getExpiryTime(),
                    0.0
                );

                dest.addBatch(newBatch);
                batchIdForTx = newBatch.getBatchId();
            }

            remaining -= moveQty;

            if (repository instanceof InventoryRepository repo) {
                repo.recordTransaction(
                    new StockTransaction(
                        productId,
                        batchIdForTx,
                        fromLocation,
                        -moveQty,
                        "TRANSFER_OUT",
                        "TRANSFER",
                        "AUTO"
                    )
                );

                repo.recordTransaction(
                    new StockTransaction(
                        productId,
                        batchIdForTx,
                        toLocation,
                        moveQty,
                        "TRANSFER_IN",
                        "TRANSFER",
                        "AUTO"
                    )
                );
            }
        }

        if (remaining > 0) {
            exceptions.onInsufficientStock(productId, quantity, source.getTotalQuantity());
            return;
        }

        source.setVersion(source.getVersion() + 1);
        dest.setVersion(dest.getVersion() + 1);

        repository.save(source);
        repository.save(dest);
    }
}
