package repository;

import database.SQLiteConnection;
import model.TechnicalIndicator;
import util.DatabaseException;

import java.sql.*;

/**
 * JDBC Repository managing persisted records in the technical_indicators table.
 */
public class IndicatorRepository {

    public void saveOrUpdate(TechnicalIndicator indicator) throws DatabaseException {
        String selectSql = "SELECT id FROM technical_indicators WHERE stock_id = ? ORDER BY id DESC LIMIT 1";
        String insertSql = """
                INSERT INTO technical_indicators
                (stock_id, sma_period, sma_value, ema_period, ema_value, rsi_period, rsi_value,
                 macd_value, macd_signal, momentum_value, calculated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateSql = """
                UPDATE technical_indicators
                SET sma_period = ?, sma_value = ?, ema_period = ?, ema_value = ?,
                    rsi_period = ?, rsi_value = ?, macd_value = ?, macd_signal = ?,
                    momentum_value = ?, calculated_at = ?
                WHERE id = ?
                """;

        try (Connection conn = SQLiteConnection.getConnection()) {
            Integer existingId = null;
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setInt(1, indicator.getStockId());
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId == null) {
                try (PreparedStatement ins = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ins.setInt(1, indicator.getStockId());
                    ins.setInt(2, indicator.getSmaPeriod());
                    ins.setDouble(3, indicator.getSmaValue());
                    ins.setInt(4, indicator.getEmaPeriod());
                    ins.setDouble(5, indicator.getEmaValue());
                    ins.setInt(6, indicator.getRsiPeriod());
                    ins.setDouble(7, indicator.getRsiValue());
                    ins.setDouble(8, indicator.getMacdLine());
                    ins.setDouble(9, indicator.getMacdSignal());
                    ins.setDouble(10, indicator.getMomentumValue());
                    ins.setLong(11, indicator.getCalculatedAt());
                    ins.executeUpdate();
                    try (ResultSet keys = ins.getGeneratedKeys()) {
                        if (keys.next()) {
                            indicator.setId(keys.getInt(1));
                        }
                    }
                }
            } else {
                try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                    upd.setInt(1, indicator.getSmaPeriod());
                    upd.setDouble(2, indicator.getSmaValue());
                    upd.setInt(3, indicator.getEmaPeriod());
                    upd.setDouble(4, indicator.getEmaValue());
                    upd.setInt(5, indicator.getRsiPeriod());
                    upd.setDouble(6, indicator.getRsiValue());
                    upd.setDouble(7, indicator.getMacdLine());
                    upd.setDouble(8, indicator.getMacdSignal());
                    upd.setDouble(9, indicator.getMomentumValue());
                    upd.setLong(10, indicator.getCalculatedAt());
                    upd.setInt(11, existingId);
                    upd.executeUpdate();
                    indicator.setId(existingId);
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to persist technical indicators: " + ex.getMessage(), ex);
        }
    }

    public void deleteByStockId(int stockId) throws DatabaseException {
        String sql = "DELETE FROM technical_indicators WHERE stock_id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to delete technical indicators: " + ex.getMessage(), ex);
        }
    }
}
