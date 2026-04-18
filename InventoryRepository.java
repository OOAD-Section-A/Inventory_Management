package inventory_subsystem;

import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.adapter.InventoryAdapter;
import com.jackfruit.scm.database.model.InventoryModels.Product;
import com.jackfruit.scm.database.model.InventoryModels.StockLevel;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class InventoryRepository implements InventoryDataStore {

    private final SupplyChainDatabaseFacade facade;
    private final InventoryAdapter adapter;

    private final Map<String, InventoryItem> localStore = new HashMap<>();

    public InventoryRepository() {
        this.facade = new SupplyChainDatabaseFacade();
        this.adapter = new InventoryAdapter(facade);
    }

    private String key(String sku, String locationId) {
        return sku + "_" + locationId;
    }

    @Override
    public InventoryItem find(String sku, String locationId) {
        return localStore.get(key(sku, locationId));
    }

    @Override
    public void save(InventoryItem item) {

        localStore.put(key(item.getSku(), item.getLocationId()), item);

        Product product = adapter.listProducts()
                .stream()
                .filter(p -> p.sku().equals(item.getSku()))
                .findFirst()
                .orElse(null);

        if (product == null) return;

        int totalQty = localStore.values().stream()
                .filter(i -> i.getSku().equals(item.getSku()))
                .mapToInt(InventoryItem::getTotalQuantity)
                .sum();

        StockLevel existing = adapter.listStockLevels()
                .stream()
                .filter(s -> s.productId().equals(product.productId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            StockLevel newStock = new StockLevel(
                    java.util.UUID.randomUUID().toString(),
                    product.productId(),
                    totalQty,
                    0,
                    totalQty,
                    0,
                    0,
                    0,
                    null,
                    "NORMAL",
                    null,
                    LocalDateTime.now()
            );
            adapter.createStockLevel(newStock);
        } else {
            StockLevel updated = new StockLevel(
                    existing.stockLevelId(),
                    product.productId(),
                    totalQty,
                    existing.reservedStockQty(),
                    totalQty,
                    existing.reorderThreshold(),
                    existing.reorderQuantity(),
                    existing.safetyStockLevel(),
                    existing.zoneAssignment(),
                    existing.stockHealthStatus(),
                    existing.snapshotTimestamp(),
                    LocalDateTime.now()
            );
            adapter.updateStockLevel(updated);
        }
    }

    @Override
    public boolean exists(String sku, String locationId) {
        return localStore.containsKey(key(sku, locationId));
    }
}