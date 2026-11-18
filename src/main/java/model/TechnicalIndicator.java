package model;

/**
 * Holds calculated technical indicators for a stock (SMA, EMA, Momentum, RSI, MACD, Slope).
 */
public class TechnicalIndicator {
    private int id;
    private int stockId;
    private int smaPeriod;
    private double smaValue;
    private boolean smaValid;

    private int emaPeriod;
    private double emaValue;
    private boolean emaValid;

    private int momentumPeriod;
    private double momentumValue;
    private boolean momentumValid;

    private int rsiPeriod;
    private double rsiValue;
    private boolean rsiValid;

    private int macdFastPeriod;
    private int macdSlowPeriod;
    private int macdSignalPeriod;
    private double macdLine;
    private double macdSignal;
    private double macdHistogram;
    private boolean macdValid;

    private double slope;
    private long calculatedAt;

    public TechnicalIndicator(int stockId) {
        this.stockId = stockId;
        this.calculatedAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public int getSmaPeriod() { return smaPeriod; }
    public void setSmaPeriod(int smaPeriod) { this.smaPeriod = smaPeriod; }

    public double getSmaValue() { return smaValue; }
    public void setSmaValue(double smaValue) { this.smaValue = smaValue; }

    public boolean isSmaValid() { return smaValid; }
    public void setSmaValid(boolean smaValid) { this.smaValid = smaValid; }

    public int getEmaPeriod() { return emaPeriod; }
    public void setEmaPeriod(int emaPeriod) { this.emaPeriod = emaPeriod; }

    public double getEmaValue() { return emaValue; }
    public void setEmaValue(double emaValue) { this.emaValue = emaValue; }

    public boolean isEmaValid() { return emaValid; }
    public void setEmaValid(boolean emaValid) { this.emaValid = emaValid; }

    public int getMomentumPeriod() { return momentumPeriod; }
    public void setMomentumPeriod(int momentumPeriod) { this.momentumPeriod = momentumPeriod; }

    public double getMomentumValue() { return momentumValue; }
    public void setMomentumValue(double momentumValue) { this.momentumValue = momentumValue; }

    public boolean isMomentumValid() { return momentumValid; }
    public void setMomentumValid(boolean momentumValid) { this.momentumValid = momentumValid; }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }

    public double getRsiValue() { return rsiValue; }
    public void setRsiValue(double rsiValue) { this.rsiValue = rsiValue; }

    public boolean isRsiValid() { return rsiValid; }
    public void setRsiValid(boolean rsiValid) { this.rsiValid = rsiValid; }

    public int getMacdFastPeriod() { return macdFastPeriod; }
    public void setMacdFastPeriod(int macdFastPeriod) { this.macdFastPeriod = macdFastPeriod; }

    public int getMacdSlowPeriod() { return macdSlowPeriod; }
    public void setMacdSlowPeriod(int macdSlowPeriod) { this.macdSlowPeriod = macdSlowPeriod; }

    public int getMacdSignalPeriod() { return macdSignalPeriod; }
    public void setMacdSignalPeriod(int macdSignalPeriod) { this.macdSignalPeriod = macdSignalPeriod; }

    public double getMacdLine() { return macdLine; }
    public void setMacdLine(double macdLine) { this.macdLine = macdLine; }

    public double getMacdSignal() { return macdSignal; }
    public void setMacdSignal(double macdSignal) { this.macdSignal = macdSignal; }

    public double getMacdHistogram() { return macdHistogram; }
    public void setMacdHistogram(double macdHistogram) { this.macdHistogram = macdHistogram; }

    public boolean isMacdValid() { return macdValid; }
    public void setMacdValid(boolean macdValid) { this.macdValid = macdValid; }

    public double getSlope() { return slope; }
    public void setSlope(double slope) { this.slope = slope; }

    public long getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(long calculatedAt) { this.calculatedAt = calculatedAt; }
}
