package inventory_subsystem;

public interface StockOperationStrategy {

    void execute(String productId,
                 String locationId,
                 int quantity,
                 InventoryDataStore repository,
                 InventoryExceptionSource exceptionSource,
                 IssuingPolicy policy);
}
