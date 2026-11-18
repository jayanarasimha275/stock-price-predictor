package ui;

import model.Currency;
import model.Stock;
import model.User;
import repository.SettingsRepository;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary AWT desktop Frame orchestrating the HeaderPanel, Sidebar,
 * scrollable module workspace, footer disclaimer, authenticated user session,
 * and persistent theme/currency settings.
 */
public class MainFrame extends Frame implements StockSelectionListener {

    private static final String PREF_CURRENCY = "currency";
    private static final String PREF_THEME = "theme";

    private final StockService stockService;
    private final SettingsRepository settingsRepository;
    private final User currentUser;
    private final Runnable onLogout;

    private final HeaderPanel headerPanel;
    private final Sidebar sidebar;
    private final ScrollPane scrollPane;
    private final Panel contentStage;
    private final Panel footerPanel;
    private final Label footerLine1;
    private final Label footerLine2;

    private final Map<String, BaseModulePanel> modules = new LinkedHashMap<>();

    private ThemePalette palette;
    private Currency currency;
    private String activeModuleId = "overview";
    private String selectedStockName = "";

    public MainFrame(StockService stockService,
                     PredictionService predictionService,
                     IndicatorService indicatorService,
                     HistoryService historyService,
                     SettingsRepository settingsRepository) {
        this(stockService, predictionService, indicatorService, historyService, settingsRepository, null, null);
    }

    public MainFrame(StockService stockService,
                     PredictionService predictionService,
                     IndicatorService indicatorService,
                     HistoryService historyService,
                     SettingsRepository settingsRepository,
                     User currentUser,
                     Runnable onLogout) {
        super("Stock Price Predictor — CSE 213 Advanced Java Programming | SRM University AP");
        this.stockService = stockService;
        this.settingsRepository = settingsRepository;
        this.currentUser = currentUser;
        this.onLogout = onLogout;

        // Restore persisted theme & currency preferences from SQLite
        String savedTheme = settingsRepository.getSetting(PREF_THEME, "dark");
        this.palette = "light".equalsIgnoreCase(savedTheme) ? ThemePalette.LIGHT : ThemePalette.DARK;
        this.currency = Currency.fromCode(settingsRepository.getSetting(PREF_CURRENCY, "INR"));

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(980, 660));
        setSize(1280, 820);
        setLocationRelativeTo(null);

        // Header
        this.headerPanel = new HeaderPanel(new HeaderPanel.HeaderListener() {
            @Override
            public void onCurrencyChanged(Currency newCurrency) {
                handleCurrencyChange(newCurrency);
            }

            @Override
            public void onToggleTheme() {
                handleThemeToggle();
            }

            @Override
            public void onOpenHowItWorks() {
                new HowItWorksDialog(MainFrame.this, palette).setVisible(true);
            }

            @Override
            public void onLogout() {
                dispose();
                if (MainFrame.this.onLogout != null) {
                    MainFrame.this.onLogout.run();
                }
            }
        });
        headerPanel.setAuthenticatedUser(currentUser, onLogout != null);
        add(headerPanel, BorderLayout.NORTH);

        // Sidebar
        this.sidebar = new Sidebar(moduleId -> onNavigate(moduleId, null));
        add(sidebar, BorderLayout.WEST);

