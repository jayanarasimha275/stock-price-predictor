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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Module 2: Add / Load Stock.
 * Supports Stock Name, Ticker Symbol, Notes, and three price input methods:
 * (a) Paste list, (b) One-by-one entry, (c) CSV file upload via FileDialog,
 * plus Load Sample Data and SQLite permanent storage.
 */
public class AddStockPanel extends BaseModulePanel {

    private enum InputMethod { PASTE, MANUAL, CSV }

    private InputMethod currentMethod = InputMethod.PASTE;

    private TextField nameField;
    private TextField symbolField;
    private TextField notesField;
    private TextArea pasteArea;

    private TextField manualInputField;
    private java.awt.List manualAwtList;
    private final List<Double> manualPrices = new ArrayList<>();

    private Label csvFileLbl;
    private Choice csvColumnChoice;
    private File selectedCsvFile;
    private final List<Double> csvPrices = new ArrayList<>();

    private Panel methodCardsPanel;
    private CardLayout methodCardsLayout;
    private Button pasteTabBtn;
    private Button manualTabBtn;
    private Button csvTabBtn;

    private Label statusLbl;
    private Label summaryLbl;

    public AddStockPanel(StockService stockService, PredictionService predictionService,
                         IndicatorService indicatorService, HistoryService historyService,
                         StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
        buildUi();
    }

    @Override
    public String getModuleId() {
        return "add";
    }

    @Override
    public String getModuleTitle() {
        return "Add / Load Stock";
    }

