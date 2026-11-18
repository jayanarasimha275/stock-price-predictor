package ui;

import model.Currency;
import model.Prediction;
import model.Stock;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;
import util.ValidationUtil;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Module 3: Stock Management.
 * Allows Administrators to view all SQLite stocks, search, add stocks,
 * edit stock details and historical price series, delete stocks, and inspect stock history charts.
 */
public class AdminStockPanel extends Panel implements ThemeAware {

    private final StockService stockService;
    private final PredictionService predictionService;
    private final Runnable onStocksChanged;

    private ThemePalette palette;
    private Currency currency = Currency.INR;

    private List<Stock> displayedStocks = new ArrayList<>();
    private Stock selectedStock = null;
    private boolean addMode = false;

    private TextField searchField;
    private java.awt.List stockTable;
    private TextField nameField;
    private TextField symbolField;
    private TextField notesField;
    private TextArea pricesArea;
    private Label editorTitleLbl;
    private Label feedbackLbl;
    private Button saveBtn;
    private Button deleteBtn;
    private StockChart historyChart;
    private java.awt.List historyPointsList;

    public AdminStockPanel(StockService stockService, PredictionService predictionService,
                           ThemePalette palette, Currency currency, Runnable onStocksChanged) {
        this.stockService = stockService;
        this.predictionService = predictionService;
        this.palette = palette != null ? palette : ThemePalette.DARK;
        this.currency = currency != null ? currency : Currency.INR;
        this.onStocksChanged = onStocksChanged;
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
        Label title = new Label("Stock Management");
        title.setFont(ThemePalette.FONT_TITLE);
        title.setForeground(palette.getTextPrimary());
        title.setBackground(palette.getPageBg());

        Label sub = new Label("View all portfolio stocks, add/edit stock metadata & closing prices, delete records, and inspect history.");
        sub.setFont(ThemePalette.FONT_SUBTITLE);
        sub.setForeground(palette.getTextSecondary());
        sub.setBackground(palette.getPageBg());
        titleStack.add(title);
        titleStack.add(sub);
        header.add(titleStack, BorderLayout.CENTER);

        Panel headerBtns = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        headerBtns.setBackground(palette.getPageBg());
        Button addBtn = styledBtn("+ Add New Stock", palette.getAccentViolet(), Color.WHITE);
        addBtn.addActionListener(e -> switchToAddMode());
        Button refreshBtn = styledBtn("Refresh", palette.getHoverBg(), palette.getTextPrimary());
        refreshBtn.addActionListener(e -> loadStocks(searchField != null ? searchField.getText() : ""));
        headerBtns.add(addBtn);
        headerBtns.add(refreshBtn);
        header.add(headerBtns, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Panel body = new Panel(new BorderLayout(0, 12));
        body.setBackground(palette.getPageBg());

        // Filter row
        AwtCardPanel filterCard = new AwtCardPanel(new BorderLayout(10, 0), 10, 14);
        filterCard.applyTheme(palette);
        Label filterLbl = new Label("Filter Stocks:");
        filterLbl.setFont(ThemePalette.FONT_BUTTON);
        filterLbl.setForeground(palette.getTextPrimary());
        filterLbl.setBackground(palette.getCardBg());
        this.searchField = styledField("", 26);
        searchField.addTextListener(e -> loadStocks(searchField.getText()));
        filterCard.add(filterLbl, BorderLayout.WEST);
        filterCard.add(searchField, BorderLayout.CENTER);
        body.add(filterCard, BorderLayout.NORTH);

        // Table + bottom split
        Panel contentStack = new Panel(new BorderLayout(0, 12));
        contentStack.setBackground(palette.getPageBg());

        AwtCardPanel tableCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        tableCard.applyTheme(palette);
        String cols = String.format("%-4s | %-16s | %-10s | %-5s | %-13s | %-13s | %-9s | %-16s",
                "ID", "NAME", "SYMBOL", "PTS", "LATEST", "PREDICTED", "TREND", "CREATED AT");
        Label colHeader = new Label(cols);
        colHeader.setFont(ThemePalette.FONT_MONO_SMALL);
        colHeader.setForeground(palette.getAccentViolet());
        colHeader.setBackground(palette.getChromeBg());

        this.stockTable = new java.awt.List(7, false);
        stockTable.setFont(ThemePalette.FONT_MONO_SMALL);
        stockTable.setBackground(palette.getInputBg());
        stockTable.setForeground(palette.getTextPrimary());
        stockTable.addItemListener(e -> handleStockSelected());

        tableCard.add(colHeader, BorderLayout.NORTH);
        tableCard.add(stockTable, BorderLayout.CENTER);
        contentStack.add(tableCard, BorderLayout.NORTH);

        // Bottom 2-column split: Edit Stock Details + View Stock History
        Panel split = new Panel(new GridLayout(1, 2, 14, 0));
        split.setBackground(palette.getPageBg());

        // Left: Stock Editor
        AwtCardPanel editorCard = new AwtCardPanel(new BorderLayout(0, 10), 14, 16);
        editorCard.applyTheme(palette);
        this.editorTitleLbl = new Label("Edit Stock Details");
        editorTitleLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        editorTitleLbl.setForeground(palette.getTextPrimary());
        editorTitleLbl.setBackground(palette.getCardBg());
        editorCard.add(editorTitleLbl, BorderLayout.NORTH);

        Panel form = new Panel(new BorderLayout(0, 8));
        form.setBackground(palette.getCardBg());
        Panel topInputs = new Panel(new GridLayout(3, 1, 0, 6));
        topInputs.setBackground(palette.getCardBg());
        this.nameField = styledField("", 22);
        this.symbolField = styledField("", 16);
        this.notesField = styledField("", 26);
        topInputs.add(fieldBlock("STOCK NAME", nameField));
        topInputs.add(fieldBlock("SYMBOL", symbolField));
        topInputs.add(fieldBlock("NOTES / SECTOR", notesField));

        this.pricesArea = new TextArea("", 4, 34, TextArea.SCROLLBARS_VERTICAL_ONLY);
        pricesArea.setFont(ThemePalette.FONT_MONO_SMALL);
        pricesArea.setBackground(palette.getInputBg());
        pricesArea.setForeground(palette.getTextPrimary());

        form.add(topInputs, BorderLayout.NORTH);
        form.add(fieldBlock("CHRONOLOGICAL PRICES (oldest -> newest, comma/space separated)", pricesArea), BorderLayout.CENTER);
        editorCard.add(form, BorderLayout.CENTER);

        Panel editorFoot = new Panel(new BorderLayout(0, 6));
        editorFoot.setBackground(palette.getCardBg());
        Panel btnRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnRow.setBackground(palette.getCardBg());

        this.saveBtn = styledBtn("Save Stock Changes", palette.getAccentViolet(), Color.WHITE);
        saveBtn.addActionListener(e -> handleSaveStock());

        this.deleteBtn = styledBtn("Delete Stock", palette.getLossBg(), palette.getLossRed());
        deleteBtn.addActionListener(e -> handleDeleteStock());

        btnRow.add(saveBtn);
        btnRow.add(deleteBtn);

        this.feedbackLbl = new Label(" ");
        feedbackLbl.setFont(ThemePalette.FONT_BODY);
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setBackground(palette.getCardBg());

        editorFoot.add(btnRow, BorderLayout.NORTH);
        editorFoot.add(feedbackLbl, BorderLayout.SOUTH);
        editorCard.add(editorFoot, BorderLayout.SOUTH);

        // Right: Stock History Viewer (Chart + Sequential Prices)
        AwtCardPanel historyCard = new AwtCardPanel(new BorderLayout(0, 8), 14, 16);
        historyCard.applyTheme(palette);
        Label histTitle = new Label("Stock Price History Inspector");
        histTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        histTitle.setForeground(palette.getTextPrimary());
        histTitle.setBackground(palette.getCardBg());

        this.historyChart = new StockChart(StockChart.ChartMode.HISTORY, 360, 175);
        historyChart.applyTheme(palette);
        historyChart.setCurrency(currency);

        this.historyPointsList = new java.awt.List(6, false);
        historyPointsList.setFont(ThemePalette.FONT_MONO_SMALL);
        historyPointsList.setBackground(palette.getInputBg());
        historyPointsList.setForeground(palette.getTextPrimary());

        historyCard.add(histTitle, BorderLayout.NORTH);
        historyCard.add(historyChart, BorderLayout.CENTER);
        historyCard.add(historyPointsList, BorderLayout.SOUTH);

        split.add(editorCard);
        split.add(historyCard);
        contentStack.add(split, BorderLayout.CENTER);

        body.add(contentStack, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        loadStocks("");
        validate();
        repaint();
    }

    private void loadStocks(String query) {
        try {
            List<Stock> all = stockService.getAllStocks();
            String q = query != null ? query.trim().toLowerCase() : "";
            this.displayedStocks = all.stream()
                    .filter(s -> q.isEmpty()
                            || s.getName().toLowerCase().contains(q)
                            || s.getSymbol().toLowerCase().contains(q))
                    .toList();

            stockTable.removeAll();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Stock s : displayedStocks) {
                Prediction pred = predictionService.computeTransientPrediction(s);
                String row = String.format("%-4d | %-16s | %-10s | %-5d | %-13s | %-13s | %-9s | %-16s",
                        s.getId(),
                        truncate(s.getName(), 16),
                        truncate(s.getSymbol(), 10),
                        s.getPointCount(),
                        CurrencyConverter.format(pred.getCurrentPrice(), currency),
                        CurrencyConverter.format(pred.getPredictedPrice(), currency),
                        pred.getTrend().getLabel(),
                        fmt.format(new Date(s.getCreatedAt())));
                stockTable.add(row);
            }

            if (!addMode && !displayedStocks.isEmpty()) {
                int idx = 0;
                if (selectedStock != null) {
                    for (int i = 0; i < displayedStocks.size(); i++) {
                        if (displayedStocks.get(i).getId() == selectedStock.getId()) {
                            idx = i;
                            break;
                        }
                    }
                }
                stockTable.select(idx);
                populateStockInspector(displayedStocks.get(idx));
            } else if (displayedStocks.isEmpty() && !addMode) {
                switchToAddMode();
            }
        } catch (DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleStockSelected() {
        int idx = stockTable.getSelectedIndex();
        if (idx >= 0 && idx < displayedStocks.size()) {
            addMode = false;
            populateStockInspector(displayedStocks.get(idx));
        }
    }

    private void populateStockInspector(Stock stock) {
        this.selectedStock = stock;
        this.addMode = false;
        editorTitleLbl.setText("Edit Stock #" + stock.getId() + " (" + stock.getName() + ")");
        nameField.setText(stock.getName());
        symbolField.setText(stock.getSymbol());
        notesField.setText(stock.getNotes());
        String joined = stock.getHistory().stream()
                .map(p -> String.format("%.2f", p))
                .collect(Collectors.joining(", "));
        pricesArea.setText(joined);

        saveBtn.setLabel("Save Stock Changes");
        deleteBtn.setEnabled(true);

        historyChart.setCurrency(currency);
        historyChart.setHistoryData(stock.getHistory());

        historyPointsList.removeAll();
        List<Double> prices = stock.getHistory();
        for (int i = prices.size() - 1; i >= 0; i--) {
            historyPointsList.add(String.format("Point #%-3d : %s", i + 1, CurrencyConverter.format(prices.get(i), currency)));
        }
    }

    private void switchToAddMode() {
        this.addMode = true;
        this.selectedStock = null;
        editorTitleLbl.setText("Add New Stock to SQLite");
        nameField.setText("");
        symbolField.setText("");
        notesField.setText("");
        pricesArea.setText("");
        saveBtn.setLabel("Create Stock");
        deleteBtn.setEnabled(false);
        historyChart.setHistoryData(new ArrayList<>());
        historyPointsList.removeAll();
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setText("Enter stock details and at least 2 positive prices.");
        nameField.requestFocus();
    }

    private void handleSaveStock() {
        try {
            List<Double> prices = ValidationUtil.parsePrices(pricesArea.getText());
            if (addMode || selectedStock == null) {
                Stock created = stockService.addStock(
                        nameField.getText(),
                        symbolField.getText(),
                        prices,
                        notesField.getText()
                );
                addMode = false;
                selectedStock = created;
                loadStocks(searchField.getText());
                setSuccess("Added stock \"" + created.getName() + "\" (" + created.getPointCount() + " points).");
            } else {
                Stock updated = stockService.updateStock(
                        selectedStock.getId(),
                        nameField.getText(),
                        symbolField.getText(),
                        notesField.getText(),
                        prices
                );
                selectedStock = updated;
                loadStocks(searchField.getText());
                setSuccess("Updated stock \"" + updated.getName() + "\" and recalculated predictions.");
            }
            if (onStocksChanged != null) onStocksChanged.run();
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleDeleteStock() {
        if (selectedStock == null) {
            setError("Select a stock to delete.");
            return;
        }
        Frame owner = findParentFrame();
        boolean ok = ConfirmDialog.showConfirm(
                owner,
                "Delete Stock \"" + selectedStock.getName() + "\"?",
                "Permanently delete \"" + selectedStock.getName() + "\" and all associated price history, predictions, and technical indicators?",
                "Delete Stock",
                palette
        );
        if (!ok) {
            return;
        }
        try {
            String deletedName = selectedStock.getName();
            stockService.deleteStock(deletedName);
            selectedStock = null;
            loadStocks(searchField.getText());
            setSuccess("Deleted stock \"" + deletedName + "\".");
            if (onStocksChanged != null) onStocksChanged.run();
        } catch (DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void setError(String msg) {
        feedbackLbl.setForeground(palette.getLossRed());
        feedbackLbl.setText(msg);
    }

    private void setSuccess(String msg) {
        feedbackLbl.setForeground(palette.getGainGreen());
        feedbackLbl.setText(msg);
    }

    private Button styledBtn(String label, Color bg, Color fg) {
        Button b = new Button(label);
        b.setFont(ThemePalette.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private TextField styledField(String text, int cols) {
        TextField tf = new TextField(text, cols);
        tf.setFont(ThemePalette.FONT_MONO);
        tf.setBackground(palette.getInputBg());
        tf.setForeground(palette.getTextPrimary());
        return tf;
    }

    private Panel fieldBlock(String caption, Component input) {
        Panel p = new Panel(new BorderLayout(0, 3));
        p.setBackground(palette.getCardBg());
        Label l = new Label(caption);
        l.setFont(ThemePalette.FONT_LABEL);
        l.setForeground(palette.getTextSecondary());
        l.setBackground(palette.getCardBg());
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private Frame findParentFrame() {
        Component c = this;
        while (c != null) {
            if (c instanceof Frame f) return f;
            c = c.getParent();
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
