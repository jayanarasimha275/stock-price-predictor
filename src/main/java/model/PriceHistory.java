package model;

/**
 * Represents a single persisted historical price entry in the price_history table.
 */
public class PriceHistory {
    private int id;
    private int stockId;
    private int seqIndex;
    private double price;
    private long recordedAt;

    public PriceHistory(int stockId, int seqIndex, double price) {
        this(0, stockId, seqIndex, price, System.currentTimeMillis());
    }

    public PriceHistory(int id, int stockId, int seqIndex, double price, long recordedAt) {
        this.id = id;
        this.stockId = stockId;
        this.seqIndex = seqIndex;
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public int getSeqIndex() {
        return seqIndex;
    }

    public void setSeqIndex(int seqIndex) {
        this.seqIndex = seqIndex;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(long recordedAt) {
        this.recordedAt = recordedAt;
    }
}
