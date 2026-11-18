package model;

/**
 * Authorization roles supported by the Stock Price Predictor application.
 */
public enum UserRole {
    ADMIN("ADMIN"),
    USER("USER");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static UserRole fromString(String value) {
        if (value != null && "ADMIN".equalsIgnoreCase(value.trim())) {
            return ADMIN;
        }
        return USER;
    }
}
