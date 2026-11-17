package ui;

import database.DatabaseSeeder;
import model.Prediction;
import model.Stock;
import model.TrendDirection;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;

import java.awt.*;
import java.util.List;

/**
 * Module 3: Predict Next Price.
 * Computes OLS linear regression slope, predicted next price, difference, % change,
 * persists prediction to SQLite, and renders the violet history + amber dashed forecast chart.
 */
public class PredictionPanel extends BaseModulePanel {

    private Choice stockChoice;

    public PredictionPanel(StockService stockService, PredictionService predictionService,
                           IndicatorService indicatorService, HistoryService historyService,
                           StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "predict";
    }

    @Override
    public String getModuleTitle() {
        return "Predict Next Price";
    }

    @Override
    public void refreshData() {
        removeAll();
        setBackground(palette.getPageBg());

        List<Stock> stocks;
        try {
            stocks = stockService.getAllStocks();
        } catch (DatabaseException ex) {
            showErrorDialog("Database Error", ex.getMessage());
            return;
        }

        if (stocks.isEmpty()) {
            add(buildModuleHeader("Predict Next Price", "OLS linear regression trend extrapolation.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Predict Next Price", () -> {
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

        Stock active = resolveActiveStock(stocks);
        this.stockChoice = createStyledChoice();
        for (Stock s : stocks) {
            stockChoice.add(s.getName());
        }
        stockChoice.select(active.getName());
        stockChoice.addItemListener(e -> {
            setSelectedStockName(stockChoice.getSelectedItem());
            refreshData();
        });

        Panel selectorWrap = new Panel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        selectorWrap.setBackground(palette.getPageBg());
        Label selLbl = new Label("Selected Stock:");
        selLbl.setFont(util.ThemePalette.FONT_BUTTON);
        selLbl.setForeground(palette.getTextSecondary());
        selLbl.setBackground(palette.getPageBg());
        selectorWrap.add(selLbl);
        selectorWrap.add(stockChoice);

        add(buildModuleHeader(
                "Predict Next Price — " + active.getName() + " [" + active.getSymbol() + "]",
                "Ordinary Least Squares trend extrapolation saved to SQLite predictions table.",
                selectorWrap
        ), BorderLayout.NORTH);

        Prediction prediction;
        try {
            prediction = predictionService.computeAndPersistPrediction(active);
        } catch (DatabaseException ex) {
            prediction = predictionService.computeTransientPrediction(active);
        }

        boolean isUp = prediction.getTrend() == TrendDirection.UPWARD;
        boolean isDown = prediction.getTrend() == TrendDirection.DOWNWARD;
        Color trendColor = isUp ? palette.getGainGreen() : (isDown ? palette.getLossRed() : palette.getFlatAmber());

        Panel content = new Panel(new BorderLayout(0, 14));
        content.setBackground(palette.getPageBg());

        Panel kpiGrid = new Panel(new GridLayout(1, 4, 12, 12));
        kpiGrid.setBackground(palette.getPageBg());
        kpiGrid.add(createKpiCard(
                "Current Price",
                CurrencyConverter.format(prediction.getCurrentPrice(), currency),
                "Step P" + active.getPointCount(),
                palette.getTextPrimary()
        ));
        kpiGrid.add(createKpiCard(
                "Predicted Next",
                CurrencyConverter.format(prediction.getPredictedPrice(), currency),
                "Step P" + (active.getPointCount() + 1),
                trendColor
        ));
        kpiGrid.add(createKpiCard(
                "Price Difference",
                CurrencyConverter.formatSigned(prediction.getDifference(), currency),
                CurrencyConverter.formatSignedPercent(prediction.getPercentChange()),
                trendColor
        ));
        kpiGrid.add(createKpiCard(
                "Trend Direction",
                prediction.getTrend().getLabel(),
                String.format("Slope: %+.4f", prediction.getSlope()),
                trendColor
        ));
        content.add(kpiGrid, BorderLayout.NORTH);

        // Chart Card
        AwtCardPanel chartCard = new AwtCardPanel(new BorderLayout(0, 8), 14, 16);
        chartCard.applyTheme(palette);

        Label chartTitle = new Label("Price Trajectory & Forecast Projection (Violet = History, Dashed Amber = Forecast)");
        chartTitle.setFont(util.ThemePalette.FONT_CARD_TITLE);
        chartTitle.setForeground(palette.getTextPrimary());
        chartTitle.setBackground(palette.getCardBg());

        StockChart chart = new StockChart(StockChart.ChartMode.PREDICTION, 620, 290);
        chart.applyTheme(palette);
        chart.setCurrency(currency);
        chart.setPredictionData(active.getHistory(), prediction.getPredictedPrice());

        Label formulaNote = new Label(String.format(
                "Formula: PredictNext(d) = last_price (%.2f) + OLS_slope (%+.6f) = %.2f",
                prediction.getCurrentPrice(), prediction.getSlope(), prediction.getPredictedPrice()));
        formulaNote.setFont(util.ThemePalette.FONT_MONO_SMALL);
        formulaNote.setForeground(palette.getTextSecondary());
        formulaNote.setBackground(palette.getCardBg());

        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.add(formulaNote, BorderLayout.SOUTH);

        content.add(chartCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        validate();
        repaint();
    }
}
