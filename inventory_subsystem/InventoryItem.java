package inventory_subsystem;

import java.util.*;
import java.time.LocalDateTime;

public class InventoryItem {

    private String sku;
    private String locationId;
    private int version;

    private List<InventoryBatch> batches = new ArrayList<>();

    public InventoryItem(String sku, String locationId, int quantity) {
        this.sku = sku;
        this.locationId = locationId;
        this.version = 0;

        batches.add(new InventoryBatch(
                quantity,
                LocalDateTime.now(),
                null
        ));
    }

    public String getSku() { return sku; }

    public String getLocationId() { return locationId; }

    public int getVersion() { return version; }

    public void setVersion(int version) { this.version = version; }

    public List<InventoryBatch> getBatches() { return batches; }

    public void addBatch(InventoryBatch batch) {
        batches.add(batch);
    }

    public int getTotalQuantity() {
        return batches.stream().mapToInt(InventoryBatch::getQuantity).sum();
    }
}