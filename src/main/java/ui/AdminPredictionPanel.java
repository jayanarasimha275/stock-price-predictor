package ui;

import model.Currency;
import model.Prediction;
import model.Stock;
import model.TrendDirection;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Admin Module 4: Prediction Management.
 * Displays persisted SQLite prediction records, per-stock prediction history,
 * and interactive stock prediction details with OLS regression trajectory charts.
 */
public class AdminPredictionPanel extends Panel implements ThemeAware {

    private final StockService stockService;
    private final PredictionService predictionService;
    private final Runnable onPredictionsChanged;

    private ThemePalette palette;
    private Currency currency = Currency.INR;

    private List<Stock> allStocks = new ArrayList<>();
    private List<Prediction> displayedPredictions = new ArrayList<>();

    private Choice stockFilterChoice;
    private java.awt.List predictionTable;
    private Label detailSummaryLbl;
    private Label detailStatsLbl;
    private Label detailTimeLbl;
    private StockChart detailChart;
    private Label statusLbl;

    public AdminPredictionPanel(StockService stockService, PredictionService predictionService,
                                ThemePalette palette, Currency currency, Runnable onPredictionsChanged) {
        this.stockService = stockService;
        this.predictionService = predictionService;
        this.palette = palette != null ? palette : ThemePalette.DARK;
        this.currency = currency != null ? currency : Currency.INR;
        this.onPredictionsChanged = onPredictionsChanged;
        setLayout(new BorderLayout(0, 14));
        setBackground(this.palette.getPageBg());
    }

    @Override
    public Insets getInsets() {
        return new Insets(18, 22, 22, 22);
    }

    public void setCurrency(Currency currency) {
        this.currency = currency != null ? currency : Currency.INR;
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        refreshData();
    }

    public void refreshData() {
        removeAll();
        setBackground(palette.getPageBg());

        Panel header = new Panel(new BorderLayout(12, 4));
        header.setBackground(palette.getPageBg());
        Panel titleStack = new Panel(new GridLayout(2, 1, 0, 2));
        titleStack.setBackground(palette.getPageBg());

        Label title = new Label("Prediction Management");
        title.setFont(ThemePalette.FONT_TITLE);
        title.setForeground(palette.getTextPrimary());
        title.setBackground(palette.getPageBg());

        Label sub = new Label("Audit all SQLite prediction records, filter prediction history by stock, and inspect forecast trajectories.");
        sub.setFont(ThemePalette.FONT_SUBTITLE);
        sub.setForeground(palette.getTextSecondary());
        sub.setBackground(palette.getPageBg());
        titleStack.add(title);
        titleStack.add(sub);
        header.add(titleStack, BorderLayout.CENTER);

        Button recalcBtn = styledBtn("Sync / Recalculate All", palette.getAccentViolet(), Color.WHITE);
        recalcBtn.addActionListener(e -> handleRecalculateAll());
        header.add(recalcBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Panel body = new Panel(new BorderLayout(0, 12));
        body.setBackground(palette.getPageBg());

        // Filter card
        AwtCardPanel filterCard = new AwtCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 4), 10, 14);
        filterCard.applyTheme(palette);

        Label filterLbl = new Label("View Prediction History For:");
        filterLbl.setFont(ThemePalette.FONT_BUTTON);
        filterLbl.setForeground(palette.getTextPrimary());
        filterLbl.setBackground(palette.getCardBg());

        this.stockFilterChoice = new Choice();
        stockFilterChoice.setFont(ThemePalette.FONT_MONO);
        stockFilterChoice.setBackground(palette.getInputBg());
        stockFilterChoice.setForeground(palette.getTextPrimary());
        stockFilterChoice.add("All Stocks (All Prediction Records)");

        try {
            this.allStocks = stockService.getAllStocks();
            for (Stock s : allStocks) {
                stockFilterChoice.add(s.getName() + " [" + s.getSymbol() + "]");
            }
        } catch (DatabaseException ex) {
            allStocks = new ArrayList<>();
        }
        stockFilterChoice.addItemListener(e -> loadPredictions());

        filterCard.add(filterLbl);
        filterCard.add(stockFilterChoice);
        body.add(filterCard, BorderLayout.NORTH);

        // Center: Prediction Records Table
        AwtCardPanel tableCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        tableCard.applyTheme(palette);

        String hdr = String.format("%-5s | %-15s | %-13s | %-13s | %-10s | %-10s | %-8s | %-8s | %-16s",
                "REC#", "STOCK", "CURRENT", "PREDICTED", "DIFF", "SLOPE", "TREND", "ACC%", "RECORDED AT");
        Label hdrLbl = new Label(hdr);
        hdrLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        hdrLbl.setForeground(palette.getAccentViolet());
        hdrLbl.setBackground(palette.getChromeBg());

        this.predictionTable = new java.awt.List(9, false);
        predictionTable.setFont(ThemePalette.FONT_MONO_SMALL);
        predictionTable.setBackground(palette.getInputBg());
        predictionTable.setForeground(palette.getTextPrimary());
        predictionTable.addItemListener(e -> updateSelectedPredictionDetails());

        this.statusLbl = new Label("Select any prediction record to view stock prediction details.");
        statusLbl.setFont(ThemePalette.FONT_SUBTITLE);
        statusLbl.setForeground(palette.getTextSecondary());
        statusLbl.setBackground(palette.getCardBg());

        tableCard.add(hdrLbl, BorderLayout.NORTH);
        tableCard.add(predictionTable, BorderLayout.CENTER);
        tableCard.add(statusLbl, BorderLayout.SOUTH);

