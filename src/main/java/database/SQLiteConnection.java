package database;

import util.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages SQLite JDBC connections, schema initialization, and non-destructive schema migrations.
 * Maintains tables: stocks, price_history, predictions, technical_indicators, app_settings, users.
 */
public final class SQLiteConnection {

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:stock_predictor.db";
    private static final int CURRENT_SCHEMA_VERSION = 2;
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

            migrateSchema(conn);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to initialize SQLite schema: " + ex.getMessage(), ex);
        }
    }

    /**
     * Applies additive, non-destructive migrations without removing or modifying existing stock records.
     */
    private static void migrateSchema(Connection conn) throws SQLException {
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER' CHECK (role IN ('ADMIN', 'USER')),
                    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                    created_at INTEGER NOT NULL
                );
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);

            // Verify columns in case an older partial users table exists
            Set<String> cols = readColumns(conn, "users");
            if (!cols.contains("full_name")) {
                stmt.execute("ALTER TABLE users ADD COLUMN full_name TEXT NOT NULL DEFAULT '';");
            }
            if (!cols.contains("password_salt")) {
                stmt.execute("ALTER TABLE users ADD COLUMN password_salt TEXT NOT NULL DEFAULT '';");
            }
            if (!cols.contains("role")) {
                stmt.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER';");
            }
            if (!cols.contains("status")) {
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';");
            }
            if (!cols.contains("created_at")) {
                stmt.execute("ALTER TABLE users ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0;");
            }

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_predictions_stock_created ON predictions(stock_id, created_at DESC);");
        }

        String upsertVersion = """
                INSERT INTO app_settings (key, value) VALUES ('schema_version', ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;
        try (PreparedStatement ps = conn.prepareStatement(upsertVersion)) {
            ps.setString(1, String.valueOf(CURRENT_SCHEMA_VERSION));
            ps.executeUpdate();
        }
    }

    private static Set<String> readColumns(Connection conn, String tableName) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ");")) {
            while (rs.next()) {
                cols.add(rs.getString("name").toLowerCase());
            }
        }
        return cols;
    }
}

