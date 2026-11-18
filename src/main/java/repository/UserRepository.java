package repository;

import database.SQLiteConnection;
import model.User;
import model.UserRole;
import model.UserStatus;
import util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Repository managing CRUD, search, role counts, and credential updates in SQLite users table.
 */
public class UserRepository implements Repository<User, Integer> {

    @Override
    public List<User> findAll() throws DatabaseException {
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users
                ORDER BY id ASC
                """;
        List<User> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch users: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<User> search(String query) throws DatabaseException {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users
                WHERE LOWER(full_name) LIKE ? OR LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(role) LIKE ?
                ORDER BY id ASC
                """;
        List<User> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to search users: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<User> findRecent(int limit) throws DatabaseException {
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;
        List<User> list = new ArrayList<>();
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to fetch recent registrations: " + ex.getMessage(), ex);
        }
        return list;
    }

    @Override
    public Optional<User> findById(Integer id) throws DatabaseException {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users WHERE id = ?
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to find user by ID: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsernameIgnoreCase(String username) throws DatabaseException {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users WHERE LOWER(username) = LOWER(?)
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to find user by username: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmailIgnoreCase(String email) throws DatabaseException {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, full_name, username, email, password_hash, password_salt, role, status, created_at
                FROM users WHERE LOWER(email) = LOWER(?)
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to find user by email: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public int countAll() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to count users: " + ex.getMessage(), ex);
        }
    }

    public int countByRole(UserRole role) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE UPPER(role) = UPPER(?)";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to count users by role: " + ex.getMessage(), ex);
        }
    }

    public int countActiveAdmins() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM users WHERE UPPER(role) = 'ADMIN' AND UPPER(status) = 'ACTIVE'";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to count active administrators: " + ex.getMessage(), ex);
        }
    }

    @Override
    public User save(User user) throws DatabaseException {
        String sql = """
                INSERT INTO users (full_name, username, email, password_hash, password_salt, role, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPasswordSalt());
            ps.setString(6, user.getRole().getLabel());
            ps.setString(7, user.getStatus().getLabel());
            ps.setLong(8, user.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to create user \"" + user.getUsername() + "\": " + ex.getMessage(), ex);
        }
    }

    public void updateProfile(User user) throws DatabaseException {
        String sql = """
                UPDATE users
                SET full_name = ?, username = ?, email = ?, role = ?, status = ?
                WHERE id = ?
                """;
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole().getLabel());
            ps.setString(5, user.getStatus().getLabel());
            ps.setInt(6, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update user \"" + user.getUsername() + "\": " + ex.getMessage(), ex);
        }
    }

    public void updatePassword(int userId, String passwordHash, String passwordSalt) throws DatabaseException {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, passwordSalt);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update password: " + ex.getMessage(), ex);
        }
    }

    public void updateStatus(int userId, UserStatus status) throws DatabaseException {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getLabel());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to update user status: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteById(Integer id) throws DatabaseException {
        if (id == null || id <= 0) {
            return;
        }
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = SQLiteConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to delete user ID " + id + ": " + ex.getMessage(), ex);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                UserRole.fromString(rs.getString("role")),
                UserStatus.fromString(rs.getString("status")),
                rs.getLong("created_at")
        );
    }
}
