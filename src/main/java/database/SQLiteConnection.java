package database;

import util.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite JDBC connections and schema initialization.
 * Maintains tables: stocks, price_history, predictions, technical_indicators, app_settings.
 */
public final class SQLiteConnection {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:stock_predictor.db";
    private static String dbUrl = DEFAULT_DB_URL;

    private SQLiteConnection() {
    }

    public static synchronized void setDbUrl(String customUrl) {
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            dbUrl = customUrl;
        }
    }

    public static Connection getConnection() throws DatabaseException {
        try {
            Connection conn = DriverManager.getConnection(dbUrl);
            try (Statement pragmaStmt = conn.createStatement()) {
                pragmaStmt.execute("PRAGMA foreign_keys = ON;");
            }
            return conn;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to connect to SQLite database: " + ex.getMessage(), ex);
        }
    }

    public static void initializeSchema() throws DatabaseException {
        String createStocksTable = """
                CREATE TABLE IF NOT EXISTS stocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    symbol TEXT NOT NULL,
                    notes TEXT DEFAULT '',
                    created_at INTEGER NOT NULL
                );
                """;

        String createPriceHistoryTable = """
                CREATE TABLE IF NOT EXISTS price_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stock_id INTEGER NOT NULL,
                    seq_index INTEGER NOT NULL,
                    price REAL NOT NULL,
                    recorded_at INTEGER NOT NULL,
                    FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE
                );
                """;

        String createPredictionsTable = """
                CREATE TABLE IF NOT EXISTS predictions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stock_id INTEGER NOT NULL,
                    current_price REAL NOT NULL,
                    predicted_price REAL NOT NULL,
                    slope REAL NOT NULL,
                    trend TEXT NOT NULL,
                    accuracy_percent REAL DEFAULT 0.0,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE
                );
                """;

        String createIndicatorsTable = """
                CREATE TABLE IF NOT EXISTS technical_indicators (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stock_id INTEGER NOT NULL,
                    sma_period INTEGER NOT NULL,
                    sma_value REAL,
                    ema_period INTEGER NOT NULL,
                    ema_value REAL,
                    rsi_period INTEGER NOT NULL,
                    rsi_value REAL,
                    macd_value REAL,
                    macd_signal REAL,
                    momentum_value REAL,
                    calculated_at INTEGER NOT NULL,
                    FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE CASCADE
                );
                """;

        String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createStocksTable);
            stmt.execute(createPriceHistoryTable);
            stmt.execute(createPredictionsTable);
            stmt.execute(createIndicatorsTable);
            stmt.execute(createSettingsTable);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize SQLite schema: " + ex.getMessage(), ex);
        }
    }
}
