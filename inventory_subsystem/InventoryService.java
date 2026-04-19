package inventory_subsystem;

public class InventoryService implements InventoryUI {

    private final InventoryDataStore repository;
    private final InventoryExceptionSource exceptionSource;

    private final AddStockStrategy addStrategy = new AddStockStrategy();
    private final RemoveStockStrategy removeStrategy = new RemoveStockStrategy();
    private final TransferStockStrategy transferStrategy = new TransferStockStrategy();

    private IssuingPolicy policy = IssuingPolicy.FIFO;

    public InventoryService(InventoryExceptionSource exceptionSource) {
        this.exceptionSource = exceptionSource;
        this.repository = new InventoryRepository();
    }

    @Override
    public void addStock(String productId, String locationId,
                         String supplierId, int quantity,
                         String referenceType, String referenceId) {

        addStrategy.execute(productId, locationId, supplierId,
                quantity, referenceType, referenceId,
                repository, exceptionSource, policy);
    }

    @Override
    public void removeStock(String productId, String locationId, int quantity) {
        removeStrategy.execute(productId, locationId, quantity,
                repository, exceptionSource, policy);
    }

    @Override
    public void transferStock(String productId, String fromLocation,
                              String toLocation, int quantity) {

        transferStrategy.execute(productId, fromLocation, quantity,
                repository, exceptionSource, policy);

        transferStrategy.addToDestination(productId, toLocation, quantity, repository);
    }

    @Override
    public int getStock(String productId, String locationId) {
        InventoryItem item = repository.find(productId, locationId);
        return item == null ? 0 : item.getTotalQuantity();
    }
}
