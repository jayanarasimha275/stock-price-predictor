package model;

/**
 * Domain entity representing an authenticated application user or administrator
 * stored in the SQLite users table.
 */
public class User {
    private int id;
    private String fullName;
    private String username;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private UserRole role;
    private UserStatus status;
    private long createdAt;

    public User(String fullName, String username, String email,
                String passwordHash, String passwordSalt,
                UserRole role, UserStatus status) {
        this(0, fullName, username, email, passwordHash, passwordSalt,
                role, status, System.currentTimeMillis());
    }

    public User(int id, String fullName, String username, String email,
                String passwordHash, String passwordSalt,
                UserRole role, UserStatus status, long createdAt) {
        this.id = id;
        this.fullName = fullName != null ? fullName.trim() : "";
        this.username = username != null ? username.trim() : "";
        this.email = email != null ? email.trim().toLowerCase() : "";
        this.passwordHash = passwordHash != null ? passwordHash : "";
        this.passwordSalt = passwordSalt != null ? passwordSalt : "";
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : "";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : "";
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash != null ? passwordHash : "";
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt != null ? passwordSalt : "";
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role != null ? role : UserRole.USER;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status != null ? status : UserStatus.ACTIVE;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return fullName + " (@" + username + ") [" + role.getLabel() + "]";
    }
}
