package ui;

import database.DatabaseSeeder;
import model.Stock;
import model.TechnicalIndicator;
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
 * Module 4: Technical Indicators.
 * Implements SMA, EMA, Momentum, RSI, MACD, and OLS Trend Slope with customizable periods,
 * SQLite persistence, and dual Graphics2D charts (Overlay + RSI/MACD).
 */
public class IndicatorPanel extends BaseModulePanel {

    private int smaPeriod = 5;
    private int emaPeriod = 5;
    private int momentumPeriod = 3;
    private int rsiPeriod = 14;
    private int macdFast = 12;
    private int macdSlow = 26;
    private int macdSignal = 9;

    private TextField smaTf;
    private TextField emaTf;
    private TextField momTf;
    private TextField rsiTf;
    private TextField macdFastTf;
    private TextField macdSlowTf;
    private TextField macdSigTf;
    private Choice stockChoice;

    public IndicatorPanel(StockService stockService, PredictionService predictionService,
                          IndicatorService indicatorService, HistoryService historyService,
                          StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "indicators";
    }

    @Override
    public String getModuleTitle() {
        return "Technical Indicators";
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
            add(buildModuleHeader("Technical Indicators", "SMA, EMA, Momentum, RSI, and MACD calculations.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Technical Indicators", () -> {
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
                "Technical Indicators — " + active.getName(),
                "SMA, EMA, Momentum, RSI, MACD, and Linear Slope persisted in SQLite (history: " + active.getPointCount() + " pts).",
                selectorWrap
        ), BorderLayout.NORTH);

        TechnicalIndicator ind;
        try {
            ind = indicatorService.computeAndPersist(active, smaPeriod, emaPeriod, momentumPeriod,
                    rsiPeriod, macdFast, macdSlow, macdSignal);
        } catch (DatabaseException ex) {
            ind = indicatorService.computeTransient(active, smaPeriod, emaPeriod, momentumPeriod,
                    rsiPeriod, macdFast, macdSlow, macdSignal);
        }

        Panel body = new Panel(new BorderLayout(0, 12));
        body.setBackground(palette.getPageBg());

        // Top Parameter Tuning Card
        AwtCardPanel configCard = new AwtCardPanel(new BorderLayout(0, 8), 12, 14);
        configCard.applyTheme(palette);

        Panel paramRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        paramRow.setBackground(palette.getCardBg());

        this.smaTf = createStyledTextField(String.valueOf(smaPeriod), 3);
        this.emaTf = createStyledTextField(String.valueOf(emaPeriod), 3);
        this.momTf = createStyledTextField(String.valueOf(momentumPeriod), 3);
        this.rsiTf = createStyledTextField(String.valueOf(rsiPeriod), 3);
        this.macdFastTf = createStyledTextField(String.valueOf(macdFast), 3);
        this.macdSlowTf = createStyledTextField(String.valueOf(macdSlow), 3);
        this.macdSigTf = createStyledTextField(String.valueOf(macdSignal), 3);

        paramRow.add(makeParamLabel("SMA:"));
        paramRow.add(smaTf);
        paramRow.add(makeParamLabel("EMA:"));
        paramRow.add(emaTf);
        paramRow.add(makeParamLabel("Mom:"));
        paramRow.add(momTf);
        paramRow.add(makeParamLabel("RSI:"));
        paramRow.add(rsiTf);
        paramRow.add(makeParamLabel("MACD (F/S/Sig):"));
        paramRow.add(macdFastTf);
        paramRow.add(macdSlowTf);
        paramRow.add(macdSigTf);

        Button applyBtn = createStyledButton("Recalculate", palette.getAccentViolet(), Color.WHITE);
        applyBtn.addActionListener(e -> applyParameters());

        Button shortPresetBtn = createStyledButton("Short Window (3/6/3)", palette.getHoverBg(), palette.getTextPrimary());
        shortPresetBtn.addActionListener(e -> {
            smaPeriod = 5;
            emaPeriod = 5;
            momentumPeriod = 3;
            rsiPeriod = 5;
            macdFast = 3;
            macdSlow = 6;
            macdSignal = 3;
            refreshData();
        });

        Button stdPresetBtn = createStyledButton("Standard (14 & 12/26/9)", palette.getHoverBg(), palette.getTextPrimary());
        stdPresetBtn.addActionListener(e -> {
            smaPeriod = 5;
            emaPeriod = 5;
            momentumPeriod = 3;
            rsiPeriod = 14;
            macdFast = 12;
            macdSlow = 26;
            macdSignal = 9;
            refreshData();
        });

        paramRow.add(applyBtn);
        paramRow.add(shortPresetBtn);
        paramRow.add(stdPresetBtn);
        configCard.add(paramRow, BorderLayout.CENTER);
        body.add(configCard, BorderLayout.NORTH);

        // KPI Grid: 6 Indicator Cards (2 rows x 3 cols)
        Panel kpiGrid = new Panel(new GridLayout(2, 3, 12, 12));
        kpiGrid.setBackground(palette.getPageBg());

        kpiGrid.add(createKpiCard(
                "Latest Price",
                CurrencyConverter.format(active.getLatestPrice(), currency),
                active.getPointCount() + " recorded points",
                palette.getTextPrimary()
        ));

        kpiGrid.add(createKpiCard(
                "SMA (" + smaPeriod + ")",
                ind.isSmaValid() ? CurrencyConverter.format(ind.getSmaValue(), currency) : "Not enough data",
                ind.isSmaValid() ? "Mean of last " + smaPeriod + " prices" : "Needs >= " + smaPeriod + " points",
                ind.isSmaValid() ? palette.getAccentViolet() : palette.getFlatAmber()
        ));

        kpiGrid.add(createKpiCard(
                "EMA (" + emaPeriod + ")",
                ind.isEmaValid() ? CurrencyConverter.format(ind.getEmaValue(), currency) : "Not enough data",
                ind.isEmaValid() ? String.format("Weight k=%.3f", 2.0 / (emaPeriod + 1.0)) : "Needs >= " + emaPeriod + " points",
                ind.isEmaValid() ? palette.getCyanSecondary() : palette.getFlatAmber()
        ));

        Color momColor = !ind.isMomentumValid() ? palette.getFlatAmber()
                : (ind.getMomentumValue() > 0 ? palette.getGainGreen()
                : (ind.getMomentumValue() < 0 ? palette.getLossRed() : palette.getFlatAmber()));
        kpiGrid.add(createKpiCard(
                "Momentum (" + momentumPeriod + ")",
                ind.isMomentumValid() ? String.format("%+.2f", ind.getMomentumValue()) : "Not enough data",
                ind.isMomentumValid() ? "Delta vs P" + (active.getPointCount() - momentumPeriod) : "Needs > " + momentumPeriod + " points",
                momColor
        ));

        String rsiStatus = !ind.isRsiValid() ? "Needs >= " + (rsiPeriod + 1) + " points"
                : (ind.getRsiValue() >= 70 ? "Overbought (>=70)"
                : (ind.getRsiValue() <= 30 ? "Oversold (<=30)" : "Neutral zone (30-70)"));
        Color rsiColor = !ind.isRsiValid() ? palette.getFlatAmber()
                : (ind.getRsiValue() >= 70 ? palette.getLossRed()
                : (ind.getRsiValue() <= 30 ? palette.getGainGreen() : palette.getAccentViolet()));
        kpiGrid.add(createKpiCard(
                "RSI (" + rsiPeriod + ")",
                ind.isRsiValid() ? String.format("%.2f", ind.getRsiValue()) : "Not enough data",
                rsiStatus,
                rsiColor
        ));

        Color macdColor = !ind.isMacdValid() ? palette.getFlatAmber()
                : (ind.getMacdHistogram() >= 0 ? palette.getGainGreen() : palette.getLossRed());
        kpiGrid.add(createKpiCard(
                "MACD (" + macdFast + "/" + macdSlow + "/" + macdSignal + ")",
                ind.isMacdValid() ? String.format("%+.2f", ind.getMacdLine()) : "Not enough data",
                ind.isMacdValid()
                        ? String.format("Sig: %+.2f | Hist: %+.2f", ind.getMacdSignal(), ind.getMacdHistogram())
                        : "Needs >= " + macdSlow + " pts (use Short preset)",
                macdColor
        ));

        Panel centerStack = new Panel(new BorderLayout(0, 12));
        centerStack.setBackground(palette.getPageBg());
        centerStack.add(kpiGrid, BorderLayout.NORTH);

        // Charts Row: Overlay Chart + RSI/MACD Charts
        Panel chartsPanel = new Panel(new GridLayout(1, 2, 12, 12));
        chartsPanel.setBackground(palette.getPageBg());

        AwtCardPanel overlayCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        overlayCard.applyTheme(palette);
        Label ovTitle = new Label("Price with SMA(" + smaPeriod + ") & EMA(" + emaPeriod + ") Overlay");
        ovTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        ovTitle.setForeground(palette.getTextPrimary());
        ovTitle.setBackground(palette.getCardBg());

        StockChart overlayChart = new StockChart(StockChart.ChartMode.INDICATORS_OVERLAY, 360, 235);
        overlayChart.applyTheme(palette);
        overlayChart.setCurrency(currency);
        overlayChart.setIndicatorsOverlayData(
                active.getHistory(),
                indicatorService.calculateSMASeries(active.getHistory(), smaPeriod),
                indicatorService.calculateEMASeries(active.getHistory(), emaPeriod),
                "SMA(" + smaPeriod + ")",
                "EMA(" + emaPeriod + ")"
        );
        overlayCard.add(ovTitle, BorderLayout.NORTH);
        overlayCard.add(overlayChart, BorderLayout.CENTER);

        AwtCardPanel oscCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        oscCard.applyTheme(palette);
        boolean showMacdChart = ind.isMacdValid();
        Label oscTitle = new Label(showMacdChart
                ? "MACD (" + macdFast + "/" + macdSlow + "/" + macdSignal + ") & Histogram"
                : "RSI (" + rsiPeriod + ") Oscillator (70/30 Bands)");
        oscTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        oscTitle.setForeground(palette.getTextPrimary());
        oscTitle.setBackground(palette.getCardBg());

        StockChart oscChart;
        if (showMacdChart) {
            oscChart = new StockChart(StockChart.ChartMode.MACD_CHART, 360, 235);
            oscChart.applyTheme(palette);
            List<Double> macdSeries = indicatorService.calculateMACDLineSeries(active.getHistory(), macdFast, macdSlow);
            List<Double> sigSeries = indicatorService.calculateMACDSignalSeries(macdSeries, macdSignal);
            oscChart.setMacdData(macdSeries, sigSeries);
        } else {
            oscChart = new StockChart(StockChart.ChartMode.RSI_CHART, 360, 235);
            oscChart.applyTheme(palette);
            oscChart.setRsiData(indicatorService.calculateRSISeries(active.getHistory(), rsiPeriod), rsiPeriod);
        }
        oscCard.add(oscTitle, BorderLayout.NORTH);
        oscCard.add(oscChart, BorderLayout.CENTER);

        chartsPanel.add(overlayCard);
        chartsPanel.add(oscCard);

        centerStack.add(chartsPanel, BorderLayout.CENTER);
        body.add(centerStack, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        validate();
        repaint();
    }

    private Label makeParamLabel(String txt) {
        Label l = new Label(txt);
        l.setFont(ThemePalette.FONT_LABEL);
        l.setForeground(palette.getTextSecondary());
        l.setBackground(palette.getCardBg());
        return l;
    }

    private void applyParameters() {
        try {
            int newSma = ValidationUtil.parsePositiveInt(smaTf.getText(), "SMA Period", 1, 200);
            int newEma = ValidationUtil.parsePositiveInt(emaTf.getText(), "EMA Period", 1, 200);
            int newMom = ValidationUtil.parsePositiveInt(momTf.getText(), "Momentum Period", 1, 200);
            int newRsi = ValidationUtil.parsePositiveInt(rsiTf.getText(), "RSI Period", 1, 200);
            int newFast = ValidationUtil.parsePositiveInt(macdFastTf.getText(), "MACD Fast", 1, 100);
            int newSlow = ValidationUtil.parsePositiveInt(macdSlowTf.getText(), "MACD Slow", newFast + 1, 200);
            int newSig = ValidationUtil.parsePositiveInt(macdSigTf.getText(), "MACD Signal", 1, 100);

            this.smaPeriod = newSma;
            this.emaPeriod = newEma;
            this.momentumPeriod = newMom;
            this.rsiPeriod = newRsi;
            this.macdFast = newFast;
            this.macdSlow = newSlow;
            this.macdSignal = newSig;
            refreshData();
        } catch (ValidationException ex) {
            showErrorDialog("Invalid Indicator Parameter", ex.getMessage());
        }
    }
}
