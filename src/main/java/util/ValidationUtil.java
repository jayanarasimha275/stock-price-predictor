package util;

import model.Stock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Input parsing and validation utilities for stock names, symbols, prices, and CSV files.
 * Demonstrates method overloading and file handling with try-with-resources.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Parses raw text containing prices separated by commas, spaces, newlines, or semicolons.
     */
    public static List<Double> parsePrices(String rawText) throws ValidationException {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new ValidationException("Please enter at least 2 price values.");
        }

        String[] tokens = rawText.trim().split("[\\s,;\\n\\r\\t]+");
        List<Double> prices = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) {
                continue;
            }
            String sanitized = token.replaceAll("^[₹$€£,]+", "").replace(",", "");
            try {
                double val = Double.parseDouble(sanitized);
                if (!Double.isFinite(val)) {
                    errors.add("Token #" + (i + 1) + " is not a finite number.");
                } else if (val <= 0) {
                    errors.add("Price #" + (i + 1) + " (" + val + ") must be strictly > 0.");
                } else {
                    prices.add(val);
                }
            } catch (NumberFormatException ex) {
                errors.add("Token #" + (i + 1) + " (\"" + token + "\") is not a valid number.");
            }
        }

        if (!errors.isEmpty()) {
            int limit = Math.min(3, errors.size());
            throw new ValidationException("Invalid input: " + String.join("; ", errors.subList(0, limit)));
        }

        return prices;
    }

    /**
     * Overloaded parsePrices: extracts positive numeric prices from a selected CSV column.
     */
    public static List<Double> parsePrices(File csvFile, int columnIndex) throws ValidationException {
        if (csvFile == null || !csvFile.exists() || !csvFile.isFile()) {
            throw new ValidationException("Selected CSV file does not exist.");
        }

        List<String> lines = readNonEmptyLines(csvFile);
        if (lines.isEmpty()) {
            throw new ValidationException("The CSV file is empty.");
        }

        String[] firstTokens = splitCsvRow(lines.get(0));
        boolean hasHeader = false;
        for (String tok : firstTokens) {
            if (!isNumericToken(tok)) {
                hasHeader = true;
                break;
            }
        }

        int startIdx = hasHeader ? 1 : 0;
        int targetCol = Math.max(0, Math.min(columnIndex, firstTokens.length - 1));
        List<Double> extracted = new ArrayList<>();

        for (int i = startIdx; i < lines.size(); i++) {
            String[] cols = splitCsvRow(lines.get(i));
            if (cols.length > targetCol) {
                String cleaned = cols[targetCol].replaceAll("^[₹$€£,]+", "").replace(",", "").trim();
                try {
                    double num = Double.parseDouble(cleaned);
                    if (Double.isFinite(num) && num > 0) {
                        extracted.add(num);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip non-numeric trailing/malformed cells
                }
            }
        }

        if (extracted.isEmpty()) {
            throw new ValidationException("No valid positive prices found in column #" + (targetCol + 1) + ".");
        }
        return extracted;
    }

    public static List<String> readCsvHeaders(File csvFile) throws ValidationException {
        if (csvFile == null || !csvFile.exists()) {
            throw new ValidationException("CSV file not found.");
        }
        List<String> lines = readNonEmptyLines(csvFile);
        if (lines.isEmpty()) {
            throw new ValidationException("Uploaded CSV file is empty.");
        }
        String[] firstTokens = splitCsvRow(lines.get(0));
        boolean hasHeader = false;
        for (String col : firstTokens) {
            if (!isNumericToken(col)) {
                hasHeader = true;
                break;
            }
        }
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < firstTokens.length; i++) {
            headers.add(hasHeader ? firstTokens[i] : "Column " + (i + 1));
        }
        return headers;
    }

    public static double parseSinglePositivePrice(String raw) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Price value cannot be empty.");
        }
        String sanitized = raw.trim().replaceAll("^[₹$€£,]+", "").replace(",", "");
        try {
            double val = Double.parseDouble(sanitized);
            if (!Double.isFinite(val) || val <= 0) {
                throw new ValidationException("Price must be a finite number strictly greater than 0.");
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new ValidationException("\"" + raw.trim() + "\" is not a valid positive number.", ex);
        }
    }

    public static int parsePositiveInt(String raw, String fieldName, int min, int max) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < min || val > max) {
                throw new ValidationException(fieldName + " must be between " + min + " and " + max + ".");
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new ValidationException(fieldName + " must be a whole integer.", ex);
        }
    }

    public static void validateStockInput(String name, String symbol, List<Double> prices,
                                          List<Stock> existingStocks, String currentStockName) throws ValidationException {
        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty()) {
            throw new ValidationException("Stock name is required.");
        }
        String trimmedSymbol = symbol != null ? symbol.trim() : "";
        if (trimmedSymbol.isEmpty()) {
            throw new ValidationException("Stock symbol is required.");
        }

        if (existingStocks != null) {
            for (Stock s : existingStocks) {
                boolean matches = s.getName().equalsIgnoreCase(trimmedName);
                boolean isSameEdited = currentStockName != null && s.getName().equalsIgnoreCase(currentStockName);
                if (matches && !isSameEdited) {
                    throw new ValidationException("A stock named \"" + trimmedName + "\" already exists (case-insensitive).");
                }
            }
        }

        if (prices == null || prices.size() < 2) {
            throw new ValidationException("At least 2 historical price points are required for trend analysis.");
        }

        for (int i = 0; i < prices.size(); i++) {
            Double p = prices.get(i);
            if (p == null || !Double.isFinite(p) || p <= 0) {
                throw new ValidationException("Price at index " + (i + 1) + " must be a finite number > 0.");
            }
        }
    }

    private static List<String> readNonEmptyLines(File file) throws ValidationException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        } catch (IOException ex) {
            throw new ValidationException("Failed to read file: " + ex.getMessage(), ex);
        }
        return lines;
    }

    private static String[] splitCsvRow(String row) {
        String[] parts = row.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim().replaceAll("^[\"']|[\"']$", "");
        }
        return parts;
    }

    private static boolean isNumericToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String cleaned = token.replaceAll("^[₹$€£,]+", "").replace(",", "").trim();
        try {
            Double.parseDouble(cleaned);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static String validateFullName(String fullName) throws ValidationException {
        String trimmed = fullName != null ? fullName.trim() : "";
        if (trimmed.length() < 2 || trimmed.length() > 80) {
            throw new ValidationException("Full name must be between 2 and 80 characters.");
        }
        return trimmed;
    }

    public static String validateUsername(String username) throws ValidationException {
        String trimmed = username != null ? username.trim() : "";
        if (!trimmed.matches("^[A-Za-z0-9_.-]{3,32}$")) {
            throw new ValidationException("Username must be 3–32 characters (letters, digits, '.', '_', '-' only, no spaces).");
        }
        return trimmed;
    }

    public static String validateEmail(String email) throws ValidationException {
        String trimmed = email != null ? email.trim().toLowerCase() : "";
        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Please enter a valid email address (e.g. user@example.com).");
        }
        return trimmed;
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
    }

    public static void validatePasswordWithConfirmation(String password, String confirmPassword)
            throws ValidationException {
        validatePassword(password);
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            throw new ValidationException("Password and Confirm Password do not match.");
        }
    }
}

