package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core domain entity representing a user-defined Stock with chronological closing price history.
 */
public class Stock {
    private int id;
    private String name;
    private String symbol;
    private String notes;
    private long createdAt;
    private final List<Double> history;

    public Stock(String name, String symbol, List<Double> history) {
        this(0, name, symbol, "", System.currentTimeMillis(), history);
    }

    public Stock(String name, String symbol, String notes, List<Double> history) {
        this(0, name, symbol, notes, System.currentTimeMillis(), history);
    }

    public Stock(int id, String name, String symbol, String notes, long createdAt, List<Double> history) {
        this.id = id;
        this.name = name != null ? name.trim() : "";
        this.symbol = (symbol != null && !symbol.trim().isEmpty()) ? symbol.trim().toUpperCase() : this.name.toUpperCase();
        this.notes = notes != null ? notes : "";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.history = new ArrayList<>();
        if (history != null) {
            this.history.addAll(history);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol != null ? symbol.trim().toUpperCase() : "";
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<Double> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void setHistory(List<Double> prices) {
        this.history.clear();
        if (prices != null) {
            this.history.addAll(prices);
        }
    }

    public int getPointCount() {
        return history.size();
    }

    public double getLatestPrice() {
        if (history.isEmpty()) {
            return 0.0;
        }
        return history.get(history.size() - 1);
    }

    public double getFirstPrice() {
        if (history.isEmpty()) {
            return 0.0;
        }
        return history.get(0);
    }

    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}
