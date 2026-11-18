package model;

/**
 * Account lifecycle statuses for registered users.
 */
public enum UserStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public static UserStatus fromString(String value) {
        if (value != null && "INACTIVE".equalsIgnoreCase(value.trim())) {
            return INACTIVE;
        }
        return ACTIVE;
    }
}
