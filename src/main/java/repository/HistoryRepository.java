package repository;

import database.SQLiteConnection;
import model.PriceHistory;
import util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Repository managing price_history records.
 */
public class HistoryRepository {

    public List<PriceHistory> findByStockId(int stockId) throws DatabaseException {
        String sql = "SELECT id, stock_id, seq_index, price, recorded_at FROM price_history WHERE stock_id = ? ORDER BY seq_index ASC";
        List<PriceHistory> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PriceHistory(
                            rs.getInt("id"),
                            rs.getInt("stock_id"),
                            rs.getInt("seq_index"),
                            rs.getDouble("price"),
                            rs.getLong("recorded_at")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to read price history: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<Double> findPricesByStockId(Connection conn, int stockId) throws SQLException {
        String sql = "SELECT price FROM price_history WHERE stock_id = ? ORDER BY seq_index ASC";
        List<Double> prices = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prices.add(rs.getDouble("price"));
                }
            }
        }
        return prices;
    }

    public void insertBatch(Connection conn, int stockId, List<Double> prices) throws SQLException {
        String sql = "INSERT INTO price_history (stock_id, seq_index, price, recorded_at) VALUES (?, ?, ?, ?)";
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < prices.size(); i++) {
                ps.setInt(1, stockId);
                ps.setInt(2, i);
                ps.setDouble(3, prices.get(i));
                ps.setLong(4, now + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void replaceHistory(Connection conn, int stockId, List<Double> prices) throws SQLException {
        String delSql = "DELETE FROM price_history WHERE stock_id = ?";
        try (PreparedStatement delPs = conn.prepareStatement(delSql)) {
            delPs.setInt(1, stockId);
            delPs.executeUpdate();
        }
        insertBatch(conn, stockId, prices);
    }

    public void appendPrice(int stockId, int nextSeqIndex, double price) throws DatabaseException {
        String sql = "INSERT INTO price_history (stock_id, seq_index, price, recorded_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            ps.setInt(2, nextSeqIndex);
            ps.setDouble(3, price);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to append price: " + ex.getMessage(), ex);
        }
    }

    public void updatePriceAtIndex(int stockId, int seqIndex, double newPrice) throws DatabaseException {
        String sql = "UPDATE price_history SET price = ?, recorded_at = ? WHERE stock_id = ? AND seq_index = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, stockId);
            ps.setInt(4, seqIndex);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update price at index " + seqIndex + ": " + ex.getMessage(), ex);
        }
    }

    public void replaceHistory(int stockId, List<Double> updatedPrices) throws DatabaseException {
        try (Connection conn = SQLiteConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                replaceHistory(conn, stockId, updatedPrices);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update stock history: " + ex.getMessage(), ex);
        }
    }
}
