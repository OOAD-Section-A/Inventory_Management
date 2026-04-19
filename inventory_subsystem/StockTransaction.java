package inventory_subsystem;

import java.time.LocalDateTime;

public class StockTransaction {

    private String productId;
    private String batchId;
    private String locationId;

    private int quantityChange;

    private String type;
    private String referenceType;
    private String referenceId;

    private LocalDateTime timestamp;

    public StockTransaction(String productId, String batchId,
                            String locationId, int quantityChange,
                            String type, String referenceType, String referenceId) {

        this.productId = productId;
        this.batchId = batchId;
        this.locationId = locationId;
        this.quantityChange = quantityChange;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.timestamp = LocalDateTime.now();
    }
}
