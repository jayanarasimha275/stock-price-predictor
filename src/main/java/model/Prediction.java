package model;

/**
 * Represents a next-step stock price prediction persisted in the predictions table.
 */
public class Prediction {
    private int id;
    private int stockId;
    private String stockName;
    private double currentPrice;
    private double predictedPrice;
    private double slope;
    private TrendDirection trend;
    private double accuracyPercent;
    private long createdAt;

    public Prediction(int stockId, String stockName, double currentPrice, double predictedPrice,
                      double slope, TrendDirection trend, double accuracyPercent) {
        this(0, stockId, stockName, currentPrice, predictedPrice, slope, trend, accuracyPercent, System.currentTimeMillis());
    }

    public Prediction(int id, int stockId, String stockName, double currentPrice, double predictedPrice,
                      double slope, TrendDirection trend, double accuracyPercent, long createdAt) {
        this.id = id;
        this.stockId = stockId;
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.predictedPrice = predictedPrice;
        this.slope = slope;
        this.trend = trend != null ? trend : TrendDirection.FLAT;
        this.accuracyPercent = accuracyPercent;
        this.createdAt = createdAt;
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

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(double predictedPrice) {
        this.predictedPrice = predictedPrice;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public TrendDirection getTrend() {
        return trend;
    }

    public void setTrend(TrendDirection trend) {
        this.trend = trend;
    }

    public double getDifference() {
        return predictedPrice - currentPrice;
    }

    public double getPercentChange() {
        return currentPrice > 0 ? (getDifference() / currentPrice) * 100.0 : 0.0;
    }

    public double getAccuracyPercent() {
        return accuracyPercent;
    }

    public void setAccuracyPercent(double accuracyPercent) {
        this.accuracyPercent = accuracyPercent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
