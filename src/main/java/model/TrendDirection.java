package model;

/**
 * Trend classification derived from comparing predicted price vs latest price.
 */
public enum TrendDirection {
    UPWARD("Upward"),
    DOWNWARD("Downward"),
    FLAT("Flat");

    private final String label;

    TrendDirection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TrendDirection fromLabel(String label) {
        if (label != null) {
            for (TrendDirection t : values()) {
                if (t.label.equalsIgnoreCase(label.trim())) {
                    return t;
                }
            }
        }
        return FLAT;
    }
}
