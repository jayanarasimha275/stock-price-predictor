package ui;

import database.DatabaseSeeder;
import model.Prediction;
import model.PriceChangeSummary;
import model.Stock;
import model.TrendDirection;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.DatabaseException;

import java.awt.*;
import java.util.List;

/**
 * Module 1: All Stocks Overview.
 * Displays Total Stocks, Upward Trends, Downward Trends, Neutral Trends,
 * and a responsive grid of StockCard components with sparklines.
 */
public class DashboardPanel extends BaseModulePanel {

    public DashboardPanel(StockService stockService, PredictionService predictionService,
                          IndicatorService indicatorService, HistoryService historyService,
                          StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "overview";
    }

    @Override
    public String getModuleTitle() {
        return "All Stocks Overview";
    }

    @Override
    public void refreshData() {
        removeAll();
        setBackground(palette.getPageBg());

        Button addAnotherBtn = createStyledButton("+ Add Stock", palette.getAccentViolet(), Color.WHITE);
        addAnotherBtn.addActionListener(e -> navigator.onNavigate("add", null));
        add(buildModuleHeader(
                "All Stocks Overview",
                "Portfolio snapshot showing latest prices, predicted next prices, trends, and sparklines from SQLite.",
                addAnotherBtn
        ), BorderLayout.NORTH);

        List<Stock> stocks;
        try {
            stocks = stockService.getAllStocks();
        } catch (DatabaseException ex) {
            showErrorDialog("Database Error", ex.getMessage());
            validate();
            repaint();
            return;
        }

        if (stocks.isEmpty()) {
            add(buildEmptyStatePanel("All Stocks Overview", () -> {
                try {
                    DatabaseSeeder.loadSamplePortfolio(stockService);
                    navigator.onPortfolioChanged(null);
                } catch (DatabaseException ex) {
                    showErrorDialog("Database Error", ex.getMessage());
                }
            }), BorderLayout.CENTER);
            validate();
            repaint();
            return;
        }

        int upwardCount = 0;
        int downwardCount = 0;
        int flatCount = 0;

        Panel body = new Panel(new BorderLayout(0, 14));
        body.setBackground(palette.getPageBg());

        Panel cardsGrid = new Panel(new GridLayout(0, 3, 14, 14));
        cardsGrid.setBackground(palette.getPageBg());

        for (Stock stock : stocks) {
            Prediction pred = predictionService.computeTransientPrediction(stock);
            PriceChangeSummary chg = historyService.calculatePriceChange(stock);
            if (pred.getTrend() == TrendDirection.UPWARD) {
                upwardCount++;
            } else if (pred.getTrend() == TrendDirection.DOWNWARD) {
                downwardCount++;
            } else {
                flatCount++;
            }
            cardsGrid.add(new StockCard(stock, pred, chg, currency, palette, navigator));
        }

        Panel kpiRow = new Panel(new GridLayout(1, 4, 12, 12));
        kpiRow.setBackground(palette.getPageBg());
        kpiRow.add(createKpiCard("Total Stocks", String.valueOf(stocks.size()), "Stored in SQLite", palette.getTextPrimary()));
        kpiRow.add(createKpiCard("Upward Trends", String.valueOf(upwardCount), "Positive OLS slope", palette.getGainGreen()));
        kpiRow.add(createKpiCard("Downward Trends", String.valueOf(downwardCount), "Negative OLS slope", palette.getLossRed()));
        kpiRow.add(createKpiCard("Neutral / Flat", String.valueOf(flatCount), "Zero net slope", palette.getFlatAmber()));

        body.add(kpiRow, BorderLayout.NORTH);

        Panel gridWrapper = new Panel(new BorderLayout());
        gridWrapper.setBackground(palette.getPageBg());
        gridWrapper.add(cardsGrid, BorderLayout.NORTH);
        body.add(gridWrapper, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        validate();
        repaint();
    }
}
