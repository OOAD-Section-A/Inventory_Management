package inventory_subsystem;

public interface InventoryDataStore {

    InventoryItem find(String productId, String locationId);

    void save(InventoryItem item);
}
