package inventory_subsystem;

import com.scm.exceptions.InventorySubsystem;

import java.time.LocalDateTime;
import java.util.UUID;

public class AddStockStrategy {

    private final InventorySubsystem exceptions = InventorySubsystem.INSTANCE;

    public void executeAdd(String productId, String locationId,
                           String supplierId, int quantity,
                           String referenceType, String referenceId,
                           InventoryDataStore repository,
                           IssuingPolicy policy) {
        
// Stock can be added only via a GRN. if its not GRN, the opertaion is rejected immediately
        if (!"GRN".equals(referenceType)) {                       
            exceptions.onStockUpdateConflict(productId);
            return;
        }

        InventoryItem item = repository.find(productId, locationId);
        
// Check if stock already exists at that location. If yes, use it. If no, create a fresh one
        if (item == null) {
            item = new InventoryItem(productId, locationId);
        }

// Create a new batch
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
