package ui;

/**
 * Callback interface for navigating between AWT predictor modules and synchronizing selected stocks.
 */
public interface StockSelectionListener {
    void onNavigate(String moduleId, String stockName);
    void onPortfolioChanged(String preferredSelectedStock);
}
