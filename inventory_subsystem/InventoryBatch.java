package inventory_subsystem;

import java.time.LocalDateTime;

public class InventoryBatch {

    private int quantity;
    private LocalDateTime arrivalTime;
    private LocalDateTime expiryTime;

    public InventoryBatch(int quantity,
                          LocalDateTime arrivalTime,
                          LocalDateTime expiryTime) {

        this.quantity = quantity;
        this.arrivalTime = arrivalTime;
        this.expiryTime = expiryTime;
    }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
}