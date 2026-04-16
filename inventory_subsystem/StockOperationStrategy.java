package inventory_subsystem;

public interface StockOperationStrategy {

void execute(String sku,
             String locationId,
             int quantity,
             InventoryRepository repository,
             InventoryExceptionSource exceptionSource);

}
