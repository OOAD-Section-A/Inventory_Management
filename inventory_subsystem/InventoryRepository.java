package inventory_subsystem;

import java.util.HashMap;
import java.util.Map;

public class InventoryRepository implements InventoryDataStore {

private final Map<String, InventoryItem> store = new HashMap<>();

private String generateKey(String sku, String locationId) {
    return sku + "_" + locationId;
}

@Override
public InventoryItem find(String sku, String locationId) {
    return store.get(generateKey(sku, locationId));
}

@Override
public void save(InventoryItem item) {
    store.put(generateKey(item.getSku(), item.getLocationId()), item);
}

@Override
public boolean exists(String sku, String locationId) {
    return store.containsKey(generateKey(sku, locationId));
}

}
