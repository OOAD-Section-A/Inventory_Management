package inventory_subsystem;

import java.time.LocalDateTime;
import java.util.UUID;

public class TransferStockStrategy implements StockOperationStrategy {

    @Override
    public void execute(String productId, String fromLocation,
                        int quantity,
                        InventoryDataStore repository,
                        InventoryExceptionSource exceptionSource,
                        IssuingPolicy policy) {

        InventoryItem source = repository.find(productId, fromLocation);
        if (source == null) return;

        source.deductQuantity(quantity);

        if (repository instanceof InventoryRepository repo) {
            repo.recordTransaction(
                new StockTransaction(
                    productId,
                    null,
                    fromLocation,
                    -quantity,
                    "TRANSFER_OUT",
                    "TRANSFER",
                    "AUTO"
                )
            );
        }

        repository.save(source);
    }

    public void addToDestination(String productId, String toLocation,
                                 int quantity,
                                 InventoryDataStore repository) {

        InventoryItem dest = repository.find(productId, toLocation);
        if (dest == null) dest = new InventoryItem(productId, toLocation);

        InventoryBatch batch = new InventoryBatch(
                UUID.randomUUID().toString(),
                productId,
                "INTERNAL_TRANSFER",
                quantity,
                LocalDateTime.now(),
                null,
                0.0
        );

        dest.addBatch(batch);

        if (repository instanceof InventoryRepository repo) {
            repo.recordTransaction(
                new StockTransaction(
                    productId,
                    batch.getBatchId(),
                    toLocation,
                    quantity,
                    "TRANSFER_IN",
                    "TRANSFER",
                    "AUTO"
                )
            );
        }

        repository.save(dest);
    }
}
