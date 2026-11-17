package ui;

import database.DatabaseSeeder;
import model.Prediction;
import model.Stock;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 9: Compare Two Stocks.
 * Allows selecting Stock A (Violet) and Stock B (Cyan), compares prices, slopes,
 * percentage changes, and renders a normalized base-100 dual-trajectory Graphics2D chart.
 */
public class ComparePanel extends BaseModulePanel {

    private String stockAName = "";
    private String stockBName = "";
    private Choice choiceA;
    private Choice choiceB;

    public ComparePanel(StockService stockService, PredictionService predictionService,
                        IndicatorService indicatorService, HistoryService historyService,
                        StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "compare";
    }

    @Override
    public String getModuleTitle() {
        return "Compare Two Stocks";
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
            add(buildModuleHeader("Compare Two Stocks", "Side-by-side price and normalized trajectory comparison.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Compare Two Stocks", () -> {
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

        if (stocks.size() < 2) {
            add(buildModuleHeader("Compare Two Stocks", "Requires at least 2 stocks in SQLite.", null), BorderLayout.NORTH);
            AwtCardPanel card = new AwtCardPanel(new GridLayout(3, 1, 0, 8), 24, 24);
            card.applyTheme(palette);
            Label msg = new Label("Comparison requires at least 2 stocks. Currently only \"" + stocks.get(0).getName() + "\" exists.", Label.CENTER);
            msg.setFont(ThemePalette.FONT_BODY);
            msg.setForeground(palette.getTextSecondary());
            msg.setBackground(palette.getCardBg());

            Panel btnRow = new Panel(new FlowLayout(FlowLayout.CENTER));
            btnRow.setBackground(palette.getCardBg());
            Button addBtn = createStyledButton("Add Second Stock", palette.getAccentViolet(), Color.WHITE);
            addBtn.addActionListener(e -> navigator.onNavigate("add", null));
            btnRow.add(addBtn);

            card.add(new Label("Add Another Stock to Compare", Label.CENTER));
            card.add(msg);
            card.add(btnRow);
            add(card, BorderLayout.CENTER);
            validate();
            repaint();
            return;
        }

        Stock stockA = findOrDefault(stocks, stockAName, stocks.get(0));
        Stock stockB = findOrDefault(stocks, stockBName, stocks.get(1));
        stockAName = stockA.getName();
        stockBName = stockB.getName();

        this.choiceA = createStyledChoice();
        this.choiceB = createStyledChoice();
        for (Stock s : stocks) {
            choiceA.add(s.getName());
            choiceB.add(s.getName());
        }
        choiceA.select(stockA.getName());
        choiceB.select(stockB.getName());

        choiceA.addItemListener(e -> {
            stockAName = choiceA.getSelectedItem();
            refreshData();
        });
        choiceB.addItemListener(e -> {
            stockBName = choiceB.getSelectedItem();
            refreshData();
        });

        Panel selectors = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        selectors.setBackground(palette.getPageBg());
        Label lblA = new Label("Stock A (Violet):");
        lblA.setFont(ThemePalette.FONT_LABEL);
        lblA.setForeground(palette.getAccentViolet());
        lblA.setBackground(palette.getPageBg());
        Label lblB = new Label("Stock B (Cyan):");
        lblB.setFont(ThemePalette.FONT_LABEL);
        lblB.setForeground(palette.getCyanSecondary());
        lblB.setBackground(palette.getPageBg());
        selectors.add(lblA);
        selectors.add(choiceA);
        selectors.add(lblB);
        selectors.add(choiceB);

        add(buildModuleHeader(
                "Compare Two Stocks",
                "Comparing " + stockA.getName() + " vs " + stockB.getName() + " with normalized base-100 trajectories.",
                selectors
        ), BorderLayout.NORTH);

        Prediction predA = predictionService.computeTransientPrediction(stockA);
        Prediction predB = predictionService.computeTransientPrediction(stockB);

        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(palette.getPageBg());

        // Winner Banner
        AwtCardPanel banner = new AwtCardPanel(new BorderLayout(), 10, 14);
        banner.applyTheme(palette);
        banner.setBorderColor(palette.getAccentViolet());
        String bannerText;
        if (stockA.getName().equalsIgnoreCase(stockB.getName())) {
            bannerText = "Same stock selected for both slots. Pick two different stocks to compare relative strength.";
        } else if (Math.abs(predA.getSlope() - predB.getSlope()) < 1e-6) {
            bannerText = String.format("Both stocks exhibit identical linear slope gradients (%+.4f).", predA.getSlope());
        } else {
            Stock stronger = predA.getSlope() > predB.getSlope() ? stockA : stockB;
            double strongSlope = Math.max(predA.getSlope(), predB.getSlope());
            double weakSlope = Math.min(predA.getSlope(), predB.getSlope());
            bannerText = String.format("%s exhibits the stronger upward slope (%+.4f vs %+.4f).",
                    stronger.getName(), strongSlope, weakSlope);
        }
        Label bannerLbl = new Label(bannerText);
        bannerLbl.setFont(ThemePalette.FONT_BUTTON);
        bannerLbl.setForeground(palette.getAccentViolet());
        bannerLbl.setBackground(palette.getCardBg());
        banner.add(bannerLbl, BorderLayout.CENTER);
        content.add(banner, BorderLayout.NORTH);

        // Side-by-Side Comparison Cards
        Panel cardsRow = new Panel(new GridLayout(1, 2, 12, 12));
        cardsRow.setBackground(palette.getPageBg());
        cardsRow.add(buildCompareStockCard(stockA, predA, palette.getAccentViolet()));
        cardsRow.add(buildCompareStockCard(stockB, predB, palette.getCyanSecondary()));

        // Normalized Chart Card
        AwtCardPanel chartCard = new AwtCardPanel(new BorderLayout(0, 8), 14, 16);
        chartCard.applyTheme(palette);
        Label chartTitle = new Label("Normalized Comparative Performance (Base = 100 at P1)");
        chartTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        chartTitle.setForeground(palette.getTextPrimary());
        chartTitle.setBackground(palette.getCardBg());

        StockChart chart = new StockChart(StockChart.ChartMode.COMPARE_NORMALIZED, 620, 260);
        chart.applyTheme(palette);
        chart.setComparisonData(
                normalizeBase100(stockA.getHistory()),
                normalizeBase100(stockB.getHistory()),
                stockA.getName(),
                stockB.getName()
        );
        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);

        Panel lower = new Panel(new BorderLayout(0, 12));
        lower.setBackground(palette.getPageBg());
        lower.add(cardsRow, BorderLayout.NORTH);
        lower.add(chartCard, BorderLayout.CENTER);

        content.add(lower, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private AwtCardPanel buildCompareStockCard(Stock stock, Prediction pred, Color accent) {
        AwtCardPanel card = new AwtCardPanel(new BorderLayout(0, 8), 12, 14);
        card.applyTheme(palette);
        card.setBorderColor(accent);

        Label title = new Label(stock.getName() + " [" + stock.getSymbol() + "] · " + stock.getPointCount() + " pts");
        title.setFont(ThemePalette.FONT_CARD_TITLE);
        title.setForeground(accent);
        title.setBackground(palette.getCardBg());
        card.add(title, BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(2, 2, 8, 8));
        grid.setBackground(palette.getCardBg());
        grid.add(makeMiniStat("Latest Price", CurrencyConverter.format(pred.getCurrentPrice(), currency), palette.getTextPrimary()));
        grid.add(makeMiniStat("Trend Slope", String.format("%+.4f", pred.getSlope()), accent));
        grid.add(makeMiniStat("Predicted Next", CurrencyConverter.format(pred.getPredictedPrice(), currency), accent));
        grid.add(makeMiniStat("Forecast % Change", CurrencyConverter.formatSignedPercent(pred.getPercentChange()),
                pred.getPercentChange() >= 0 ? palette.getGainGreen() : palette.getLossRed()));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private Panel makeMiniStat(String cap, String val, Color valColor) {
        Panel p = new Panel(new GridLayout(2, 1, 0, 2));
        p.setBackground(palette.getInputBg());
        Label c = new Label(" " + cap.toUpperCase());
        c.setFont(ThemePalette.FONT_LABEL);
        c.setForeground(palette.getTextSecondary());
        c.setBackground(palette.getInputBg());

        Label v = new Label(" " + val);
        v.setFont(ThemePalette.FONT_MONO);
        v.setForeground(valColor);
        v.setBackground(palette.getInputBg());
        p.add(c);
        p.add(v);
        return p;
    }

    private List<Double> normalizeBase100(List<Double> prices) {
        List<Double> norm = new ArrayList<>();
        if (prices.isEmpty() || prices.get(0) <= 0) {
            return norm;
        }
        double base = prices.get(0);
        for (double p : prices) {
            norm.add((p / base) * 100.0);
        }
        return norm;
    }

    private Stock findOrDefault(List<Stock> stocks, String targetName, Stock fallback) {
        for (Stock s : stocks) {
            if (s.getName().equalsIgnoreCase(targetName)) {
                return s;
            }
        }
        return fallback;
    }
}
