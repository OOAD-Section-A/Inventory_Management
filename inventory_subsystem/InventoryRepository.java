package inventory_subsystem;

import com.jackfruit.scm.database.adapter.InventoryAdapter;
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.scm.core.Severity;
import com.scm.factory.SCMExceptionFactory;
import com.scm.handler.SCMExceptionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class InventoryRepository implements InventoryDataStore {

    private SupplyChainDatabaseFacade facade;
    private InventoryAdapter adapter;
    private final List<StockTransaction> pendingTransactions = new ArrayList<>();

    @Override
    public InventoryItem find(String productId, String locationId) {
        try {
            com.jackfruit.scm.database.model.InventoryItem dbItem =
                    inventoryAdapter().getInventoryItem(productId, locationId);

            InventoryItem item = fromDatabaseItem(dbItem);
            List<com.jackfruit.scm.database.model.InventoryBatch> dbBatches =
                    inventoryAdapter().findInventoryBatchesByProductAndLocation(productId, locationId);

            for (com.jackfruit.scm.database.model.InventoryBatch dbBatch : dbBatches) {
                if (dbBatch.getQuantity() != null && dbBatch.getQuantity() > 0) {
                    item.getBatches().add(fromDatabaseBatch(dbBatch));
                }
            }

            return item;
        } catch (RuntimeException e) {
            if (isMissingInventoryItem(e, productId, locationId)) {
                return null;
            }

            handleRepositoryException("Inventory repository find failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(InventoryItem item) {
        try {
            com.jackfruit.scm.database.model.InventoryItem dbItem = toDatabaseItem(item);

            if (find(item.getProductId(), item.getLocationId()) == null) {
                inventoryAdapter().createInventoryItem(dbItem);
            } else {
                inventoryAdapter().updateInventoryItem(dbItem);
            }

            Set<String> activeBatchIds = new HashSet<>();
            for (InventoryBatch batch : item.getBatches()) {
                activeBatchIds.add(batch.getBatchId());

                com.jackfruit.scm.database.model.InventoryBatch dbBatch =
                        toDatabaseBatch(batch, item.getLocationId());

                if (databaseBatchExists(batch.getBatchId())) {
                    inventoryAdapter().updateInventoryBatch(dbBatch);
                } else {
                    inventoryAdapter().createInventoryBatch(dbBatch);
                }
            }

            List<com.jackfruit.scm.database.model.InventoryBatch> existingBatches =
                    inventoryAdapter().findInventoryBatchesByProductAndLocation(item.getProductId(), item.getLocationId());

            for (com.jackfruit.scm.database.model.InventoryBatch existingBatch : existingBatches) {
                if (!activeBatchIds.contains(existingBatch.getBatchId())) {
                    existingBatch.setQuantity(0);
                    inventoryAdapter().updateInventoryBatch(existingBatch);
                }
            }

            flushPendingTransactions();
        } catch (RuntimeException e) {
            handleRepositoryException("Inventory repository save failed: " + e.getMessage());
            return;
        }
    }

    public void recordTransaction(StockTransaction tx) {
        try {
            if (canRecordTransaction(tx)) {
                inventoryAdapter().recordStockTransaction(toDatabaseTransaction(tx));
            } else {
                pendingTransactions.add(tx);
            }
        } catch (RuntimeException e) {
            handleRepositoryException("Inventory repository transaction recording failed: " + e.getMessage());
            return;
        }
    }

    private boolean databaseBatchExists(String batchId) {
        try {
            inventoryAdapter().getInventoryBatch(batchId);
            return true;
        } catch (RuntimeException e) {
            if (isMissingInventoryBatch(e, batchId)) {
                return false;
            }
            throw e;
        }
    }

    private boolean canRecordTransaction(StockTransaction tx) {
        try {
            inventoryAdapter().getInventoryItem(tx.getProductId(), tx.getLocationId());
            if (tx.getBatchId() != null) {
                inventoryAdapter().getInventoryBatch(tx.getBatchId());
            }
            return true;
        } catch (RuntimeException e) {
            if (isMissingRecordForTransaction(e, tx)) {
                return false;
            }
            throw e;
        }
    }

    private void flushPendingTransactions() {
        Iterator<StockTransaction> iterator = pendingTransactions.iterator();
        while (iterator.hasNext()) {
            StockTransaction tx = iterator.next();
            if (canRecordTransaction(tx)) {
                inventoryAdapter().recordStockTransaction(toDatabaseTransaction(tx));
                iterator.remove();
            }
        }
    }

    private InventoryAdapter inventoryAdapter() {
        if (adapter == null) {
            facade = new SupplyChainDatabaseFacade();
            adapter = new InventoryAdapter(facade);
        }
        return adapter;
    }

    private InventoryItem fromDatabaseItem(com.jackfruit.scm.database.model.InventoryItem dbItem) {
        InventoryItem item = new InventoryItem(dbItem.getProductId(), dbItem.getLocationId());
        item.setTotalQuantity(valueOrZero(dbItem.getTotalQuantity()));
        item.setReservedQuantity(valueOrZero(dbItem.getReservedQuantity()));
        item.setAbcCategory(dbItem.getAbcCategory() == null ? "C" : dbItem.getAbcCategory().toString());
        item.setReorderThreshold(valueOrZero(dbItem.getReorderThreshold()));
        item.setSafetyStockLevel(valueOrZero(dbItem.getSafetyStockLevel()));
        item.setVersion(valueOrZero(dbItem.getVersion()));
        return item;
    }

    private InventoryBatch fromDatabaseBatch(com.jackfruit.scm.database.model.InventoryBatch dbBatch) {
        return new InventoryBatch(
                dbBatch.getBatchId(),
                dbBatch.getProductId(),
                dbBatch.getSupplierId(),
                valueOrZero(dbBatch.getQuantity()),
                dbBatch.getArrivalTime(),
                dbBatch.getExpiryTime()
        );
    }

    private com.jackfruit.scm.database.model.InventoryItem toDatabaseItem(InventoryItem item) {
        return new com.jackfruit.scm.database.model.InventoryItem(
                item.getProductId(),
                item.getLocationId(),
                item.getTotalQuantity(),
                item.getReservedQuantity(),
                firstAbcCategoryChar(item.getAbcCategory()),
                item.getReorderThreshold(),
                item.getSafetyStockLevel(),
                item.getVersion()
        );
    }

    private com.jackfruit.scm.database.model.InventoryBatch toDatabaseBatch(
            InventoryBatch batch, String locationId) {
        return new com.jackfruit.scm.database.model.InventoryBatch(
                batch.getBatchId(),
                batch.getProductId(),
                locationId,
                batch.getSupplierId(),
                batch.getQuantity(),
                batch.getArrivalTime(),
                batch.getExpiryTime()
        );
    }

    private com.jackfruit.scm.database.model.StockTransaction toDatabaseTransaction(StockTransaction tx) {
        return new com.jackfruit.scm.database.model.StockTransaction(
                UUID.randomUUID().toString(),
                tx.getProductId(),
                tx.getBatchId(),
                tx.getLocationId(),
                tx.getQuantityChange(),
                tx.getType(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                tx.getTimestamp()
        );
    }

    private Character firstAbcCategoryChar(String abcCategory) {
        if (abcCategory == null || abcCategory.isBlank()) {
            return 'C';
        }
        return abcCategory.charAt(0);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isMissingInventoryItem(RuntimeException e, String productId, String locationId) {
        return e.getMessage() != null
                && e.getMessage().contains("InventoryItem not found: " + productId + "@" + locationId);
    }

    private boolean isMissingInventoryBatch(RuntimeException e, String batchId) {
        return e.getMessage() != null
                && e.getMessage().contains("InventoryBatch not found: " + batchId);
    }

    private boolean isMissingRecordForTransaction(RuntimeException e, StockTransaction tx) {
        return e.getMessage() != null
                && (e.getMessage().contains("InventoryItem not found: " + tx.getProductId() + "@" + tx.getLocationId())
                || e.getMessage().contains("InventoryBatch not found: " + tx.getBatchId()));
    }

    private void handleRepositoryException(String message) {
        SCMExceptionHandler.INSTANCE.handle(
                SCMExceptionFactory.create(0, "UNREGISTERED_EXCEPTION",
                        message,
                        "Inventory", Severity.MINOR)
        );
    }
}
