package model;

/**
 * Summary of overall and daily price changes for a stock.
 */
public class PriceChangeSummary {
    private final String stockName;
    private final String stockSymbol;
    private final double firstPrice;
    private final double latestPrice;
    private final double previousPrice;
    private final double dailyChange;
    private final double dailyPercentChange;
    private final double absoluteChange;
    private final double percentChange;
    private final double minPrice;
    private final double maxPrice;
    private final double spread;
    private final int points;

    public PriceChangeSummary(String stockName, String stockSymbol, double firstPrice, double latestPrice,
                              double previousPrice, double minPrice, double maxPrice, int points) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.firstPrice = firstPrice;
        this.latestPrice = latestPrice;
        this.previousPrice = previousPrice;
        this.dailyChange = points >= 2 ? latestPrice - previousPrice : 0.0;
        this.dailyPercentChange = (points >= 2 && previousPrice > 0) ? (dailyChange / previousPrice) * 100.0 : 0.0;
        this.absoluteChange = latestPrice - firstPrice;
        this.percentChange = firstPrice > 0 ? (absoluteChange / firstPrice) * 100.0 : 0.0;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.spread = maxPrice - minPrice;
        this.points = points;
    }

    public String getStockName() { return stockName; }
    public String getStockSymbol() { return stockSymbol; }
    public double getFirstPrice() { return firstPrice; }
    public double getLatestPrice() { return latestPrice; }
    public double getPreviousPrice() { return previousPrice; }
    public double getDailyChange() { return dailyChange; }
    public double getDailyPercentChange() { return dailyPercentChange; }
    public double getAbsoluteChange() { return absoluteChange; }
    public double getPercentChange() { return percentChange; }
    public double getMinPrice() { return minPrice; }
    public double getMaxPrice() { return maxPrice; }
    public double getSpread() { return spread; }
    public int getPoints() { return points; }
}
