package repository;

import database.SQLiteConnection;
import model.Stock;
import util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Repository for stocks table and coordinated price_history retrieval.
 */
public class StockRepository implements Repository<Stock, Integer> {

    private final HistoryRepository historyRepository;

    public StockRepository() {
        this.historyRepository = new HistoryRepository();
    }

    public StockRepository(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public List<Stock> findAll() throws DatabaseException {
        String sql = "SELECT id, name, symbol, notes, created_at FROM stocks ORDER BY id ASC";
        List<Stock> stocks = new ArrayList<>();

        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String symbol = rs.getString("symbol");
                String notes = rs.getString("notes");
                long createdAt = rs.getLong("created_at");
                List<Double> prices = historyRepository.findPricesByStockId(conn, id);
                stocks.add(new Stock(id, name, symbol, notes, createdAt, prices));
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch stocks: " + ex.getMessage(), ex);
        }
        return stocks;
    }

    @Override
    public Optional<Stock> findById(Integer id) throws DatabaseException {
        String sql = "SELECT id, name, symbol, notes, created_at FROM stocks WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String symbol = rs.getString("symbol");
                    String notes = rs.getString("notes");
                    long createdAt = rs.getLong("created_at");
                    List<Double> prices = historyRepository.findPricesByStockId(conn, id);
                    return Optional.of(new Stock(id, name, symbol, notes, createdAt, prices));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to find stock by id: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<Stock> findByNameIgnoreCase(String name) throws DatabaseException {
        if (name == null) {
            return Optional.empty();
        }
        String sql = "SELECT id, name, symbol, notes, created_at FROM stocks WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String foundName = rs.getString("name");
                    String symbol = rs.getString("symbol");
                    String notes = rs.getString("notes");
                    long createdAt = rs.getLong("created_at");
                    List<Double> prices = historyRepository.findPricesByStockId(conn, id);
                    return Optional.of(new Stock(id, foundName, symbol, notes, createdAt, prices));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to look up stock by name: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public Stock save(Stock stock) throws DatabaseException {
        String insertSql = "INSERT INTO stocks (name, symbol, notes, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = SQLiteConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, stock.getName());
                ps.setString(2, stock.getSymbol());
                ps.setString(3, stock.getNotes());
                ps.setLong(4, stock.getCreatedAt());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        stock.setId(keys.getInt(1));
                    }
                }
                historyRepository.insertBatch(conn, stock.getId(), stock.getHistory());
                conn.commit();
                return stock;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to save stock \"" + stock.getName() + "\": " + ex.getMessage(), ex);
        }
    }

    public void update(Stock stock) throws DatabaseException {
        String updateSql = "UPDATE stocks SET name = ?, symbol = ?, notes = ? WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, stock.getName());
                ps.setString(2, stock.getSymbol());
                ps.setString(3, stock.getNotes());
                ps.setInt(4, stock.getId());
                ps.executeUpdate();

                historyRepository.replaceHistory(conn, stock.getId(), stock.getHistory());
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update stock \"" + stock.getName() + "\": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteById(Integer id) throws DatabaseException {
        if (id == null || id <= 0) {
            return;
        }
        String delHistory = "DELETE FROM price_history WHERE stock_id = ?";
        String delPred = "DELETE FROM predictions WHERE stock_id = ?";
        String delInd = "DELETE FROM technical_indicators WHERE stock_id = ?";
        String delStock = "DELETE FROM stocks WHERE id = ?";

        try (Connection conn = SQLiteConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(delHistory)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(delPred)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(delInd)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(delStock)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to delete stock ID " + id + ": " + ex.getMessage(), ex);
        }
    }

    public void deleteByName(String name) throws DatabaseException {
        Optional<Stock> found = findByNameIgnoreCase(name);
        if (found.isPresent()) {
            deleteById(found.get().getId());
        }
    }

    public void deleteAll() throws DatabaseException {
        try (Connection conn = SQLiteConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM price_history");
                stmt.executeUpdate("DELETE FROM predictions");
                stmt.executeUpdate("DELETE FROM technical_indicators");
                stmt.executeUpdate("DELETE FROM stocks");
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to clear all stocks: " + ex.getMessage(), ex);
        }
    }
}
