package inventory_subsystem;

public interface InventoryUI {

    void addStock(String productId, String locationId,
                  String supplierId, int quantity,
                  String referenceType, String referenceId);

    void removeStock(String productId, String locationId, int quantity);

    void transferStock(String productId, String fromLocation,
                       String toLocation, int quantity);

    int getStock(String productId, String locationId);
}
