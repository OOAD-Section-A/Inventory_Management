package inventory_subsystem;

import java.util.*;

public class InventoryItem {

    private String productId;
    private String locationId;

    private int totalQuantity;
    private int reservedQuantity;

    private String status = "AVAILABLE";

    private String abcCategory = "C";
    private int reorderThreshold = 0;
    private int safetyStockLevel = 0;

    private int version;

    private List<InventoryBatch> batches = new ArrayList<>();

    public InventoryItem(String productId, String locationId) {
        this.productId = productId;
        this.locationId = locationId;
    }

    public String getProductId() { return productId; }
    public String getLocationId() { return locationId; }

    public int getTotalQuantity() { return totalQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getAvailableQuantity() { return totalQuantity - reservedQuantity; }

    public List<InventoryBatch> getBatches() { return batches; }

    public void addBatch(InventoryBatch batch) {
        batches.add(batch);
        totalQuantity += batch.getQuantity();
    }

    public void deductQuantity(int qty) {
        totalQuantity -= qty;
    }

    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }

    public int getReorderThreshold() { return reorderThreshold; }
    public int getSafetyStockLevel() { return safetyStockLevel; }
}
