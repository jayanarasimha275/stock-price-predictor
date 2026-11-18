package ui;

import model.Currency;
import model.Stock;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.ThemePalette;

import java.awt.*;
import java.util.List;

/**
 * Abstract base Panel for all 10 application modules.
 * Demonstrates Abstraction, Inheritance, and Polymorphism across AWT views.
 */
public abstract class BaseModulePanel extends Panel implements ThemeAware {

    protected final StockService stockService;
    protected final PredictionService predictionService;
    protected final IndicatorService indicatorService;
    protected final HistoryService historyService;
    protected final StockSelectionListener navigator;

    protected ThemePalette palette = ThemePalette.DARK;
    protected Currency currency = Currency.INR;
    protected String selectedStockName = "";

    protected BaseModulePanel(StockService stockService,
                              PredictionService predictionService,
                              IndicatorService indicatorService,
                              HistoryService historyService,
                              StockSelectionListener navigator) {
        this.stockService = stockService;
        this.predictionService = predictionService;
        this.indicatorService = indicatorService;
        this.historyService = historyService;
        this.navigator = navigator;
        setLayout(new BorderLayout(0, 14));
    }

    public abstract String getModuleId();

    public abstract String getModuleTitle();

    public abstract void refreshData();

    public void setCurrency(Currency currency) {
        this.currency = currency != null ? currency : Currency.INR;
    }

    public void setSelectedStockName(String selectedStockName) {
        this.selectedStockName = selectedStockName != null ? selectedStockName : "";
    }

    public String getSelectedStockName() {
        return selectedStockName;
    }

    @Override
    public Insets getInsets() {
        return new Insets(18, 22, 22, 22);
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        setBackground(palette.getPageBg());
        applyThemeRecursive(this, palette);
        repaint();
    }

    protected void applyThemeRecursive(Container container, ThemePalette p) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof ThemeAware aware && comp != this) {
                aware.applyTheme(p);
            }
            if (comp instanceof Container child && !(comp instanceof BaseChartCanvas)) {
                applyThemeRecursive(child, p);
            }
        }
    }

    protected Stock resolveActiveStock(List<Stock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return null;
        }
        for (Stock s : stocks) {
            if (s.getName().equalsIgnoreCase(selectedStockName)) {
                return s;
            }
        }
        selectedStockName = stocks.get(0).getName();
        return stocks.get(0);
    }

    protected Panel buildModuleHeader(String title, String subtitle, Component rightControl) {
        Panel header = new Panel(new BorderLayout(12, 4));
        header.setBackground(palette.getPageBg());

        Panel left = new Panel(new GridLayout(2, 1, 0, 2));
        left.setBackground(palette.getPageBg());

        Label titleLbl = new Label(title);
        titleLbl.setFont(ThemePalette.FONT_TITLE);
        titleLbl.setForeground(palette.getTextPrimary());
        titleLbl.setBackground(palette.getPageBg());

        Label subLbl = new Label(subtitle);
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        subLbl.setForeground(palette.getTextSecondary());
        subLbl.setBackground(palette.getPageBg());

        left.add(titleLbl);
        left.add(subLbl);
        header.add(left, BorderLayout.CENTER);

        if (rightControl != null) {
            Panel rightWrap = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            rightWrap.setBackground(palette.getPageBg());
            rightWrap.add(rightControl);
            header.add(rightWrap, BorderLayout.EAST);
        }
        return header;
    }

    protected Panel buildEmptyStatePanel(String featureTitle, Runnable onLoadSample) {
        AwtCardPanel card = new AwtCardPanel(new GridBagLayout(), 36, 28);
        card.applyTheme(palette);

        Panel inner = new Panel(new GridLayout(0, 1, 0, 10));
        inner.setBackground(palette.getCardBg());

        Label heading = new Label("No Stocks Defined Yet", Label.CENTER);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        heading.setForeground(palette.getTextPrimary());
        heading.setBackground(palette.getCardBg());

        Label desc = new Label(
                "To use " + featureTitle + ", add a stock manually/via CSV or load sample data into SQLite.",
                Label.CENTER
        );
        desc.setFont(ThemePalette.FONT_BODY);
        desc.setForeground(palette.getTextSecondary());
        desc.setBackground(palette.getCardBg());

        Panel btns = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btns.setBackground(palette.getCardBg());

        Button addBtn = createStyledButton("Add Your First Stock", palette.getAccentViolet(), Color.WHITE);
        addBtn.addActionListener(e -> navigator.onNavigate("add", null));

        Button sampleBtn = createStyledButton("Load Sample Data", palette.getHoverBg(), palette.getTextPrimary());
        sampleBtn.addActionListener(e -> {
            if (onLoadSample != null) {
                onLoadSample.run();
            }
        });

        btns.add(addBtn);
        btns.add(sampleBtn);

        inner.add(heading);
        inner.add(desc);
        inner.add(btns);
        card.add(inner);

        Panel wrap = new Panel(new BorderLayout());
        wrap.setBackground(palette.getPageBg());
        wrap.add(card, BorderLayout.NORTH);
        return wrap;
    }

    protected Button createStyledButton(String text, Color bg, Color fg) {
        Button btn = new Button(text);
        btn.setFont(ThemePalette.FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    protected TextField createStyledTextField(String initial, int cols) {
        TextField tf = new TextField(initial, cols);
        tf.setFont(ThemePalette.FONT_MONO);
        tf.setBackground(palette.getInputBg());
        tf.setForeground(palette.getTextPrimary());
        return tf;
    }

    protected Choice createStyledChoice() {
        Choice choice = new Choice();
        choice.setFont(ThemePalette.FONT_MONO);
        choice.setBackground(palette.getCardBg());
        choice.setForeground(palette.getTextPrimary());
        return choice;
    }

    protected AwtCardPanel createKpiCard(String caption, String value, String subtext, Color valueColor) {
        AwtCardPanel card = new AwtCardPanel(new GridLayout(3, 1, 0, 3), 12, 14);
        card.applyTheme(palette);

        Label capLbl = new Label(caption.toUpperCase());
        capLbl.setFont(ThemePalette.FONT_LABEL);
        capLbl.setForeground(palette.getTextSecondary());
        capLbl.setBackground(palette.getCardBg());

        Label valLbl = new Label(value);
        valLbl.setFont(ThemePalette.FONT_KPI);
        valLbl.setForeground(valueColor != null ? valueColor : palette.getTextPrimary());
        valLbl.setBackground(palette.getCardBg());

        Label subLbl = new Label(subtext);
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        subLbl.setForeground(palette.getTextSecondary());
        subLbl.setBackground(palette.getCardBg());

        card.add(capLbl);
        card.add(valLbl);
        card.add(subLbl);
        return card;
    }

    protected void showErrorDialog(String title, String message) {
        Window owner = findParentWindow();
        if (owner instanceof Frame frame) {
            ConfirmDialog.showAlert(frame, title, message, palette);
        }
    }

    protected Frame findParentFrame() {
        Window w = findParentWindow();
        return (w instanceof Frame f) ? f : null;
    }

    private Window findParentWindow() {
        Component c = this;
        while (c != null) {
            if (c instanceof Window w) {
                return w;
            }
            c = c.getParent();
        }
        return null;
    }
}
