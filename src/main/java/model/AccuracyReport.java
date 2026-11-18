package model;

/**
 * Value object containing backtesting accuracy metrics.
 */
public class AccuracyReport {
    private final boolean canCalculate;
    private final double predicted;
    private final double actual;
    private final double error;
    private final double accuracyPercent;
    private final int trainPoints;
    private final double trainSlope;
    private final String reason;

    public AccuracyReport(boolean canCalculate, double predicted, double actual, double error,
                          double accuracyPercent, int trainPoints, double trainSlope, String reason) {
        this.canCalculate = canCalculate;
        this.predicted = predicted;
        this.actual = actual;
        this.error = error;
        this.accuracyPercent = accuracyPercent;
        this.trainPoints = trainPoints;
        this.trainSlope = trainSlope;
        this.reason = reason;
    }

    public boolean isCanCalculate() { return canCalculate; }
    public double getPredicted() { return predicted; }
    public double getActual() { return actual; }
    public double getError() { return error; }
    public double getAccuracyPercent() { return accuracyPercent; }
    public int getTrainPoints() { return trainPoints; }
    public double getTrainSlope() { return trainSlope; }
    public String getReason() { return reason; }
}
