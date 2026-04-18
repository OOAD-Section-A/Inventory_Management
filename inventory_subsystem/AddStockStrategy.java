package inventory_subsystem;

import java.time.LocalDateTime;

public class AddStockStrategy implements StockOperationStrategy {

    @Override
    public void execute(String sku, String locationId, int quantity,
                        InventoryDataStore repository,
                        InventoryExceptionSource exceptionSource,
                        IssuingPolicy policy) {

        InventoryItem item = repository.find(sku, locationId);

        if (item == null) {
            item = new InventoryItem(sku, locationId, quantity);
        } else {
            item.addBatch(new InventoryBatch(
                    quantity,
                    LocalDateTime.now(),
                    null
            ));
            item.setVersion(item.getVersion() + 1);
        }

        repository.save(item);
    }
}