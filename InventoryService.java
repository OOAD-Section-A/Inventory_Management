package inventory_subsystem;

public class InventoryService implements InventoryUI {

    private final InventoryDataStore repository;
    private final InventoryExceptionSource exceptionSource;

    private final AddStockStrategy addStrategy;
    private final RemoveStockStrategy removeStrategy;
    private final TransferStockStrategy transferStrategy;

    private IssuingPolicy policy = IssuingPolicy.FIFO;

    public InventoryService(InventoryDataStore repository,
                            InventoryExceptionSource exceptionSource) {

        this.repository = repository;
        this.exceptionSource = exceptionSource;

        this.addStrategy = new AddStockStrategy();
        this.removeStrategy = new RemoveStockStrategy();
        this.transferStrategy = new TransferStockStrategy();
    }

    public void setIssuingPolicy(IssuingPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void addStock(String sku, String locationId, int quantity) {
        addStrategy.execute(sku, locationId, quantity, repository, exceptionSource, policy);
    }

    @Override
    public void removeStock(String sku, String locationId, int quantity) {
        removeStrategy.execute(sku, locationId, quantity, repository, exceptionSource, policy);
    }

    @Override
    public void transferStock(String sku, String fromLocation,
                              String toLocation, int quantity) {

        transferStrategy.executeTransfer(
                sku, fromLocation, toLocation, quantity,
                repository, exceptionSource, policy
        );
    }

    @Override
    public int getStock(String sku, String locationId) {

        InventoryItem item = repository.find(sku, locationId);

        if (item == null) {
            exceptionSource.fireResourceNotFound(
                    166, "InventoryItem", sku + "_" + locationId);
            return 0;
        }

        return item.getTotalQuantity();
    }
}