package inventory_subsystem;

public class InventoryRepository implements InventoryDataStore {

    @Override
    public InventoryItem find(String productId, String locationId) {
        // DB adapter call
        return null;
    }

    @Override
    public void save(InventoryItem item) {
        // DB adapter call
    }

    public void recordTransaction(StockTransaction tx) {
        // DB insert
    }
}
