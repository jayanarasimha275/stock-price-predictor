package repository;

import database.SQLiteConnection;
import model.Prediction;
import model.TrendDirection;
import util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Repository managing persisted records in the predictions table.
 */
public class PredictionRepository {

    public void saveOrUpdate(Prediction prediction) throws DatabaseException {
        String selectSql = """
                SELECT id, current_price, predicted_price, slope
                FROM predictions WHERE stock_id = ? ORDER BY id DESC LIMIT 1
                """;
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
            boolean sameValues = false;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setInt(1, prediction.getStockId());
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                        double prevCur = rs.getDouble("current_price");
                        double prevPred = rs.getDouble("predicted_price");
                        double prevSlope = rs.getDouble("slope");
                        sameValues = Math.abs(prevCur - prediction.getCurrentPrice()) < 1e-6
                                && Math.abs(prevPred - prediction.getPredictedPrice()) < 1e-6
                                && Math.abs(prevSlope - prediction.getSlope()) < 1e-7;
                    }
                }
            }

            // Record a new historical snapshot if none exists or price/slope changed; refresh timestamp otherwise
            if (existingId == null || !sameValues) {
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
                    return Optional.of(mapRow(rs, stockName));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch prediction: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public List<Prediction> findAllWithStockInfo() throws DatabaseException {
        String sql = """
                SELECT p.id, p.stock_id, s.name AS stock_name, p.current_price, p.predicted_price,
                       p.slope, p.trend, p.accuracy_percent, p.created_at
                FROM predictions p
                INNER JOIN stocks s ON p.stock_id = s.id
                ORDER BY p.created_at DESC, p.id DESC
                """;
        List<Prediction> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs, rs.getString("stock_name")));
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch all prediction records: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<Prediction> findByStockId(int stockId, String stockName) throws DatabaseException {
        String sql = """
                SELECT id, stock_id, current_price, predicted_price, slope, trend, accuracy_percent, created_at
                FROM predictions WHERE stock_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        List<Prediction> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs, stockName));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch prediction history: " + ex.getMessage(), ex);
        }
        return list;
    }

    public int countAll() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM predictions";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to count predictions: " + ex.getMessage(), ex);
        }
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

    private Prediction mapRow(ResultSet rs, String stockName) throws SQLException {
        return new Prediction(
                rs.getInt("id"),
                rs.getInt("stock_id"),
                stockName,
                rs.getDouble("current_price"),
                rs.getDouble("predicted_price"),
                rs.getDouble("slope"),
                TrendDirection.fromLabel(rs.getString("trend")),
                rs.getDouble("accuracy_percent"),
                rs.getLong("created_at")
        );
    }
}

