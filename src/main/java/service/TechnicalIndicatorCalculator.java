package service;

import java.util.List;

/**
 * Interface defining polymorphic technical indicator calculation over a price series.
 */
public interface TechnicalIndicatorCalculator {
    String getIndicatorName();
    boolean hasEnoughData(List<Double> prices, int period);
    double calculate(List<Double> prices, int period);
}