        // Scrollable Center Stage
        this.scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        this.contentStage = new Panel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int vpW = scrollPane.getViewportSize().width;
                if (vpW > 100) {
                    return new Dimension(Math.max(vpW - 4, 720), Math.max(d.height, scrollPane.getViewportSize().height));
                }
                return d;
            }
        };
        scrollPane.add(contentStage);
        add(scrollPane, BorderLayout.CENTER);

        // Footer Disclaimer
        this.footerPanel = new Panel(new GridLayout(2, 1, 0, 2)) {
            @Override
            public Insets getInsets() {
                return new Insets(8, 16, 8, 16);
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(palette.getBorder());
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        this.footerLine1 = new Label(
                "Predictions are estimates based on a simple linear trend and are not financial advice. Actual stock performance may differ.",
                Label.CENTER
        );
        footerLine1.setFont(ThemePalette.FONT_BODY);
        this.footerLine2 = new Label(
                "CSE 213 – Advanced Java Programming | SRM University AP · Pure Java AWT & SQLite JDBC",
                Label.CENTER
        );
        footerLine2.setFont(ThemePalette.FONT_SUBTITLE);
        footerPanel.add(footerLine1);
        footerPanel.add(footerLine2);
        add(footerPanel, BorderLayout.SOUTH);

        // Register all 10 polymorphic modules
        registerModule(new DashboardPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new AddStockPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new PredictionPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new IndicatorPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new HistoryPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new AccuracyPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new ForecastPanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new PriceChangePanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new ComparePanel(stockService, predictionService, indicatorService, historyService, this));
        registerModule(new DeleteStockPanel(stockService, predictionService, indicatorService, historyService, this));

        // Responsive relayout when frame is resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                contentStage.invalidate();
                scrollPane.doLayout();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        applyCurrentTheme();
        headerPanel.setCurrency(currency);
        syncStockMetadata();
        onNavigate("overview", null);
    }

    private void registerModule(BaseModulePanel module) {
        modules.put(module.getModuleId(), module);
    }

    @Override
    public void onNavigate(String moduleId, String stockName) {
        if (stockName != null && !stockName.trim().isEmpty()) {
            this.selectedStockName = stockName;
        }
        BaseModulePanel target = modules.get(moduleId);
        if (target == null) {
            return;
        }
        this.activeModuleId = moduleId;
        sidebar.setActiveModule(moduleId);

        target.setCurrency(currency);
        target.setSelectedStockName(selectedStockName);
        target.applyTheme(palette);
        target.refreshData();

        contentStage.removeAll();
        contentStage.add(target, BorderLayout.NORTH);
        contentStage.validate();
        scrollPane.doLayout();
        scrollPane.setScrollPosition(0, 0);
        validate();
        repaint();
    }

    @Override
    public void onPortfolioChanged(String preferredSelectedStock) {
        if (preferredSelectedStock != null && !preferredSelectedStock.trim().isEmpty()) {
            this.selectedStockName = preferredSelectedStock;
        }
        syncStockMetadata();
        BaseModulePanel current = modules.get(activeModuleId);
        if (current != null) {
            current.setSelectedStockName(selectedStockName);
            current.refreshData();
            contentStage.validate();
            scrollPane.doLayout();
        }
    }

    private void syncStockMetadata() {
        try {
            List<Stock> stocks = stockService.getAllStocks();
            headerPanel.setStockCount(stocks.size());
            sidebar.setStockCount(stocks.size());
            if (!stocks.isEmpty()) {
                boolean exists = stocks.stream().anyMatch(s -> s.getName().equalsIgnoreCase(selectedStockName));
                if (!exists) {
                    selectedStockName = stocks.get(0).getName();
                }
            } else {
                selectedStockName = "";
            }
        } catch (DatabaseException ex) {
            ConfirmDialog.showAlert(this, "Database Error", ex.getMessage(), palette);
        }
    }

    private void handleCurrencyChange(Currency newCurrency) {
        this.currency = newCurrency;
        settingsRepository.setSetting(PREF_CURRENCY, newCurrency.getCode());
        for (BaseModulePanel mod : modules.values()) {
            mod.setCurrency(newCurrency);
        }
        BaseModulePanel active = modules.get(activeModuleId);
        if (active != null) {
            active.refreshData();
            contentStage.validate();
            scrollPane.doLayout();
        }
    }

    private void handleThemeToggle() {
        this.palette = palette.isDark() ? ThemePalette.LIGHT : ThemePalette.DARK;
        settingsRepository.setSetting(PREF_THEME, palette.isDark() ? "dark" : "light");
        applyCurrentTheme();
        BaseModulePanel active = modules.get(activeModuleId);
        if (active != null) {
            active.applyTheme(palette);
            active.refreshData();
            contentStage.validate();
            scrollPane.doLayout();
        }
    }

    private void applyCurrentTheme() {
        setBackground(palette.getPageBg());
        headerPanel.applyTheme(palette);
        sidebar.applyTheme(palette);
        scrollPane.setBackground(palette.getPageBg());
        contentStage.setBackground(palette.getPageBg());
        footerPanel.setBackground(palette.getChromeBg());
        footerLine1.setBackground(palette.getChromeBg());
        footerLine1.setForeground(palette.getTextPrimary());
        footerLine2.setBackground(palette.getChromeBg());
        footerLine2.setForeground(palette.getTextSecondary());
        repaint();
    }
}
