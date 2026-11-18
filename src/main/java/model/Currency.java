package model;

/**
 * Supported display currencies for stock prices and reports.
 */
public enum Currency {
    INR("INR", "₹", "Indian Rupee"),
    USD("USD", "$", "US Dollar");

    private final String code;
    private final String symbol;
    private final String displayName;

    Currency(String code, String symbol, String displayName) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Currency fromCode(String code) {
        if ("USD".equalsIgnoreCase(code)) {
            return USD;
        }
        return INR;
    }
}
