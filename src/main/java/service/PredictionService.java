package service;

import model.AccuracyReport;
import model.Prediction;
import model.Stock;
import model.TrendDirection;
import repository.PredictionRepository;
import util.DatabaseException;

import java.util.List;

/**
 * Implements Ordinary Least Squares (OLS) linear trend slope, next-price prediction,
 * trend classification, and backtesting accuracy algorithms, persisting predictions to SQLite.
 */
public class PredictionService {

    private final PredictionRepository predictionRepository;

    public PredictionService() {
        this.predictionRepository = new PredictionRepository();
    }

    public PredictionService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    /**
     * Linear regression slope with x = 0..n-1.
     * If n < 2 returns 0.
     * den = n * sum(x^2) - (sum(x))^2; if |den| < 1e-9 returns 0.
     */
    public double calculateSlope(List<Double> prices) {
        if (prices == null || prices.size() < 2) {
            return 0.0;
        }
        int n = prices.size();
        double sumX = 0.0;
        double sumX2 = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = prices.get(i);
            sumX += x;
            sumX2 += x * x;
            sumY += y;
            sumXY += x * y;
        }

        double den = n * sumX2 - (sumX * sumX);
        if (Math.abs(den) < 1e-9) {
            return 0.0;
        }
        return (n * sumXY - (sumX * sumY)) / den;
    }

    /**
     * Predicts next price: if length < 2 returns last value (or 0 if empty);
     * otherwise returns last value + calculateSlope(prices).
     */
    public double predictNextPrice(List<Double> prices) {
        if (prices == null || prices.isEmpty()) {
            return 0.0;
        }
        if (prices.size() < 2) {
            return prices.get(0);
        }
        double lastValue = prices.get(prices.size() - 1);
        double slope = calculateSlope(prices);
        return lastValue + slope;
    }

    /**
     * Determines trend direction: predicted > last -> Upward, < last -> Downward, equal -> Flat.
     */
    public TrendDirection getTrendDirection(double predicted, double last) {
        double diff = predicted - last;
        if (Math.abs(diff) < 1e-5) {
            return TrendDirection.FLAT;
        }
        return diff > 0 ? TrendDirection.UPWARD : TrendDirection.DOWNWARD;
    }

    /**
     * Backtesting accuracy: requires at least 3 prices.
     * Trains on all prices except the last, predicts next, compares with actual last price.
     */
    public AccuracyReport calculateAccuracy(List<Double> prices) {
        if (prices == null || prices.size() < 3) {
            int count = prices != null ? prices.size() : 0;
            double last = count > 0 ? prices.get(count - 1) : 0.0;
            return new AccuracyReport(
                    false, 0.0, last, 0.0, 0.0, 0, 0.0,
                    "Requires at least 3 historical price points (currently has " + count + ")."
            );
        }

        List<Double> trainData = prices.subList(0, prices.size() - 1);
        double actual = prices.get(prices.size() - 1);
        double trainSlope = calculateSlope(trainData);
        double predicted = predictNextPrice(trainData);
        double error = Math.abs(predicted - actual);
        double accuracyPercent = actual > 0 ? Math.max(0.0, 100.0 - (error / actual) * 100.0) : 0.0;

        return new AccuracyReport(
                true, predicted, actual, error, accuracyPercent, trainData.size(), trainSlope, null
        );
    }

    /**
     * Computes prediction + accuracy for a stock and persists/updates it in SQLite predictions table.
     */
    public Prediction computeAndPersistPrediction(Stock stock) throws DatabaseException {
        List<Double> history = stock.getHistory();
        double current = stock.getLatestPrice();
        double slope = calculateSlope(history);
        double predicted = predictNextPrice(history);
        TrendDirection trend = getTrendDirection(predicted, current);
        AccuracyReport acc = calculateAccuracy(history);

        Prediction prediction = new Prediction(
                stock.getId(),
                stock.getName(),
                current,
                predicted,
                slope,
                trend,
                acc.isCanCalculate() ? acc.getAccuracyPercent() : 0.0
        );
        if (stock.getId() > 0) {
            predictionRepository.saveOrUpdate(prediction);
        }
        return prediction;
    }

    public Prediction computeTransientPrediction(Stock stock) {
        List<Double> history = stock.getHistory();
        double current = stock.getLatestPrice();
        double slope = calculateSlope(history);
        double predicted = predictNextPrice(history);
        TrendDirection trend = getTrendDirection(predicted, current);
        AccuracyReport acc = calculateAccuracy(history);

        return new Prediction(
                stock.getId(),
                stock.getName(),
                current,
                predicted,
                slope,
                trend,
                acc.isCanCalculate() ? acc.getAccuracyPercent() : 0.0
        );
    }

    public List<Prediction> getAllPredictions() throws DatabaseException {
        return predictionRepository.findAllWithStockInfo();
    }

    public List<Prediction> getPredictionsByStockId(int stockId, String stockName) throws DatabaseException {
        return predictionRepository.findByStockId(stockId, stockName);
    }

    public int getTotalPredictionCount() throws DatabaseException {
        return predictionRepository.countAll();
    }
}

