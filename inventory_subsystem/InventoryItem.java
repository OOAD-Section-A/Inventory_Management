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
    public void setTotalQuantity(int total) { this.totalQuantity = total; }

    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reserved) { this.reservedQuantity = reserved; }

    public int getAvailableQuantity() { return totalQuantity - reservedQuantity; }

    public List<InventoryBatch> getBatches() { return batches; }
    public String getStatus() { return status; }

    public String getAbcCategory() { return abcCategory; }
    public void setAbcCategory(String abc) { this.abcCategory = abc; }

    public int getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(int threshold) { this.reorderThreshold = threshold; }

    public int getSafetyStockLevel() { return safetyStockLevel; }
    public void setSafetyStockLevel(int level) { this.safetyStockLevel = level; }

    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }

    public void addBatch(InventoryBatch batch) {
        batches.add(batch);
        recomputeTotalQuantity();
    }

    public void recomputeTotalQuantity() {
        totalQuantity = batches.stream()
                .mapToInt(InventoryBatch::getQuantity)
                .sum();
        updateStatus();
    }

    public boolean needsReorder() {
        return getAvailableQuantity() <= reorderThreshold;
    }

    public boolean belowSafetyStock() {
        return getAvailableQuantity() <= safetyStockLevel;
    }

    public void updateStatus() {
        if (totalQuantity == 0) status = "OUT_OF_STOCK";
        else if (belowSafetyStock()) status = "LOW_STOCK";
        else status = "AVAILABLE";
    }
}
