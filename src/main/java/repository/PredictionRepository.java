package repository;

import database.SQLiteConnection;
import model.Prediction;
import model.TrendDirection;
import util.DatabaseException;

import java.sql.*;
import java.util.Optional;

/**
 * JDBC Repository managing persisted records in the predictions table.
 */
public class PredictionRepository {

    public void saveOrUpdate(Prediction prediction) throws DatabaseException {
        String selectSql = "SELECT id FROM predictions WHERE stock_id = ? ORDER BY id DESC LIMIT 1";
        String insertSql = """
                INSERT INTO predictions (stock_id, current_price, predicted_price, slope, trend, accuracy_percent, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        String updateSql = """
                UPDATE predictions
                SET current_price = ?, predicted_price = ?, slope = ?, trend = ?, accuracy_percent = ?, created_at = ?
                WHERE id = ?
                """;

        try (Connection conn = SQLiteConnection.getConnection()) {
            Integer existingId = null;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setInt(1, prediction.getStockId());
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId == null) {
                try (PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ins.setInt(1, prediction.getStockId());
                    ins.setDouble(2, prediction.getCurrentPrice());
                    ins.setDouble(3, prediction.getPredictedPrice());
                    ins.setDouble(4, prediction.getSlope());
                    ins.setString(5, prediction.getTrend().getLabel());
                    ins.setDouble(6, prediction.getAccuracyPercent());
                    ins.setLong(7, prediction.getCreatedAt());
                    ins.executeUpdate();
                    try (ResultSet keys = ins.getGeneratedKeys()) {
                        if (keys.next()) {
                            prediction.setId(keys.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                    upd.setDouble(1, prediction.getCurrentPrice());
                    upd.setDouble(2, prediction.getPredictedPrice());
                    upd.setDouble(3, prediction.getSlope());
                    upd.setString(4, prediction.getTrend().getLabel());
                    upd.setDouble(5, prediction.getAccuracyPercent());
                    upd.setLong(6, prediction.getCreatedAt());
                    upd.setInt(7, existingId);
                    upd.executeUpdate();
                    prediction.setId(existingId);
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to persist prediction: " + ex.getMessage(), ex);
        }
    }

    public Optional<Prediction> findLatestByStockId(int stockId, String stockName) throws DatabaseException {
        String sql = """
                SELECT id, stock_id, current_price, predicted_price, slope, trend, accuracy_percent, created_at
                FROM predictions WHERE stock_id = ? ORDER BY id DESC LIMIT 1
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Prediction(
                            rs.getInt("id"),
                            rs.getInt("stock_id"),
                            stockName,
                            rs.getDouble("current_price"),
                            rs.getDouble("predicted_price"),
                            rs.getDouble("slope"),
                            TrendDirection.fromLabel(rs.getString("trend")),
                            rs.getDouble("accuracy_percent"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch prediction: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public void deleteByStockId(int stockId) throws DatabaseException {
        String sql = "DELETE FROM predictions WHERE stock_id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to delete predictions: " + ex.getMessage(), ex);
        }
    }
}
