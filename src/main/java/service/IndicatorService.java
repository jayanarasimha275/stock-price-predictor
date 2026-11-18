package service;

import model.Stock;
import model.TechnicalIndicator;
import repository.IndicatorRepository;
import util.DatabaseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Service calculating SMA, EMA, Momentum, RSI, MACD, and Slope,
 * using polymorphic TechnicalIndicatorCalculator implementations and persisting to SQLite.
 */
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final PredictionService predictionService;

    private final TechnicalIndicatorCalculator smaCalculator = new SmaCalculator();
    private final TechnicalIndicatorCalculator emaCalculator = new EmaCalculator();
    private final TechnicalIndicatorCalculator momentumCalculator = new MomentumCalculator();
    private final TechnicalIndicatorCalculator rsiCalculator = new RsiCalculator();

    public IndicatorService() {
        this.indicatorRepository = new IndicatorRepository();
        this.predictionService = new PredictionService();
    }

    public IndicatorService(IndicatorRepository indicatorRepository, PredictionService predictionService) {
        this.indicatorRepository = indicatorRepository;
        this.predictionService = predictionService;
    }

    public double calculateSMA(List<Double> d, int p) {
        return smaCalculator.calculate(d, p);
    }

    public double calculateEMA(List<Double> d, int p) {
        return emaCalculator.calculate(d, p);
    }

    public double calculateMomentum(List<Double> d, int p) {
        return momentumCalculator.calculate(d, p);
    }

    public double calculateRSI(List<Double> d, int p) {
        return rsiCalculator.calculate(d, p);
    }

    /**
     * Computes MACD (line, signal, histogram) for customizable fast, slow, and signal periods.
     * Standard defaults: fast=12, slow=26, signal=9.
     */
    public double[] calculateMACD(List<Double> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (prices == null || fastPeriod <= 0 || slowPeriod <= fastPeriod || signalPeriod <= 0
                || prices.size() < slowPeriod) {
            return new double[]{0.0, 0.0, 0.0};
        }

        List<Double> macdSeries = calculateMACDLineSeries(prices, fastPeriod, slowPeriod);
        List<Double> validMacd = new ArrayList<>();
        for (Double v : macdSeries) {
            if (v != null) {
                validMacd.add(v);
            }
        }
        if (validMacd.isEmpty()) {
            return new double[]{0.0, 0.0, 0.0};
        }
        double latestMacd = validMacd.get(validMacd.size() - 1);
        double signal = validMacd.size() >= signalPeriod
                ? calculateEMA(validMacd, signalPeriod)
                : calculateSMA(validMacd, validMacd.size());
        double histogram = latestMacd - signal;
        return new double[]{latestMacd, signal, histogram};
    }

    public List<Double> calculateSMASeries(List<Double> prices, int period) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            List<Double> sub = prices.subList(0, i + 1);
            series.add(smaCalculator.hasEnoughData(sub, period) ? smaCalculator.calculate(sub, period) : null);
        }
        return series;
    }

    public List<Double> calculateEMASeries(List<Double> prices, int period) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            List<Double> sub = prices.subList(0, i + 1);
            series.add(emaCalculator.hasEnoughData(sub, period) ? emaCalculator.calculate(sub, period) : null);
        }
        return series;
    }

    public List<Double> calculateRSISeries(List<Double> prices, int period) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            List<Double> sub = prices.subList(0, i + 1);
            series.add(rsiCalculator.hasEnoughData(sub, period) ? rsiCalculator.calculate(sub, period) : null);
        }
        return series;
    }

    public List<Double> calculateMACDLineSeries(List<Double> prices, int fastPeriod, int slowPeriod) {
        List<Double> series = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            List<Double> sub = prices.subList(0, i + 1);
            if (sub.size() >= slowPeriod && fastPeriod > 0 && slowPeriod > fastPeriod) {
                double fastEma = calculateEMA(sub, fastPeriod);
                double slowEma = calculateEMA(sub, slowPeriod);
                series.add(fastEma - slowEma);
            } else {
                series.add(null);
            }
        }
        return series;
    }

    public List<Double> calculateMACDSignalSeries(List<Double> macdLineSeries, int signalPeriod) {
        List<Double> result = new ArrayList<>();
        List<Double> validBuffer = new ArrayList<>();
        for (Double macdVal : macdLineSeries) {
            if (macdVal == null) {
                result.add(null);
            } else {
                validBuffer.add(macdVal);
                if (validBuffer.size() >= signalPeriod) {
                    result.add(calculateEMA(validBuffer, signalPeriod));
                } else {
                    result.add(null);
                }
            }
        }
        return result;
    }

    public TechnicalIndicator computeAndPersist(Stock stock, int smaPeriod, int emaPeriod,
                                                int momentumPeriod, int rsiPeriod,
                                                int macdFast, int macdSlow, int macdSignal) throws DatabaseException {
        TechnicalIndicator ind = computeTransient(stock, smaPeriod, emaPeriod, momentumPeriod, rsiPeriod,
                macdFast, macdSlow, macdSignal);
        if (stock.getId() > 0) {
            indicatorRepository.saveOrUpdate(ind);
        }
        return ind;
    }

    public TechnicalIndicator computeTransient(Stock stock, int smaPeriod, int emaPeriod,
                                               int momentumPeriod, int rsiPeriod,
                                               int macdFast, int macdSlow, int macdSignal) {
        List<Double> prices = stock.getHistory();
        TechnicalIndicator ind = new TechnicalIndicator(stock.getId());
        ind.setSmaPeriod(smaPeriod);
        ind.setEmaPeriod(emaPeriod);
        ind.setMomentumPeriod(momentumPeriod);
        ind.setRsiPeriod(rsiPeriod);
        ind.setMacdFastPeriod(macdFast);
        ind.setMacdSlowPeriod(macdSlow);
        ind.setMacdSignalPeriod(macdSignal);

        boolean smaValid = smaCalculator.hasEnoughData(prices, smaPeriod);
        ind.setSmaValid(smaValid);
        ind.setSmaValue(smaValid ? smaCalculator.calculate(prices, smaPeriod) : 0.0);

        boolean emaValid = emaCalculator.hasEnoughData(prices, emaPeriod);
        ind.setEmaValid(emaValid);
        ind.setEmaValue(emaValid ? emaCalculator.calculate(prices, emaPeriod) : 0.0);

        boolean momValid = momentumCalculator.hasEnoughData(prices, momentumPeriod);
        ind.setMomentumValid(momValid);
        ind.setMomentumValue(momValid ? momentumCalculator.calculate(prices, momentumPeriod) : 0.0);

        boolean rsiValid = rsiCalculator.hasEnoughData(prices, rsiPeriod);
        ind.setRsiValid(rsiValid);
        ind.setRsiValue(rsiValid ? rsiCalculator.calculate(prices, rsiPeriod) : 0.0);

        boolean macdValid = macdFast > 0 && macdSlow > macdFast && macdSignal > 0 && prices.size() >= macdSlow;
        ind.setMacdValid(macdValid);
        if (macdValid) {
            double[] macd = calculateMACD(prices, macdFast, macdSlow, macdSignal);
            ind.setMacdLine(macd[0]);
            ind.setMacdSignal(macd[1]);
            ind.setMacdHistogram(macd[2]);
        }

        ind.setSlope(predictionService.calculateSlope(prices));
        return ind;
    }

    // --- Polymorphic Indicator Implementations ---

    private static class SmaCalculator implements TechnicalIndicatorCalculator {
        @Override
        public String getIndicatorName() {
            return "Simple Moving Average (SMA)";
        }

        @Override
        public boolean hasEnoughData(List<Double> prices, int period) {
            return prices != null && period > 0 && prices.size() >= period;
        }

        @Override
        public double calculate(List<Double> prices, int period) {
            if (!hasEnoughData(prices, period)) {
                return 0.0;
            }
            double sum = 0.0;
            int start = prices.size() - period;
            for (int i = start; i < prices.size(); i++) {
                sum += prices.get(i);
            }
            return sum / period;
        }
    }

    private static class EmaCalculator implements TechnicalIndicatorCalculator {
        @Override
        public String getIndicatorName() {
            return "Exponential Moving Average (EMA)";
        }

        @Override
        public boolean hasEnoughData(List<Double> prices, int period) {
            return prices != null && period > 0 && prices.size() >= period;
        }

        @Override
        public double calculate(List<Double> prices, int period) {
            if (!hasEnoughData(prices, period)) {
                return 0.0;
            }
            double k = 2.0 / (period + 1.0);
            double sumFirstP = 0.0;
            for (int i = 0; i < period; i++) {
                sumFirstP += prices.get(i);
            }
            double ema = sumFirstP / period;
            for (int i = period; i < prices.size(); i++) {
                ema = (prices.get(i) - ema) * k + ema;
            }
            return ema;
        }
    }

    private static class MomentumCalculator implements TechnicalIndicatorCalculator {
        @Override
        public String getIndicatorName() {
            return "Momentum";
        }

        @Override
        public boolean hasEnoughData(List<Double> prices, int period) {
            return prices != null && period > 0 && prices.size() > period;
        }

        @Override
        public double calculate(List<Double> prices, int period) {
            if (!hasEnoughData(prices, period)) {
                return 0.0;
            }
            int lastIdx = prices.size() - 1;
            return prices.get(lastIdx) - prices.get(lastIdx - period);
        }
    }

    private static class RsiCalculator implements TechnicalIndicatorCalculator {
        @Override
        public String getIndicatorName() {
            return "Relative Strength Index (RSI)";
        }

        @Override
        public boolean hasEnoughData(List<Double> prices, int period) {
            return prices != null && period > 0 && prices.size() > period;
        }

        @Override
        public double calculate(List<Double> prices, int period) {
            if (!hasEnoughData(prices, period)) {
                return 0.0;
            }
            double avgGain = 0.0;
            double avgLoss = 0.0;
            for (int i = 1; i <= period; i++) {
                double delta = prices.get(i) - prices.get(i - 1);
                if (delta > 0) {
                    avgGain += delta;
                } else {
                    avgLoss += Math.abs(delta);
                }
            }
            avgGain /= period;
            avgLoss /= period;

            for (int i = period + 1; i < prices.size(); i++) {
                double delta = prices.get(i) - prices.get(i - 1);
                double gain = delta > 0 ? delta : 0.0;
                double loss = delta < 0 ? Math.abs(delta) : 0.0;
                avgGain = (avgGain * (period - 1) + gain) / period;
                avgLoss = (avgLoss * (period - 1) + loss) / period;
            }

            if (avgLoss < 1e-12) {
                return avgGain < 1e-12 ? 50.0 : 100.0;
            }
            double rs = avgGain / avgLoss;
            return 100.0 - (100.0 / (1.0 + rs));
        }
    }
}
