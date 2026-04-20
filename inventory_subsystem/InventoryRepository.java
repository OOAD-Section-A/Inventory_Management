package inventory_subsystem;

import com.scm.core.Severity;
import com.scm.factory.SCMExceptionFactory;
import com.scm.handler.SCMExceptionHandler;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryRepository implements InventoryDataStore {

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/scm", "user", "password");
    }

    @Override
    public InventoryItem find(String productId, String locationId) {

        try (Connection conn = getConnection()) {

            String itemQuery = "SELECT * FROM inventory_items WHERE product_id=? AND location_id=?";
            PreparedStatement psItem = conn.prepareStatement(itemQuery);
            psItem.setString(1, productId);
            psItem.setString(2, locationId);

            ResultSet rsItem = psItem.executeQuery();
            if (!rsItem.next()) return null;

            InventoryItem item = new InventoryItem(productId, locationId);

            item.setTotalQuantity(rsItem.getInt("total_quantity"));
            item.setReservedQuantity(rsItem.getInt("reserved_quantity"));
            item.setAbcCategory(rsItem.getString("abc_category"));
            item.setReorderThreshold(rsItem.getInt("reorder_threshold"));
            item.setSafetyStockLevel(rsItem.getInt("safety_stock_level"));
            item.setVersion(rsItem.getInt("version"));

            String batchQuery = "SELECT * FROM inventory_batches WHERE product_id=? AND location_id=?";
            PreparedStatement psBatch = conn.prepareStatement(batchQuery);
            psBatch.setString(1, productId);
            psBatch.setString(2, locationId);

            ResultSet rsBatch = psBatch.executeQuery();

            while (rsBatch.next()) {
                InventoryBatch batch = new InventoryBatch(
                        rsBatch.getString("batch_id"),
                        productId,
                        rsBatch.getString("supplier_id"),
                        rsBatch.getInt("quantity"),
                        rsBatch.getTimestamp("arrival_time").toLocalDateTime(),
                        rsBatch.getTimestamp("expiry_time") != null
                                ? rsBatch.getTimestamp("expiry_time").toLocalDateTime()
                                : null
                );

                item.getBatches().add(batch);
            }

            return item;

        } catch (Exception e) {
            SCMExceptionHandler.INSTANCE.handle(
                    SCMExceptionFactory.create(0, "UNREGISTERED_EXCEPTION",
                            "Inventory repository find failed: " + e.getMessage(),
                            "Inventory", Severity.MINOR)
            );
            return null;
        }
    }

    @Override
    public void save(InventoryItem item) {

        try (Connection conn = getConnection()) {

            conn.setAutoCommit(false);

            String upsertItem = """
                INSERT INTO inventory_items 
                (product_id, location_id, total_quantity, reserved_quantity,
                 abc_category, reorder_threshold, safety_stock_level, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    total_quantity = VALUES(total_quantity),
                    reserved_quantity = VALUES(reserved_quantity),
                    abc_category = VALUES(abc_category),
                    reorder_threshold = VALUES(reorder_threshold),
                    safety_stock_level = VALUES(safety_stock_level),
                    version = VALUES(version)
            """;

            PreparedStatement psItem = conn.prepareStatement(upsertItem);

            psItem.setString(1, item.getProductId());
            psItem.setString(2, item.getLocationId());
            psItem.setInt(3, item.getTotalQuantity());
            psItem.setInt(4, item.getReservedQuantity());
            psItem.setString(5, item.getAbcCategory());
            psItem.setInt(6, item.getReorderThreshold());
            psItem.setInt(7, item.getSafetyStockLevel());
            psItem.setInt(8, item.getVersion());

            psItem.executeUpdate();

            String deleteBatches = "DELETE FROM inventory_batches WHERE product_id=? AND location_id=?";
            PreparedStatement psDelete = conn.prepareStatement(deleteBatches);
            psDelete.setString(1, item.getProductId());
            psDelete.setString(2, item.getLocationId());
            psDelete.executeUpdate();

            String insertBatch = """
                INSERT INTO inventory_batches
                (batch_id, product_id, location_id, supplier_id, quantity, arrival_time, expiry_time)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement psBatch = conn.prepareStatement(insertBatch);

            for (InventoryBatch batch : item.getBatches()) {
                psBatch.setString(1, batch.getBatchId());
                psBatch.setString(2, item.getProductId());
                psBatch.setString(3, item.getLocationId());
                psBatch.setString(4, batch.getSupplierId());
                psBatch.setInt(5, batch.getQuantity());
                psBatch.setTimestamp(6, Timestamp.valueOf(batch.getArrivalTime()));

                if (batch.getExpiryTime() != null)
                    psBatch.setTimestamp(7, Timestamp.valueOf(batch.getExpiryTime()));
                else
                    psBatch.setNull(7, Types.TIMESTAMP);

                psBatch.addBatch();
            }

            psBatch.executeBatch();
            conn.commit();

        } catch (Exception e) {
            SCMExceptionHandler.INSTANCE.handle(
                    SCMExceptionFactory.create(0, "UNREGISTERED_EXCEPTION",
                            "Inventory repository save failed: " + e.getMessage(),
                            "Inventory", Severity.MINOR)
            );
            return;
        }
    }

    public void recordTransaction(StockTransaction tx) {

        try (Connection conn = getConnection()) {

            String insertTx = """
                INSERT INTO stock_transactions
                (transaction_id, product_id, batch_id, location_id,
                 quantity_change, type, reference_type, reference_id, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement ps = conn.prepareStatement(insertTx);

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tx.getProductId());
            ps.setString(3, tx.getBatchId());
            ps.setString(4, tx.getLocationId());
            ps.setInt(5, tx.getQuantityChange());
            ps.setString(6, tx.getType());
            ps.setString(7, tx.getReferenceType());
            ps.setString(8, tx.getReferenceId());
            ps.setTimestamp(9, Timestamp.valueOf(tx.getTimestamp()));

            ps.executeUpdate();

        } catch (Exception e) {
            SCMExceptionHandler.INSTANCE.handle(
                    SCMExceptionFactory.create(0, "UNREGISTERED_EXCEPTION",
                            "Inventory repository transaction recording failed: " + e.getMessage(),
                            "Inventory", Severity.MINOR)
            );
            return;
        }
    }
}
