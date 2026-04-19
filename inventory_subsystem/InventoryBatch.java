package inventory_subsystem;

import java.time.LocalDateTime;
import java.util.*;

public class InventoryBatch {

    private String batchId;
    private String productId;
    private String supplierId;

    private int quantity;

    private LocalDateTime arrivalTime;
    private LocalDateTime expiryTime;

    private double unitCost;

    public InventoryBatch(String batchId,
                          String productId,
                          String supplierId,
                          int quantity,
                          LocalDateTime arrivalTime,
                          LocalDateTime expiryTime,
                          double unitCost) {

        this.batchId = batchId;
        this.productId = productId;
        this.supplierId = supplierId;
        this.quantity = quantity;
        this.arrivalTime = arrivalTime;
        this.expiryTime = expiryTime;
        this.unitCost = unitCost;
    }

    public String getBatchId() { return batchId; }
    public String getProductId() { return productId; }
    public String getSupplierId() { return supplierId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
}
