package ui;

import model.Currency;
import model.Prediction;
import model.PriceChangeSummary;
import model.Stock;
import model.TrendDirection;
import util.CurrencyConverter;
import util.ThemePalette;

import java.awt.*;

/**
 * Individual Stock Card rendered inside All Stocks Overview.
 * Shows ticker, symbol, trend badge, latest & predicted price, sparkline Canvas, and quick navigation.
 */
public class StockCard extends AwtCardPanel {

    private final StockChart sparkline;

    public StockCard(Stock stock, Prediction prediction, PriceChangeSummary change,
                     Currency currency, ThemePalette palette, StockSelectionListener navigator) {
        super(new BorderLayout(0, 8), 14, 16);
        applyTheme(palette);

        TrendDirection trend = prediction.getTrend();
        boolean isUp = trend == TrendDirection.UPWARD;
        boolean isDown = trend == TrendDirection.DOWNWARD;
        Color trendColor = isUp ? palette.getGainGreen() : (isDown ? palette.getLossRed() : palette.getFlatAmber());
        Color trendBg = isUp ? palette.getGainBg() : (isDown ? palette.getLossBg() : palette.getAmberBg());

        // Top: Stock Name/Symbol + Trend Badge
        Panel topRow = new Panel(new BorderLayout(8, 2));
        topRow.setBackground(palette.getCardBg());

        Panel nameStack = new Panel(new GridLayout(2, 1, 0, 1));
        nameStack.setBackground(palette.getCardBg());
        Label nameLbl = new Label(stock.getName() + " [" + stock.getSymbol() + "]");
        nameLbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        nameLbl.setForeground(palette.getTextPrimary());
        nameLbl.setBackground(palette.getCardBg());

        Label ptsLbl = new Label(stock.getPointCount() + " price points recorded");
        ptsLbl.setFont(ThemePalette.FONT_SUBTITLE);
        ptsLbl.setForeground(palette.getTextSecondary());
        ptsLbl.setBackground(palette.getCardBg());
        nameStack.add(nameLbl);
        nameStack.add(ptsLbl);

        Label trendBadge = new Label(" " + trend.getLabel().toUpperCase() + " ", Label.CENTER);
        trendBadge.setFont(ThemePalette.FONT_LABEL);
        trendBadge.setBackground(trendBg);
        trendBadge.setForeground(trendColor);

        topRow.add(nameStack, BorderLayout.CENTER);
        topRow.add(trendBadge, BorderLayout.EAST);
        add(topRow, BorderLayout.NORTH);

        // Center: Prices + Sparkline
        Panel center = new Panel(new BorderLayout(0, 8));
        center.setBackground(palette.getCardBg());

        Panel priceGrid = new Panel(new GridLayout(2, 2, 8, 2));
        priceGrid.setBackground(palette.getCardBg());

        Label latestCap = new Label("LATEST PRICE");
        latestCap.setFont(ThemePalette.FONT_LABEL);
        latestCap.setForeground(palette.getTextSecondary());
        latestCap.setBackground(palette.getCardBg());

        Label predCap = new Label("PREDICTED NEXT", Label.RIGHT);
        predCap.setFont(ThemePalette.FONT_LABEL);
        predCap.setForeground(palette.getTextSecondary());
        predCap.setBackground(palette.getCardBg());

        Label latestVal = new Label(CurrencyConverter.format(prediction.getCurrentPrice(), currency));
        latestVal.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        latestVal.setForeground(palette.getTextPrimary());
        latestVal.setBackground(palette.getCardBg());

        Label predVal = new Label(CurrencyConverter.format(prediction.getPredictedPrice(), currency), Label.RIGHT);
        predVal.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        predVal.setForeground(trendColor);
        predVal.setBackground(palette.getCardBg());

        priceGrid.add(latestCap);
        priceGrid.add(predCap);
        priceGrid.add(latestVal);
        priceGrid.add(predVal);
        center.add(priceGrid, BorderLayout.NORTH);

        Panel sparkWrap = new Panel(new BorderLayout(0, 3));
        sparkWrap.setBackground(palette.getCardBg());
        Panel trajRow = new Panel(new BorderLayout());
        trajRow.setBackground(palette.getCardBg());
        Label trajLbl = new Label("Trajectory (oldest -> latest)");
        trajLbl.setFont(ThemePalette.FONT_SUBTITLE);
        trajLbl.setForeground(palette.getTextSecondary());
        trajLbl.setBackground(palette.getCardBg());

        boolean chgUp = change.getAbsoluteChange() >= 0;
        Label pctLbl = new Label(CurrencyConverter.formatSignedPercent(change.getPercentChange()), Label.RIGHT);
        pctLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        pctLbl.setForeground(chgUp ? palette.getGainGreen() : palette.getLossRed());
        pctLbl.setBackground(palette.getCardBg());

        trajRow.add(trajLbl, BorderLayout.WEST);
        trajRow.add(pctLbl, BorderLayout.EAST);

        this.sparkline = new StockChart(StockChart.ChartMode.SPARKLINE, 240, 44);
        sparkline.applyTheme(palette);
        sparkline.setSparklineData(stock.getHistory(), isUp, isDown);

        sparkWrap.add(trajRow, BorderLayout.NORTH);
        sparkWrap.add(sparkline, BorderLayout.CENTER);
        center.add(sparkWrap, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        // Bottom: Quick Action Buttons
        Panel actions = new Panel(new GridLayout(1, 3, 6, 0));
        actions.setBackground(palette.getCardBg());

        Button predictBtn = new Button("Predict");
        predictBtn.setFont(ThemePalette.FONT_BUTTON);
        predictBtn.setBackground(palette.getAccentVioletSoft());
        predictBtn.setForeground(palette.getAccentViolet());
        predictBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        predictBtn.addActionListener(e -> navigator.onNavigate("predict", stock.getName()));

        Button indBtn = new Button("Indicators");
        indBtn.setFont(ThemePalette.FONT_BUTTON);
        indBtn.setBackground(palette.getHoverBg());
        indBtn.setForeground(palette.getTextPrimary());
        indBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        indBtn.addActionListener(e -> navigator.onNavigate("indicators", stock.getName()));

        Button histBtn = new Button("History");
        histBtn.setFont(ThemePalette.FONT_BUTTON);
        histBtn.setBackground(palette.getHoverBg());
        histBtn.setForeground(palette.getTextPrimary());
        histBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        histBtn.addActionListener(e -> navigator.onNavigate("history", stock.getName()));

        actions.add(predictBtn);
        actions.add(indBtn);
        actions.add(histBtn);
        add(actions, BorderLayout.SOUTH);
    }
}
