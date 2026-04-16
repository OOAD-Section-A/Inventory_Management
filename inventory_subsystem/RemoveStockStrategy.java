package inventory_subsystem;

public class RemoveStockStrategy implements StockOperationStrategy {

@Override
public void execute(String sku, String locationId, int quantity,
                    InventoryRepository repository,
                    InventoryExceptionSource exceptionSource) {

    InventoryItem item = repository.find(sku, locationId);

    if (item == null) {
        exceptionSource.fireResourceNotFound(166, "InventoryItem", sku);
        return;
    }

    if (item.getQuantity() < quantity) {
        exceptionSource.fireResourceExhausted(
                167, "Stock", sku, quantity, item.getQuantity());
        return;
    }

    item.setQuantity(item.getQuantity() - quantity);

    repository.save(item);
}

}
