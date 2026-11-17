package ui;

import database.DatabaseSeeder;
import model.Stock;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;
import util.ValidationUtil;

import java.awt.*;
import java.util.List;

/**
 * Module 5: Price History.
 * Displays historical stock prices in a chart and interactive AWT List,
 * allowing users to select a stock, append new prices, edit, or delete records in SQLite.
 */
public class HistoryPanel extends BaseModulePanel {

    private Choice stockChoice;
    private java.awt.List priceList;
    private TextField appendField;
    private TextField editField;
    private Label feedbackLbl;

    public HistoryPanel(StockService stockService, PredictionService predictionService,
                        IndicatorService indicatorService, HistoryService historyService,
                        StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "history";
    }

    @Override
    public String getModuleTitle() {
        return "Price History";
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
            add(buildModuleHeader("Price History", "Inspect and edit chronological closing prices.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Price History", () -> {
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
                "Price History — " + active.getName() + " [" + active.getSymbol() + "]",
                "Persisted chronological records in SQLite price_history table (" + active.getPointCount() + " points).",
                selectorWrap
        ), BorderLayout.NORTH);

        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(palette.getPageBg());

        // Historical Chart Card
        AwtCardPanel chartCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        chartCard.applyTheme(palette);
        Label chartLbl = new Label("Historical Price Curve (" + active.getPointCount() + " data points)");
        chartLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        chartLbl.setForeground(palette.getTextPrimary());
        chartLbl.setBackground(palette.getCardBg());

        StockChart chart = new StockChart(StockChart.ChartMode.HISTORY, 600, 220);
        chart.applyTheme(palette);
        chart.setCurrency(currency);
        chart.setHistoryData(active.getHistory());

        chartCard.add(chartLbl, BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        content.add(chartCard, BorderLayout.NORTH);

        // Editor & Price Records Card
        AwtCardPanel editorCard = new AwtCardPanel(new BorderLayout(0, 10), 12, 14);
        editorCard.applyTheme(palette);

        Panel controlsRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controlsRow.setBackground(palette.getCardBg());

        Label appendCap = new Label("Append P" + (active.getPointCount() + 1) + ":");
        appendCap.setFont(ThemePalette.FONT_BUTTON);
        appendCap.setForeground(palette.getTextPrimary());
        appendCap.setBackground(palette.getCardBg());

        this.appendField = createStyledTextField("", 10);
        Button appendBtn = createStyledButton("+ Append Price", palette.getAccentViolet(), Color.WHITE);
        appendBtn.addActionListener(e -> handleAppend(active));

        Label editCap = new Label("  Edit Selected:");
        editCap.setFont(ThemePalette.FONT_BUTTON);
        editCap.setForeground(palette.getTextPrimary());
        editCap.setBackground(palette.getCardBg());

        this.editField = createStyledTextField("", 10);
        Button saveEditBtn = createStyledButton("Update", palette.getHoverBg(), palette.getGainGreen());
        saveEditBtn.addActionListener(e -> handleUpdateSelected(active));

        Button delBtn = createStyledButton("Delete Selected", palette.getHoverBg(), palette.getLossRed());
        delBtn.addActionListener(e -> handleDeleteSelected(active));

        controlsRow.add(appendCap);
        controlsRow.add(appendField);
        controlsRow.add(appendBtn);
        controlsRow.add(editCap);
        controlsRow.add(editField);
        controlsRow.add(saveEditBtn);
        controlsRow.add(delBtn);

        this.priceList = new java.awt.List(8, false);
        priceList.setFont(ThemePalette.FONT_MONO);
        priceList.setBackground(palette.getInputBg());
        priceList.setForeground(palette.getTextPrimary());

        List<Double> history = active.getHistory();
        for (int i = 0; i < history.size(); i++) {
            double p = history.get(i);
            String deltaStr;
            if (i == 0) {
                deltaStr = "Baseline";
            } else {
                double prev = history.get(i - 1);
                double diff = p - prev;
                double pct = prev > 0 ? (diff / prev) * 100.0 : 0.0;
                deltaStr = CurrencyConverter.formatSigned(diff, currency) + " (" + CurrencyConverter.formatSignedPercent(pct) + ")";
            }
            priceList.add(String.format("P%-4d | %-14s | Change: %s", i + 1,
                    CurrencyConverter.format(p, currency), deltaStr));
        }

        priceList.addItemListener(e -> {
            int idx = priceList.getSelectedIndex();
            if (idx >= 0 && idx < history.size()) {
                editField.setText(String.valueOf(history.get(idx)));
            }
        });

        this.feedbackLbl = new Label("Select a row to edit/delete, or append a new chronological closing price.");
        feedbackLbl.setFont(ThemePalette.FONT_SUBTITLE);
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setBackground(palette.getCardBg());

        editorCard.add(controlsRow, BorderLayout.NORTH);
        editorCard.add(priceList, BorderLayout.CENTER);
        editorCard.add(feedbackLbl, BorderLayout.SOUTH);

        content.add(editorCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void handleAppend(Stock stock) {
        try {
            double val = ValidationUtil.parseSinglePositivePrice(appendField.getText());
            historyService.appendPrice(stock, val);
            stockService.syncDerivedAnalytics(stock);
            navigator.onPortfolioChanged(stock.getName());
        } catch (ValidationException | DatabaseException ex) {
            feedbackLbl.setForeground(palette.getLossRed());
            feedbackLbl.setText(ex.getMessage());
        }
    }

    private void handleUpdateSelected(Stock stock) {
        int idx = priceList.getSelectedIndex();
        if (idx < 0) {
            feedbackLbl.setForeground(palette.getFlatAmber());
            feedbackLbl.setText("Please select a price record from the list first.");
            return;
        }
        try {
            double val = ValidationUtil.parseSinglePositivePrice(editField.getText());
            historyService.updatePriceAt(stock, idx, val);
            stockService.syncDerivedAnalytics(stock);
            navigator.onPortfolioChanged(stock.getName());
        } catch (ValidationException | DatabaseException ex) {
            feedbackLbl.setForeground(palette.getLossRed());
            feedbackLbl.setText(ex.getMessage());
        }
    }

    private void handleDeleteSelected(Stock stock) {
        int idx = priceList.getSelectedIndex();
        if (idx < 0) {
            feedbackLbl.setForeground(palette.getFlatAmber());
            feedbackLbl.setText("Please select a price record to delete.");
            return;
        }
        try {
            historyService.deletePriceAt(stock, idx);
            stockService.syncDerivedAnalytics(stock);
            navigator.onPortfolioChanged(stock.getName());
        } catch (ValidationException | DatabaseException ex) {
            feedbackLbl.setForeground(palette.getLossRed());
            feedbackLbl.setText(ex.getMessage());
        }
    }
}