    private void buildUi() {
        removeAll();
        setBackground(palette.getPageBg());

        Panel headerActions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setBackground(palette.getPageBg());

        Button fillDemoFormBtn = createStyledButton("Fill Sample Form", palette.getAccentVioletSoft(), palette.getAccentViolet());
        fillDemoFormBtn.addActionListener(e -> prefillSampleForm());

        Button loadSqliteDemoBtn = createStyledButton("Load SQLite Sample Portfolio", palette.getHoverBg(), palette.getTextPrimary());
        loadSqliteDemoBtn.addActionListener(e -> loadSamplePortfolioToDb());

        headerActions.add(fillDemoFormBtn);
        headerActions.add(loadSqliteDemoBtn);

        add(buildModuleHeader(
                "Add / Load Stock",
                "Enter stock name, symbol, and chronological prices (oldest -> newest) to persist in SQLite.",
                headerActions
        ), BorderLayout.NORTH);

        Panel formStack = new Panel(new BorderLayout(0, 14));
        formStack.setBackground(palette.getPageBg());

        // Metadata Card: Name, Symbol, Notes
        AwtCardPanel metaCard = new AwtCardPanel(new GridLayout(3, 2, 12, 10), 14, 16);
        metaCard.applyTheme(palette);

        Label nameCap = new Label("Stock Name (unique, case-insensitive) *");
        nameCap.setFont(ThemePalette.FONT_BUTTON);
        nameCap.setForeground(palette.getTextPrimary());
        nameCap.setBackground(palette.getCardBg());
        this.nameField = createStyledTextField("", 28);

        Label symCap = new Label("Ticker Symbol *");
        symCap.setFont(ThemePalette.FONT_BUTTON);
        symCap.setForeground(palette.getTextPrimary());
        symCap.setBackground(palette.getCardBg());
        this.symbolField = createStyledTextField("", 16);

        Label notesCap = new Label("Notes / Sector (optional)");
        notesCap.setFont(ThemePalette.FONT_BUTTON);
        notesCap.setForeground(palette.getTextPrimary());
        notesCap.setBackground(palette.getCardBg());
        this.notesField = createStyledTextField("", 28);

        metaCard.add(nameCap);
        metaCard.add(nameField);
        metaCard.add(symCap);
        metaCard.add(symbolField);
        metaCard.add(notesCap);
        metaCard.add(notesField);
        formStack.add(metaCard, BorderLayout.NORTH);

        // Input Method Tabs + Content Card
        AwtCardPanel inputCard = new AwtCardPanel(new BorderLayout(0, 12), 14, 16);
        inputCard.applyTheme(palette);

        Panel tabBar = new Panel(new GridLayout(1, 3, 8, 0));
        tabBar.setBackground(palette.getCardBg());
        this.pasteTabBtn = createStyledButton("(a) Paste Price List", palette.getAccentViolet(), Color.WHITE);
        this.manualTabBtn = createStyledButton("(b) One-by-One Entry", palette.getHoverBg(), palette.getTextPrimary());
        this.csvTabBtn = createStyledButton("(c) Import CSV File", palette.getHoverBg(), palette.getTextPrimary());

        pasteTabBtn.addActionListener(e -> switchMethod(InputMethod.PASTE));
        manualTabBtn.addActionListener(e -> switchMethod(InputMethod.MANUAL));
        csvTabBtn.addActionListener(e -> switchMethod(InputMethod.CSV));

        tabBar.add(pasteTabBtn);
        tabBar.add(manualTabBtn);
        tabBar.add(csvTabBtn);
        inputCard.add(tabBar, BorderLayout.NORTH);

        this.methodCardsLayout = new CardLayout();
        this.methodCardsPanel = new Panel(methodCardsLayout);
        methodCardsPanel.setBackground(palette.getCardBg());

        // Method A: Paste
        Panel pastePanel = new Panel(new BorderLayout(0, 6));
        pastePanel.setBackground(palette.getCardBg());
        Label pasteHelp = new Label("Paste prices separated by commas, spaces, or newlines (oldest -> newest):");
        pasteHelp.setFont(ThemePalette.FONT_BODY);
        pasteHelp.setForeground(palette.getTextSecondary());
        pasteHelp.setBackground(palette.getCardBg());

        this.pasteArea = new TextArea("", 6, 60, TextArea.SCROLLBARS_VERTICAL_ONLY);
        pasteArea.setFont(ThemePalette.FONT_MONO);
        pasteArea.setBackground(palette.getInputBg());
        pasteArea.setForeground(palette.getTextPrimary());
        pasteArea.addTextListener(e -> updateLiveSummary());

        pastePanel.add(pasteHelp, BorderLayout.NORTH);
        pastePanel.add(pasteArea, BorderLayout.CENTER);
        methodCardsPanel.add(pastePanel, "PASTE");

        // Method B: Manual One-by-One
        Panel manualPanel = new Panel(new BorderLayout(0, 8));
        manualPanel.setBackground(palette.getCardBg());

        Panel addRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addRow.setBackground(palette.getCardBg());
        this.manualInputField = createStyledTextField("", 16);
        manualInputField.addActionListener(e -> addManualPrice());
        Button addPriceBtn = createStyledButton("Add Price", palette.getAccentViolet(), Color.WHITE);
        addPriceBtn.addActionListener(e -> addManualPrice());
        Button removePriceBtn = createStyledButton("Remove Selected", palette.getHoverBg(), palette.getLossRed());
        removePriceBtn.addActionListener(e -> removeSelectedManualPrice());
        Button clearManualBtn = createStyledButton("Clear List", palette.getHoverBg(), palette.getTextSecondary());
        clearManualBtn.addActionListener(e -> {
            manualPrices.clear();
            refreshManualList();
        });

        addRow.add(new Label("Price:"));
        addRow.add(manualInputField);
        addRow.add(addPriceBtn);
        addRow.add(removePriceBtn);
        addRow.add(clearManualBtn);

        this.manualAwtList = new java.awt.List(6, false);
        manualAwtList.setFont(ThemePalette.FONT_MONO);
        manualAwtList.setBackground(palette.getInputBg());
        manualAwtList.setForeground(palette.getTextPrimary());

        manualPanel.add(addRow, BorderLayout.NORTH);
        manualPanel.add(manualAwtList, BorderLayout.CENTER);
        methodCardsPanel.add(manualPanel, "MANUAL");

        // Method C: CSV Upload
        Panel csvPanel = new Panel(new BorderLayout(0, 10));
        csvPanel.setBackground(palette.getCardBg());
        Panel csvTop = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        csvTop.setBackground(palette.getCardBg());

        Button browseCsvBtn = createStyledButton("Choose CSV File...", palette.getAccentViolet(), Color.WHITE);
        browseCsvBtn.addActionListener(e -> openCsvFileDialog());

        this.csvFileLbl = new Label("No file chosen (supports single column or Date,Close tables)");
        csvFileLbl.setFont(ThemePalette.FONT_BODY);
        csvFileLbl.setForeground(palette.getTextSecondary());
        csvFileLbl.setBackground(palette.getCardBg());

        csvTop.add(browseCsvBtn);
        csvTop.add(csvFileLbl);

        Panel csvColRow = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        csvColRow.setBackground(palette.getCardBg());
        Label colLbl = new Label("Price Column:");
        colLbl.setFont(ThemePalette.FONT_BUTTON);
        colLbl.setForeground(palette.getTextPrimary());
        colLbl.setBackground(palette.getCardBg());
        this.csvColumnChoice = createStyledChoice();
        csvColumnChoice.add("Column 1");
        csvColumnChoice.addItemListener(e -> reparseCsvColumn());

        csvColRow.add(colLbl);
        csvColRow.add(csvColumnChoice);

        csvPanel.add(csvTop, BorderLayout.NORTH);
        csvPanel.add(csvColRow, BorderLayout.CENTER);
        methodCardsPanel.add(csvPanel, "CSV");

        inputCard.add(methodCardsPanel, BorderLayout.CENTER);
        formStack.add(inputCard, BorderLayout.CENTER);

        // Bottom Submit & Feedback Bar
        AwtCardPanel footerCard = new AwtCardPanel(new BorderLayout(12, 8), 12, 16);
        footerCard.applyTheme(palette);

        Panel statusStack = new Panel(new GridLayout(2, 1, 0, 2));
        statusStack.setBackground(palette.getCardBg());
        this.summaryLbl = new Label("Input Summary: 0 valid price points detected (minimum 2 required).");
        summaryLbl.setFont(ThemePalette.FONT_BUTTON);
        summaryLbl.setForeground(palette.getTextPrimary());
        summaryLbl.setBackground(palette.getCardBg());

        this.statusLbl = new Label("Ready to save stock permanently to SQLite.");
        statusLbl.setFont(ThemePalette.FONT_BODY);
        statusLbl.setForeground(palette.getTextSecondary());
        statusLbl.setBackground(palette.getCardBg());

        statusStack.add(summaryLbl);
        statusStack.add(statusLbl);

        Button saveBtn = createStyledButton("Save Stock to SQLite", palette.getAccentViolet(), Color.WHITE);
        saveBtn.addActionListener(e -> handleSaveStock());

        footerCard.add(statusStack, BorderLayout.CENTER);
        footerCard.add(saveBtn, BorderLayout.EAST);
        formStack.add(footerCard, BorderLayout.SOUTH);

        add(formStack, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        applyTheme(palette);
        updateLiveSummary();
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        setBackground(palette.getPageBg());
        applyThemeRecursive(this, palette);
        if (nameField != null) {
            nameField.setBackground(palette.getInputBg());
            nameField.setForeground(palette.getTextPrimary());
            symbolField.setBackground(palette.getInputBg());
            symbolField.setForeground(palette.getTextPrimary());
            notesField.setBackground(palette.getInputBg());
            notesField.setForeground(palette.getTextPrimary());
            pasteArea.setBackground(palette.getInputBg());
            pasteArea.setForeground(palette.getTextPrimary());
            manualInputField.setBackground(palette.getInputBg());
            manualInputField.setForeground(palette.getTextPrimary());
            manualAwtList.setBackground(palette.getInputBg());
            manualAwtList.setForeground(palette.getTextPrimary());
            csvColumnChoice.setBackground(palette.getCardBg());
            csvColumnChoice.setForeground(palette.getTextPrimary());
            updateMethodButtons();
        }
        repaint();
    }

    private void switchMethod(InputMethod method) {
        this.currentMethod = method;
        methodCardsLayout.show(methodCardsPanel, method.name());
        updateMethodButtons();
        updateLiveSummary();
    }

    private void updateMethodButtons() {
        if (pasteTabBtn == null) return;
        styleTabButton(pasteTabBtn, currentMethod == InputMethod.PASTE);
        styleTabButton(manualTabBtn, currentMethod == InputMethod.MANUAL);
        styleTabButton(csvTabBtn, currentMethod == InputMethod.CSV);
    }

    private void styleTabButton(Button btn, boolean active) {
        btn.setBackground(active ? palette.getAccentViolet() : palette.getHoverBg());
        btn.setForeground(active ? Color.WHITE : palette.getTextPrimary());
    }

    private void addManualPrice() {
        try {
            double val = ValidationUtil.parseSinglePositivePrice(manualInputField.getText());
            manualPrices.add(val);
            manualInputField.setText("");
            refreshManualList();
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Added price " + CurrencyConverter.format(val, currency));
        } catch (ValidationException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText(ex.getMessage());
        }
    }

    private void removeSelectedManualPrice() {
        int idx = manualAwtList.getSelectedIndex();
        if (idx >= 0 && idx < manualPrices.size()) {
            manualPrices.remove(idx);
            refreshManualList();
        }
    }

    private void refreshManualList() {
        manualAwtList.removeAll();
        for (int i = 0; i < manualPrices.size(); i++) {
            manualAwtList.add("P" + (i + 1) + "  ->  " + CurrencyConverter.format(manualPrices.get(i), currency));
        }
        updateLiveSummary();
    }

    private void openCsvFileDialog() {
        Frame owner = findParentFrame();
        FileDialog fd = new FileDialog(owner, "Select Historical Prices CSV File", FileDialog.LOAD);
        fd.setFile("*.csv");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) {
            return;
        }
        selectedCsvFile = new File(dir, file);
        try {
            List<String> headers = ValidationUtil.readCsvHeaders(selectedCsvFile);
            csvColumnChoice.removeAll();
            for (String h : headers) {
                csvColumnChoice.add(h);
            }
            if (headers.size() > 1) {
                // Default to second column (common Date,Close format) if available
                csvColumnChoice.select(1);
            }
            reparseCsvColumn();
        } catch (ValidationException ex) {
            csvPrices.clear();
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText(ex.getMessage());
            updateLiveSummary();
        }
    }

