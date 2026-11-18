package ui;

import database.DatabaseSeeder;
import model.AccuracyReport;
import model.Stock;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.ChartRenderer;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.util.List;

/**
 * Module 6: Prediction Accuracy Backtesting.
 * Trains on all prices except the last, predicts the held-out last price,
 * computes absolute error and accuracy percentage, and visualizes train vs test points.
 */
public class AccuracyPanel extends BaseModulePanel {

    private Choice stockChoice;

    public AccuracyPanel(StockService stockService, PredictionService predictionService,
                         IndicatorService indicatorService, HistoryService historyService,
                         StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "accuracy";
    }

    @Override
    public String getModuleTitle() {
        return "Prediction Accuracy";
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
            add(buildModuleHeader("Prediction Accuracy", "Backtest OLS model against actual closing prices.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Prediction Accuracy", () -> {
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
        selLbl.setFont(ThemePalette.FONT_BUTTON);
        selLbl.setForeground(palette.getTextSecondary());
        selLbl.setBackground(palette.getPageBg());
        selectorWrap.add(selLbl);
        selectorWrap.add(stockChoice);

        add(buildModuleHeader(
                "Prediction Accuracy — " + active.getName(),
                "Walk-forward backtest holding out the final recorded price as ground truth.",
                selectorWrap
        ), BorderLayout.NORTH);

        AccuracyReport report = predictionService.calculateAccuracy(active.getHistory());

        if (!report.isCanCalculate()) {
            AwtCardPanel warnCard = new AwtCardPanel(new GridLayout(3, 1, 0, 8), 24, 24);
            warnCard.applyTheme(palette);
            Label wTitle = new Label("Insufficient Data for Backtesting", Label.CENTER);
            wTitle.setFont(ThemePalette.FONT_CARD_TITLE);
            wTitle.setForeground(palette.getFlatAmber());
            wTitle.setBackground(palette.getCardBg());

            Label wDesc = new Label(report.getReason() + " Add more prices in Price History.", Label.CENTER);
            wDesc.setFont(ThemePalette.FONT_BODY);
            wDesc.setForeground(palette.getTextSecondary());
            wDesc.setBackground(palette.getCardBg());

            Panel btnRow = new Panel(new FlowLayout(FlowLayout.CENTER));
            btnRow.setBackground(palette.getCardBg());
            Button goHistBtn = createStyledButton("Go to Price History", palette.getAccentViolet(), Color.WHITE);
            goHistBtn.addActionListener(e -> navigator.onNavigate("history", active.getName()));
            btnRow.add(goHistBtn);

            warnCard.add(wTitle);
            warnCard.add(wDesc);
            warnCard.add(btnRow);
            add(warnCard, BorderLayout.CENTER);
            validate();
            repaint();
            return;
        }

        double accScore = report.getAccuracyPercent();
        Color accColor = accScore >= 90 ? palette.getGainGreen()
                : (accScore >= 75 ? palette.getAccentViolet()
                : (accScore >= 50 ? palette.getFlatAmber() : palette.getLossRed()));

        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(palette.getPageBg());

        Panel kpiGrid = new Panel(new GridLayout(1, 4, 12, 12));
        kpiGrid.setBackground(palette.getPageBg());
        kpiGrid.add(createKpiCard(
                "Backtest Predicted",
                CurrencyConverter.format(report.getPredicted(), currency),
                "Trained on " + report.getTrainPoints() + " points",
                palette.getFlatAmber()
        ));
        kpiGrid.add(createKpiCard(
                "Actual Ground Truth",
                CurrencyConverter.format(report.getActual(), currency),
                "True step P" + active.getPointCount(),
                palette.getCyanSecondary()
        ));
        double variancePct = report.getActual() > 0 ? (report.getError() / report.getActual()) * 100.0 : 0.0;
        kpiGrid.add(createKpiCard(
                "Absolute Error",
                CurrencyConverter.format(report.getError(), currency),
                String.format("%.2f%% variance", variancePct),
                palette.getTextPrimary()
        ));

        AwtCardPanel scoreCard = new AwtCardPanel(new BorderLayout(0, 4), 12, 14);
        scoreCard.applyTheme(palette);
        Label sCap = new Label("ACCURACY SCORE");
        sCap.setFont(ThemePalette.FONT_LABEL);
        sCap.setForeground(palette.getTextSecondary());
        sCap.setBackground(palette.getCardBg());

        Label sVal = new Label(String.format("%.2f%%", accScore));
        sVal.setFont(ThemePalette.FONT_KPI);
        sVal.setForeground(accColor);
        sVal.setBackground(palette.getCardBg());

        AccuracyBarCanvas barCanvas = new AccuracyBarCanvas(accScore, accColor, palette);
        scoreCard.add(sCap, BorderLayout.NORTH);
        scoreCard.add(sVal, BorderLayout.CENTER);
        scoreCard.add(barCanvas, BorderLayout.SOUTH);
        kpiGrid.add(scoreCard);

        content.add(kpiGrid, BorderLayout.NORTH);

        // Visual Backtest Chart
        AwtCardPanel chartCard = new AwtCardPanel(new BorderLayout(0, 8), 14, 16);
        chartCard.applyTheme(palette);

        Label chartTitle = new Label("Train vs Test Split (Violet = Training, Cyan = Actual, Amber = Predicted)");
        chartTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        chartTitle.setForeground(palette.getTextPrimary());
        chartTitle.setBackground(palette.getCardBg());

        List<Double> trainData = active.getHistory().subList(0, active.getPointCount() - 1);
        StockChart chart = new StockChart(StockChart.ChartMode.ACCURACY_BACKTEST, 620, 260);
        chart.applyTheme(palette);
        chart.setCurrency(currency);
        chart.setAccuracyData(trainData, report.getActual(), report.getPredicted());

        String mathBreakdown = String.format(
                "Train P1..P%d Slope m=%+.6f | Pred=%.2f | Actual=%.2f | Error=|pred-act|=%.2f | Accuracy=%.2f%%",
                report.getTrainPoints(), report.getTrainSlope(),
                report.getPredicted(), report.getActual(), report.getError(), accScore
        );
        Label mathLbl = new Label(mathBreakdown);
        mathLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        mathLbl.setForeground(palette.getTextSecondary());
        mathLbl.setBackground(palette.getCardBg());

        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.add(mathLbl, BorderLayout.SOUTH);
        content.add(chartCard, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        validate();
        repaint();
    }

    /**
     * Custom Graphics2D progress bar Canvas for the accuracy percentage gauge.
     */
    private static class AccuracyBarCanvas extends Canvas {
        private final double percent;
        private final Color fillColor;
        private final ThemePalette palette;

        AccuracyBarCanvas(double percent, Color fillColor, ThemePalette palette) {
            this.percent = Math.max(0.0, Math.min(100.0, percent));
            this.fillColor = fillColor;
            this.palette = palette;
            setPreferredSize(new Dimension(140, 12));
            setSize(140, 12);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ChartRenderer.enableQualityHints(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(palette.getCardBg());
            g2.fillRect(0, 0, w, h);

            g2.setColor(palette.getInputBg());
            g2.fillRoundRect(0, 2, w, 8, 6, 6);

            int fillW = (int) Math.round((percent / 100.0) * w);
            if (fillW > 0) {
                g2.setColor(fillColor);
                g2.fillRoundRect(0, 2, fillW, 8, 6, 6);
            }
            g2.dispose();
        }
    }
}
