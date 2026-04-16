package inventory_subsystem;

public class AddStockStrategy implements StockOperationStrategy {

@Override
public void execute(String sku, String locationId, int quantity,
                    InventoryRepository repository,
                    InventoryExceptionSource exceptionSource) {

    InventoryItem item = repository.find(sku, locationId);

    if (item == null) {
        item = new InventoryItem(sku, locationId, quantity);
    } else {
        item.setQuantity(item.getQuantity() + quantity);
    }

    repository.save(item);
}

}
