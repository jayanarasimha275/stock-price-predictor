package util;

import model.Currency;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for formatting monetary amounts and percentage changes.
 * Demonstrates method overloading across currency and decimal precisions.
 */
public final class CurrencyConverter {

    private CurrencyConverter() {
    }

    public static String format(double value, Currency currency) {
        return format(value, currency, 2);
    }

    public static String format(double value, Currency currency, int decimals) {
        String symbol = currency != null ? currency.getSymbol() : "₹";
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return symbol + "0.00";
        }
        String pattern = buildPattern(decimals);
        DecimalFormat df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        return symbol + df.format(value);
    }

    public static String formatSigned(double value, Currency currency) {
        return formatSigned(value, currency, 2);
    }

    public static String formatSigned(double value, Currency currency, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return format(0.0, currency, decimals);
        }
        String sign = value > 0 ? "+" : (value < 0 ? "-" : "");
        return sign + format(Math.abs(value), currency, decimals);
    }

    public static String formatSignedPercent(double value) {
        return formatSignedPercent(value, 2);
    }

    public static String formatSignedPercent(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0.00%";
        }
        String sign = value > 0 ? "+" : (value < 0 ? "-" : "");
        return String.format(Locale.US, "%s%." + Math.max(0, decimals) + "f%%", sign, Math.abs(value));
    }

    private static String buildPattern(int decimals) {
        if (decimals <= 0) {
            return "#,##0";
        }
        StringBuilder sb = new StringBuilder("#,##0.");
        for (int i = 0; i < decimals; i++) {
            sb.append('0');
        }
        return sb.toString();
    }
}
