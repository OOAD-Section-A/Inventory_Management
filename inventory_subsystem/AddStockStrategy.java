package inventory_subsystem;

import java.time.LocalDateTime;
import java.util.UUID;

public class AddStockStrategy implements StockOperationStrategy {

    @Override
    public void execute(String productId, String locationId,
                        String supplierId, int quantity,
                        String referenceType, String referenceId,
                        InventoryDataStore repository,
                        InventoryExceptionSource exceptionSource,
                        IssuingPolicy policy) {

        if (!"GRN".equals(referenceType)) {
            exceptionSource.fireConflict(
                110,
                "StockCreation",
                productId,
                "Stock must originate from GRN"
            );
            return;
        }

        InventoryItem item = repository.find(productId, locationId);

        if (item == null) {
            item = new InventoryItem(productId, locationId);
        }

        InventoryBatch batch = new InventoryBatch(
                UUID.randomUUID().toString(),
                productId,
                supplierId,
                quantity,
                LocalDateTime.now(),
                null,
                0.0
        );

        item.addBatch(batch);
        item.setVersion(item.getVersion() + 1);

        if (repository instanceof InventoryRepository repo) {
            repo.recordTransaction(
                    new StockTransaction(
                            productId,
                            batch.getBatchId(),
                            locationId,
                            quantity,
                            "ADD",
                            referenceType,
                            referenceId
                    )
            );
        }

        repository.save(item);
    }
}