    private void reparseCsvColumn() {
        if (selectedCsvFile == null) {
            return;
        }
        int colIdx = Math.max(0, csvColumnChoice.getSelectedIndex());
        try {
            List<Double> extracted = ValidationUtil.parsePrices(selectedCsvFile, colIdx);
            csvPrices.clear();
            csvPrices.addAll(extracted);
            csvFileLbl.setText(selectedCsvFile.getName() + " (" + csvPrices.size() + " prices loaded)");
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Loaded " + csvPrices.size() + " prices from CSV column \"" + csvColumnChoice.getSelectedItem() + "\".");
        } catch (ValidationException ex) {
            csvPrices.clear();
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText(ex.getMessage());
        }
        updateLiveSummary();
    }

    private void prefillSampleForm() {
        int suffix = (int) (Math.random() * 900 + 100);
        nameField.setText("TATA-TECH-" + suffix);
        symbolField.setText("TATATECH");
        notesField.setText("Sample demo stock");
        List<Double> sample = Arrays.asList(1240.50, 1248.00, 1255.75, 1251.20, 1262.40, 1270.00, 1268.50, 1279.80, 1285.30, 1294.60);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sample.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(sample.get(i));
        }
        pasteArea.setText(sb.toString());
        switchMethod(InputMethod.PASTE);
        statusLbl.setForeground(palette.getFlatAmber());
        statusLbl.setText("Sample demo data prefilled. Click 'Save Stock to SQLite' to store it.");
        updateLiveSummary();
    }

    private void loadSamplePortfolioToDb() {
        try {
            DatabaseSeeder.loadSamplePortfolio(stockService);
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Sample portfolio (RELIANCE, TCS, INFY) saved to SQLite.");
            navigator.onPortfolioChanged("RELIANCE");
        } catch (DatabaseException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText(ex.getMessage());
        }
    }

    private void updateLiveSummary() {
        if (summaryLbl == null) return;
        List<Double> active = resolveCurrentPricesQuietly();
        if (active.size() >= 2) {
            summaryLbl.setText("Input Summary: " + active.size() + " valid prices | First: "
                    + CurrencyConverter.format(active.get(0), currency) + " | Latest: "
                    + CurrencyConverter.format(active.get(active.size() - 1), currency));
            summaryLbl.setForeground(palette.getGainGreen());
        } else {
            summaryLbl.setText("Input Summary: " + active.size() + " valid prices detected (minimum 2 required).");
            summaryLbl.setForeground(palette.getFlatAmber());
        }
    }

    private List<Double> resolveCurrentPricesQuietly() {
        if (currentMethod == InputMethod.PASTE) {
            try {
                return ValidationUtil.parsePrices(pasteArea.getText());
            } catch (ValidationException ex) {
                return new ArrayList<>();
            }
        } else if (currentMethod == InputMethod.MANUAL) {
            return manualPrices;
        } else {
            return csvPrices;
        }
    }

    private void handleSaveStock() {
        try {
            List<Double> prices;
            if (currentMethod == InputMethod.PASTE) {
                prices = ValidationUtil.parsePrices(pasteArea.getText());
            } else if (currentMethod == InputMethod.MANUAL) {
                prices = new ArrayList<>(manualPrices);
            } else {
                prices = new ArrayList<>(csvPrices);
            }

            String name = nameField.getText().trim();
            String symbol = symbolField.getText().trim().isEmpty() ? name : symbolField.getText().trim();
            String notes = notesField.getText().trim();

            Stock saved = stockService.addStock(name, symbol, prices, notes);
            statusLbl.setForeground(palette.getGainGreen());
            statusLbl.setText("Saved \"" + saved.getName() + "\" (" + saved.getPointCount() + " points) permanently to SQLite.");

            nameField.setText("");
            symbolField.setText("");
            notesField.setText("");
            pasteArea.setText("");
            manualPrices.clear();
            refreshManualList();
            csvPrices.clear();
            selectedCsvFile = null;
            csvFileLbl.setText("No file chosen");

            navigator.onPortfolioChanged(saved.getName());
        } catch (ValidationException | DatabaseException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText("Validation / Save Error: " + ex.getMessage());
        }
    }
}
