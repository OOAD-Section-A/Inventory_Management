package inventory_subsystem;

public class InventoryService implements InventoryUI {

    private final InventoryDataStore repository;

    private final AddStockStrategy addStrategy = new AddStockStrategy();
    private final RemoveStockStrategy removeStrategy = new RemoveStockStrategy();
    private final TransferStockStrategy transferStrategy = new TransferStockStrategy();

    private IssuingPolicy policy = IssuingPolicy.FIFO;

    public InventoryService() {
        this.repository = new InventoryRepository();
    }

    @Override
    public void addStock(String productId, String locationId,
                         String supplierId, int quantity,
                         String referenceType, String referenceId) {

        addStrategy.executeAdd(productId, locationId, supplierId,
            quantity, referenceType, referenceId,
            repository, policy);
    }

    @Override
    public void removeStock(String productId, String locationId, int quantity) {
        removeStrategy.execute(productId, locationId, quantity, repository, policy);
    }

    @Override
    public void transferStock(String productId, String fromLocation,
                              String toLocation, int quantity) {

        transferStrategy.transfer(productId, fromLocation, toLocation, quantity, repository, policy);
    }

    @Override
    public int getStock(String productId, String locationId) {
        InventoryItem item = repository.find(productId, locationId);
        return item == null ? 0 : item.getTotalQuantity();
    }
}
