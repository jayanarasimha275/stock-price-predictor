package repository;

import database.SQLiteConnection;
import util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists user preferences (currency, theme, initial seeder status) in SQLite app_settings table.
 */
public class SettingsRepository {

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (DatabaseException | SQLException ignored) {
            // Fall back to default value if settings read fails
        }
        return defaultValue;
    }

    public void setSetting(String key, String value) {
        String sql = """
                INSERT INTO app_settings (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (DatabaseException | SQLException ignored) {
            // Non-fatal preference save
        }
    }
}
