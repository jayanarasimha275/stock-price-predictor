package ui;

import database.DatabaseSeeder;
import model.Prediction;
import model.Stock;
import model.TechnicalIndicator;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 7: Forecast Report.
 * Generates a consolidated portfolio table with Search filter, CSV file export (File Handling),
 * and native desktop printing via java.awt.print.Printable.
 */
public class ForecastPanel extends BaseModulePanel implements Printable {

    private TextField searchField;
    private java.awt.List reportList;
    private Label statusLbl;
    private List<String> printableLines = new ArrayList<>();

    public ForecastPanel(StockService stockService, PredictionService predictionService,
                         IndicatorService indicatorService, HistoryService historyService,
                         StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "forecast";
    }

    @Override
    public String getModuleTitle() {
        return "Forecast Report";
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
            add(buildModuleHeader("Portfolio Forecast Report", "Consolidated indicators, slope, and predictions.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Forecast Report", () -> {
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

        Panel actions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(palette.getPageBg());

        Button exportCsvBtn = createStyledButton("Export CSV", palette.getAccentViolet(), Color.WHITE);
        exportCsvBtn.addActionListener(e -> handleExportCsv(stocks));

        Button printBtn = createStyledButton("Print Report", palette.getHoverBg(), palette.getTextPrimary());
        printBtn.addActionListener(e -> handlePrintReport());

        actions.add(exportCsvBtn);
        actions.add(printBtn);

        add(buildModuleHeader(
                "Portfolio Forecast Report",
                "Consolidated latest prices, SMA(5), EMA(5), Momentum(3), RSI(14), OLS Slope, and Predicted Next Price.",
                actions
        ), BorderLayout.NORTH);

        Panel body = new Panel(new BorderLayout(0, 12));
        body.setBackground(palette.getPageBg());

        // Search Bar Card
        AwtCardPanel filterCard = new AwtCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 4), 10, 14);
        filterCard.applyTheme(palette);
        Label searchLbl = new Label("Filter Stock:");
        searchLbl.setFont(ThemePalette.FONT_BUTTON);
        searchLbl.setForeground(palette.getTextPrimary());
        searchLbl.setBackground(palette.getCardBg());

        this.searchField = createStyledTextField("", 26);
        searchField.addTextListener(e -> populateReportRows(stocks, searchField.getText()));

        filterCard.add(searchLbl);
        filterCard.add(searchField);
        body.add(filterCard, BorderLayout.NORTH);

        // Tabular Report Card
        AwtCardPanel tableCard = new AwtCardPanel(new BorderLayout(0, 8), 12, 14);
        tableCard.applyTheme(palette);

        String headerRow = String.format("%-14s | %-6s | %-12s | %-11s | %-11s | %-9s | %-8s | %-10s | %-12s | %-8s",
                "STOCK", "PTS", "LATEST", "SMA(5)", "EMA(5)", "MOM(3)", "RSI(14)", "SLOPE", "PREDICTED", "TREND");
        Label headerLbl = new Label(headerRow);
        headerLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        headerLbl.setForeground(palette.getAccentViolet());
        headerLbl.setBackground(palette.getChromeBg());

        this.reportList = new java.awt.List(14, false);
        reportList.setFont(ThemePalette.FONT_MONO_SMALL);
        reportList.setBackground(palette.getInputBg());
        reportList.setForeground(palette.getTextPrimary());

        this.statusLbl = new Label("Double-click any row to open Predict Next Price for that stock.");
        statusLbl.setFont(ThemePalette.FONT_SUBTITLE);
        statusLbl.setForeground(palette.getTextSecondary());
        statusLbl.setBackground(palette.getCardBg());

        tableCard.add(headerLbl, BorderLayout.NORTH);
        tableCard.add(reportList, BorderLayout.CENTER);
        tableCard.add(statusLbl, BorderLayout.SOUTH);

        populateReportRows(stocks, "");

        reportList.addActionListener(e -> {
            int idx = reportList.getSelectedIndex();
            String query = searchField.getText().trim().toLowerCase();
            List<Stock> filtered = stocks.stream()
                    .filter(s -> query.isEmpty() || s.getName().toLowerCase().contains(query))
                    .toList();
            if (idx >= 0 && idx < filtered.size()) {
                navigator.onNavigate("predict", filtered.get(idx).getName());
            }
        });

        body.add(tableCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void populateReportRows(List<Stock> stocks, String filter) {
        reportList.removeAll();
        printableLines.clear();
        printableLines.add("Stock Price Predictor — Forecast Report (CSE 213 SRM University AP)");
        printableLines.add(String.format("%-14s | %-6s | %-12s | %-11s | %-11s | %-9s | %-8s | %-10s | %-12s | %-8s",
                "STOCK", "PTS", "LATEST", "SMA(5)", "EMA(5)", "MOM(3)", "RSI(14)", "SLOPE", "PREDICTED", "TREND"));
        printableLines.add("-".repeat(120));

        String query = filter != null ? filter.trim().toLowerCase() : "";
        for (Stock s : stocks) {
            if (!query.isEmpty() && !s.getName().toLowerCase().contains(query)
                    && !s.getSymbol().toLowerCase().contains(query)) {
                continue;
            }
            Prediction pred = predictionService.computeTransientPrediction(s);
            TechnicalIndicator ind = indicatorService.computeTransient(s, 5, 5, 3, 14, 12, 26, 9);

            String row = String.format("%-14s | %-6d | %-12s | %-11s | %-11s | %-9s | %-8s | %+-10.4f | %-12s | %-8s",
                    truncate(s.getName(), 14),
                    s.getPointCount(),
                    CurrencyConverter.format(pred.getCurrentPrice(), currency),
                    ind.isSmaValid() ? CurrencyConverter.format(ind.getSmaValue(), currency) : "N/A",
                    ind.isEmaValid() ? CurrencyConverter.format(ind.getEmaValue(), currency) : "N/A",
                    ind.isMomentumValid() ? String.format("%+.2f", ind.getMomentumValue()) : "N/A",
                    ind.isRsiValid() ? String.format("%.1f", ind.getRsiValue()) : "N/A",
                    pred.getSlope(),
                    CurrencyConverter.format(pred.getPredictedPrice(), currency),
                    pred.getTrend().getLabel()
            );
            reportList.add(row);
            printableLines.add(row);
        }
    }

    private void handleExportCsv(List<Stock> stocks) {
        Frame owner = findParentFrame();
        FileDialog fd = new FileDialog(owner, "Save Forecast Report CSV", FileDialog.SAVE);
        fd.setFile("stock_forecast_report.csv");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) {
            return;
        }
        File outFile = new File(dir, file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write("Stock Name,Symbol,Data Points,Latest Price (" + currency.getCode()
                    + "),SMA(5),EMA(5),Momentum(3),RSI(14),Slope,Predicted Next (" + currency.getCode() + "),Trend");
            writer.newLine();

            for (Stock s : stocks) {
                Prediction pred = predictionService.computeTransientPrediction(s);
                TechnicalIndicator ind = indicatorService.computeTransient(s, 5, 5, 3, 14, 12, 26, 9);
                writer.write(String.format("\"%s\",\"%s\",%d,%.2f,%s,%s,%s,%s,%.6f,%.2f,%s",
                        s.getName().replace("\"", "\"\""),
                        s.getSymbol().replace("\"", "\"\""),
                        s.getPointCount(),
                        pred.getCurrentPrice(),
                        ind.isSmaValid() ? String.format("%.2f", ind.getSmaValue()) : "N/A",
                        ind.isEmaValid() ? String.format("%.2f", ind.getEmaValue()) : "N/A",
                        ind.isMomentumValid() ? String.format("%.2f", ind.getMomentumValue()) : "N/A",
                        ind.isRsiValid() ? String.format("%.2f", ind.getRsiValue()) : "N/A",
                        pred.getSlope(),
                        pred.getPredictedPrice(),
                        pred.getTrend().getLabel()
                ));
                writer.newLine();
            }
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Exported CSV report to: " + outFile.getAbsolutePath());
        } catch (IOException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText("Failed to export CSV: " + ex.getMessage());
        }
    }

    private void handlePrintReport() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Stock Price Predictor Forecast Report");
        job.setPrintable(this);
        if (job.printDialog()) {
            try {
                job.print();
                statusLbl.setForeground(palette.getGainGreen());
                statusLbl.setText("Report sent to printer.");
            } catch (PrinterException ex) {
                statusLbl.setForeground(palette.getLossRed());
                statusLbl.setText("Print error: " + ex.getMessage());
            }
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        g2.setColor(Color.BLACK);

        int y = 20;
        for (String line : printableLines) {
            g2.drawString(line, 12, y);
            y += 14;
        }
        return PAGE_EXISTS;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
