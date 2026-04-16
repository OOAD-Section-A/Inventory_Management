package inventory_subsystem;

public class InventoryService implements InventoryUI {

    private final InventoryRepository repository;
    private final InventoryExceptionSource exceptionSource;

    private final AddStockStrategy addStrategy;
    private final RemoveStockStrategy removeStrategy;
    private final TransferStockStrategy transferStrategy;

    public InventoryService(InventoryRepository repository,
                            InventoryExceptionSource exceptionSource) {

        this.repository = repository;
        this.exceptionSource = exceptionSource;

        this.addStrategy = new AddStockStrategy();
        this.removeStrategy = new RemoveStockStrategy();
        this.transferStrategy = new TransferStockStrategy();
    }

    @Override
    public void addStock(String sku, String locationId, int quantity) {
        addStrategy.execute(sku, locationId, quantity, repository, exceptionSource);
    }

    @Override
    public void removeStock(String sku, String locationId, int quantity) {

        InventoryItem item = repository.find(sku, locationId);

        if (item == null) {
            removeStrategy.execute(sku, locationId, quantity, repository, exceptionSource);
            return;
        }

        int currentVersion = item.getVersion();

        removeStrategy.execute(sku, locationId, quantity, repository, exceptionSource);

        InventoryItem updated = repository.find(sku, locationId);

        if (updated == null) return;

        // Simulated concurrency check
        if (updated.getVersion() != currentVersion) {
            exceptionSource.fireConflict(
                    110, "InventoryItem", sku, "Concurrent modification detected");
            return;
        }

        updated.setVersion(currentVersion + 1);
        repository.save(updated);
    }

    @Override
    public void transferStock(String sku, String fromLocation,
                              String toLocation, int quantity) {

        transferStrategy.executeTransfer(
                sku, fromLocation, toLocation, quantity,
                repository, exceptionSource
        );
    }

    @Override
    public int getStock(String sku, String locationId) {

        InventoryItem item = repository.find(sku, locationId);

        if (item == null) {
            exceptionSource.fireResourceNotFound(
                    166, "InventoryItem", sku + " at " + locationId);
            return 0;
        }

        return item.getQuantity();
    }
}