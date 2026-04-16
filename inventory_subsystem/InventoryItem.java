package inventory_subsystem;

public class InventoryItem {

private String sku;
private String locationId;
private int quantity;

private String lotNumber;
private String expiryDate;

private int version;

public InventoryItem(String sku, String locationId, int quantity) {
    this.sku = sku;
    this.locationId = locationId;
    this.quantity = quantity;
    this.version = 0;
}

public String getSku() { return sku; }

public String getLocationId() { return locationId; }

public int getQuantity() { return quantity; }

public void setQuantity(int quantity) { this.quantity = quantity; }

public String getLotNumber() { return lotNumber; }

public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

public String getExpiryDate() { return expiryDate; }

public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

public int getVersion() { return version; }

public void setVersion(int version) { this.version = version; }

}