        // Bottom: Selected Stock Prediction Details + Trajectory Chart
        AwtCardPanel detailCard = new AwtCardPanel(new BorderLayout(0, 10), 14, 16);
        detailCard.applyTheme(palette);

        Panel detailInfo = new Panel(new GridLayout(3, 1, 0, 4));
        detailInfo.setBackground(palette.getCardBg());

        this.detailSummaryLbl = new Label("Stock Prediction Details");
        detailSummaryLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        detailSummaryLbl.setForeground(palette.getTextPrimary());
        detailSummaryLbl.setBackground(palette.getCardBg());

        this.detailStatsLbl = new Label("Select a prediction row above to view current, predicted, slope, trend, and accuracy.");
        detailStatsLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        detailStatsLbl.setForeground(palette.getTextSecondary());
        detailStatsLbl.setBackground(palette.getCardBg());

        this.detailTimeLbl = new Label(" ");
        detailTimeLbl.setFont(ThemePalette.FONT_SUBTITLE);
        detailTimeLbl.setForeground(palette.getTextMuted());
        detailTimeLbl.setBackground(palette.getCardBg());

        detailInfo.add(detailSummaryLbl);
        detailInfo.add(detailStatsLbl);
        detailInfo.add(detailTimeLbl);

        this.detailChart = new StockChart(StockChart.ChartMode.PREDICTION, 600, 195);
        detailChart.applyTheme(palette);
        detailChart.setCurrency(currency);

        detailCard.add(detailInfo, BorderLayout.NORTH);
        detailCard.add(detailChart, BorderLayout.CENTER);

        Panel centerSplit = new Panel(new BorderLayout(0, 12));
        centerSplit.setBackground(palette.getPageBg());
        centerSplit.add(tableCard, BorderLayout.NORTH);
        centerSplit.add(detailCard, BorderLayout.CENTER);
        body.add(centerSplit, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        loadPredictions();
        validate();
        repaint();
    }

    private void loadPredictions() {
        try {
            int selIdx = stockFilterChoice.getSelectedIndex();
            if (selIdx <= 0 || selIdx - 1 >= allStocks.size()) {
                this.displayedPredictions = predictionService.getAllPredictions();
            } else {
                Stock chosen = allStocks.get(selIdx - 1);
                this.displayedPredictions = predictionService.getPredictionsByStockId(chosen.getId(), chosen.getName());
            }

            predictionTable.removeAll();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Prediction p : displayedPredictions) {
                String row = String.format("%-5d | %-15s | %-13s | %-13s | %-10s | %+-10.4f | %-8s | %6.1f%%  | %-16s",
                        p.getId(),
                        truncate(p.getStockName(), 15),
                        CurrencyConverter.format(p.getCurrentPrice(), currency),
                        CurrencyConverter.format(p.getPredictedPrice(), currency),
                        CurrencyConverter.formatSigned(p.getDifference(), currency),
                        p.getSlope(),
                        p.getTrend().getLabel(),
                        p.getAccuracyPercent(),
                        fmt.format(new Date(p.getCreatedAt())));
                predictionTable.add(row);
            }
            statusLbl.setText("Showing " + displayedPredictions.size() + " persisted prediction record(s) in SQLite.");
            if (!displayedPredictions.isEmpty()) {
                predictionTable.select(0);
                updateSelectedPredictionDetails();
            }
        } catch (DatabaseException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText("Error loading predictions: " + ex.getMessage());
        }
    }

    private void updateSelectedPredictionDetails() {
        int idx = predictionTable.getSelectedIndex();
        if (idx < 0 || idx >= displayedPredictions.size()) {
            return;
        }
        Prediction p = displayedPredictions.get(idx);
        Color trendColor = p.getTrend() == TrendDirection.UPWARD
                ? palette.getGainGreen()
                : (p.getTrend() == TrendDirection.DOWNWARD ? palette.getLossRed() : palette.getFlatAmber());

        detailSummaryLbl.setForeground(trendColor);
        detailSummaryLbl.setText("Record #" + p.getId() + " — " + p.getStockName() + " (" + p.getTrend().getLabel() + ")");
        detailStatsLbl.setForeground(palette.getTextPrimary());
        detailStatsLbl.setText(String.format(
                "Current: %s  |  Predicted: %s (%s / %s)  |  OLS Slope: %+.4f  |  Accuracy: %.2f%%",
                CurrencyConverter.format(p.getCurrentPrice(), currency),
                CurrencyConverter.format(p.getPredictedPrice(), currency),
                CurrencyConverter.formatSigned(p.getDifference(), currency),
                CurrencyConverter.formatSignedPercent(p.getPercentChange()),
                p.getSlope(),
                p.getAccuracyPercent()
        ));

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        detailTimeLbl.setText("Recorded in SQLite predictions table at: " + fmt.format(new Date(p.getCreatedAt())));

        Optional<Stock> match = allStocks.stream().filter(s -> s.getId() == p.getStockId()).findFirst();
        if (match.isPresent()) {
            detailChart.setCurrency(currency);
            detailChart.setPredictionData(match.get().getHistory(), p.getPredictedPrice());
        } else {
            detailChart.setPredictionData(new ArrayList<>(), p.getPredictedPrice());
        }
    }

    private void handleRecalculateAll() {
        try {
            for (Stock s : stockService.getAllStocks()) {
                predictionService.computeAndPersistPrediction(s);
            }
            loadPredictions();
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Synchronized predictions for all portfolio stocks.");
            if (onPredictionsChanged != null) onPredictionsChanged.run();
        } catch (DatabaseException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText("Error: " + ex.getMessage());
        }
    }

    private Button styledBtn(String label, Color bg, Color fg) {
        Button b = new Button(label);
        b.setFont(ThemePalette.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
