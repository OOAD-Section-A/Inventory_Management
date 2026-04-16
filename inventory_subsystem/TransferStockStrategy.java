package inventory_subsystem;

public class TransferStockStrategy implements StockOperationStrategy {

@Override
public void execute(String sku, String locationId, int quantity,
                    InventoryRepository repository,
                    InventoryExceptionSource exceptionSource) {
    throw new UnsupportedOperationException("Use transfer method with two locations");
}

public void executeTransfer(String sku,
                            String fromLocation,
                            String toLocation,
                            int quantity,
                            InventoryRepository repository,
                            InventoryExceptionSource exceptionSource) {

    InventoryItem source = repository.find(sku, fromLocation);

    if (source == null) {
        exceptionSource.fireResourceNotFound(166, "InventoryItem", sku);
        return;
    }

    if (source.getQuantity() < quantity) {
        exceptionSource.fireResourceExhausted(
                167, "Stock", sku, quantity, source.getQuantity());
        return;
    }

    InventoryItem destination = repository.find(sku, toLocation);

    source.setQuantity(source.getQuantity() - quantity);

    if (destination == null) {
        destination = new InventoryItem(sku, toLocation, quantity);
    } else {
        destination.setQuantity(destination.getQuantity() + quantity);
    }

    repository.save(source);
    repository.save(destination);
}

}
