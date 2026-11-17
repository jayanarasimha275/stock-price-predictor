package ui;

import model.Currency;
import util.ChartRenderer;
import util.CurrencyConverter;
import util.ThemePalette;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Versatile custom AWT Canvas using Graphics2D to render:
 * 1. Sparklines (in StockCards)
 * 2. Price History Curve with Violet Gradient Fill
 * 3. Prediction Trajectory (Solid Violet History + Dashed Amber Forecast)
 * 4. Technical Indicators Overlay (Price + SMA + EMA)
 * 5. RSI Oscillator (0-100 with 70/30 overbought/oversold guides)
 * 6. MACD Histogram & Signal Chart
 * 7. Accuracy Backtesting (Training Curve + Cyan Actual Dot + Amber Predicted Dot)
 * 8. Normalized Base-100 Comparison Chart (Violet Stock A vs Cyan Stock B)
 */
public class StockChart extends BaseChartCanvas {

    public enum ChartMode {
        SPARKLINE,
        HISTORY,
        PREDICTION,
        INDICATORS_OVERLAY,
        RSI_CHART,
        MACD_CHART,
        ACCURACY_BACKTEST,
        COMPARE_NORMALIZED
    }

    private ChartMode mode = ChartMode.HISTORY;
    private Currency currency = Currency.INR;

    private List<Double> primarySeries = new ArrayList<>();
    private List<Double> secondarySeries = new ArrayList<>();
    private List<Double> tertiarySeries = new ArrayList<>();

    private String labelA = "Series A";
    private String labelB = "Series B";
    private String labelC = "Series C";

    private double predictedValue = 0.0;
    private double actualBacktestValue = 0.0;
    private boolean sparkUp = false;
    private boolean sparkDown = false;
    private int prefWidth = 520;
    private int prefHeight = 250;

    public StockChart(ChartMode mode) {
        this.mode = mode;
        if (mode == ChartMode.SPARKLINE) {
            this.prefWidth = 240;
            this.prefHeight = 46;
        }
    }

    public StockChart(ChartMode mode, int prefWidth, int prefHeight) {
        this.mode = mode;
        this.prefWidth = prefWidth;
        this.prefHeight = prefHeight;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(prefWidth, prefHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(Math.min(180, prefWidth), Math.min(40, prefHeight));
    }

    public void setCurrency(Currency currency) {
        this.currency = currency != null ? currency : Currency.INR;
        repaint();
    }

    public void setSparklineData(List<Double> prices, boolean isUp, boolean isDown) {
        this.mode = ChartMode.SPARKLINE;
        this.primarySeries = prices != null ? new ArrayList<>(prices) : new ArrayList<>();
        this.sparkUp = isUp;
        this.sparkDown = isDown;
        repaint();
    }

    public void setHistoryData(List<Double> prices) {
        this.mode = ChartMode.HISTORY;
        this.primarySeries = prices != null ? new ArrayList<>(prices) : new ArrayList<>();
        repaint();
    }

    public void setPredictionData(List<Double> history, double predictedPrice) {
        this.mode = ChartMode.PREDICTION;
        this.primarySeries = history != null ? new ArrayList<>(history) : new ArrayList<>();
        this.predictedValue = predictedPrice;
        repaint();
    }

    public void setIndicatorsOverlayData(List<Double> prices, List<Double> smaSeries, List<Double> emaSeries,
                                         String smaLabel, String emaLabel) {
        this.mode = ChartMode.INDICATORS_OVERLAY;
        this.primarySeries = prices != null ? new ArrayList<>(prices) : new ArrayList<>();
        this.secondarySeries = smaSeries != null ? new ArrayList<>(smaSeries) : new ArrayList<>();
        this.tertiarySeries = emaSeries != null ? new ArrayList<>(emaSeries) : new ArrayList<>();
        this.labelA = "Price";
        this.labelB = smaLabel;
        this.labelC = emaLabel;
        repaint();
    }

    public void setRsiData(List<Double> rsiSeries, int period) {
        this.mode = ChartMode.RSI_CHART;
        this.primarySeries = rsiSeries != null ? new ArrayList<>(rsiSeries) : new ArrayList<>();
        this.labelA = "RSI (" + period + ")";
        repaint();
    }

    public void setMacdData(List<Double> macdLine, List<Double> signalLine) {
        this.mode = ChartMode.MACD_CHART;
        this.primarySeries = macdLine != null ? new ArrayList<>(macdLine) : new ArrayList<>();
        this.secondarySeries = signalLine != null ? new ArrayList<>(signalLine) : new ArrayList<>();
        this.labelA = "MACD Line";
        this.labelB = "Signal Line";
        repaint();
    }

    public void setAccuracyData(List<Double> trainData, double actual, double predicted) {
        this.mode = ChartMode.ACCURACY_BACKTEST;
        this.primarySeries = trainData != null ? new ArrayList<>(trainData) : new ArrayList<>();
        this.actualBacktestValue = actual;
        this.predictedValue = predicted;
        repaint();
    }

    public void setComparisonData(List<Double> normA, List<Double> normB, String nameA, String nameB) {
        this.mode = ChartMode.COMPARE_NORMALIZED;
        this.primarySeries = normA != null ? new ArrayList<>(normA) : new ArrayList<>();
        this.secondarySeries = normB != null ? new ArrayList<>(normB) : new ArrayList<>();
        this.labelA = nameA;
        this.labelB = nameB;
        repaint();
    }

    @Override
    protected void drawChartContent(Graphics2D g2, int width, int height) {
        if (mode == ChartMode.SPARKLINE) {
            drawSparkline(g2, width, height);
            return;
        }

        if (primarySeries == null || primarySeries.isEmpty()) {
            g2.setFont(ThemePalette.FONT_BODY);
            g2.setColor(palette.getTextSecondary());
            String msg = "No chart data available.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2);
            return;
        }

        int leftPad = 68;
        int rightPad = 24;
        int topPad = 28;
        int bottomPad = 34;
        int plotW = Math.max(40, width - leftPad - rightPad);
        int plotH = Math.max(40, height - topPad - bottomPad);

        switch (mode) {
            case HISTORY -> drawHistoryChart(g2, leftPad, topPad, plotW, plotH, width);
            case PREDICTION -> drawPredictionChart(g2, leftPad, topPad, plotW, plotH, width);
            case INDICATORS_OVERLAY -> drawOverlayChart(g2, leftPad, topPad, plotW, plotH, width);
            case RSI_CHART -> drawRsiChart(g2, leftPad, topPad, plotW, plotH, width);
            case MACD_CHART -> drawMacdChart(g2, leftPad, topPad, plotW, plotH, width);
            case ACCURACY_BACKTEST -> drawAccuracyChart(g2, leftPad, topPad, plotW, plotH, width);
            case COMPARE_NORMALIZED -> drawComparisonChart(g2, leftPad, topPad, plotW, plotH, width);
            default -> {
            }
        }
    }

    private void drawSparkline(Graphics2D g2, int width, int height) {
        if (primarySeries.size() < 2) {
            return;
        }
        double min = Collections.min(primarySeries);
        double max = Collections.max(primarySeries);
        if (Math.abs(max - min) < 1e-9) {
            min -= 1;
            max += 1;
        }
        Color strokeColor = sparkUp ? palette.getGainGreen()
                : (sparkDown ? palette.getLossRed() : palette.getFlatAmber());
        int pad = 5;
        ChartRenderer.drawLineSeries(g2, primarySeries, 0, primarySeries.size(), min, max,
                pad, pad, Math.max(10, width - pad * 2), Math.max(10, height - pad * 2),
                strokeColor, 2.0f, false, false, null);
    }

    private void drawHistoryChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        double[] bounds = computeMinMax(primarySeries);
        String[] labels = buildStepLabels(primarySeries.size(), false);
        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                primarySeries.size(), labels, palette, currency.getSymbol());

        ChartRenderer.drawAreaGradient(g2, primarySeries, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph, palette.getAccentViolet());
        ChartRenderer.drawLineSeries(g2, primarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.4f, false, true, palette.getCardBg());

        drawHoverTooltip(g2, px, py, pw, ph, totalW, primarySeries.size(), idx -> {
            double val = primarySeries.get(idx);
            return new String[]{"Step P" + (idx + 1), CurrencyConverter.format(val, currency)};
        });
    }

    private void drawPredictionChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        List<Double> combined = new ArrayList<>(primarySeries);
        combined.add(predictedValue);
        double[] bounds = computeMinMax(combined);
        int totalPoints = combined.size();
        String[] labels = buildStepLabels(primarySeries.size(), true);

        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                totalPoints, labels, palette, currency.getSymbol());

        // Area fill under history portion
        ChartRenderer.drawAreaGradient(g2, primarySeries, totalPoints,
                bounds[0], bounds[1], px, py, pw, ph, palette.getAccentViolet());

        // Solid violet history line
        ChartRenderer.drawLineSeries(g2, primarySeries, 0, totalPoints,
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.5f, false, true, palette.getCardBg());

        // Dashed amber forecast segment from last point to predicted point
        List<Double> forecastSegment = new ArrayList<>();
        forecastSegment.add(primarySeries.get(primarySeries.size() - 1));
        forecastSegment.add(predictedValue);
        ChartRenderer.drawLineSeries(g2, forecastSegment, primarySeries.size() - 1, totalPoints,
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getFlatAmber(), 2.5f, true, false, null);

        // Highlight predicted amber dot
        int predX = ChartRenderer.mapX(totalPoints - 1, totalPoints, px, pw);
        int predY = ChartRenderer.mapY(predictedValue, bounds[0], bounds[1], py, ph);
        g2.setColor(palette.getFlatAmber());
        g2.fillOval(predX - 6, predY - 6, 12, 12);
        g2.setColor(palette.getCardBg());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(predX - 6, predY - 6, 12, 12);

        drawLegend(g2, px, 16,
                new String[]{"History", "Predicted P" + totalPoints},
                new Color[]{palette.getAccentViolet(), palette.getFlatAmber()});

        drawHoverTooltip(g2, px, py, pw, ph, totalW, totalPoints, idx -> {
            if (idx < primarySeries.size()) {
                return new String[]{"P" + (idx + 1) + " (Actual)", CurrencyConverter.format(primarySeries.get(idx), currency)};
            }
            return new String[]{"P" + totalPoints + " (Forecast)", CurrencyConverter.format(predictedValue, currency)};
        });
    }

    private void drawOverlayChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        double[] bounds = computeMinMax(primarySeries, secondarySeries, tertiarySeries);
        String[] labels = buildStepLabels(primarySeries.size(), false);
        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                primarySeries.size(), labels, palette, currency.getSymbol());

        ChartRenderer.drawLineSeries(g2, primarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getTextMuted(), 1.8f, false, true, palette.getCardBg());
        ChartRenderer.drawLineSeries(g2, secondarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.4f, false, false, null);
        ChartRenderer.drawLineSeries(g2, tertiarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getCyanSecondary(), 2.4f, false, false, null);

        drawLegend(g2, px, 16,
                new String[]{labelA, labelB, labelC},
                new Color[]{palette.getTextMuted(), palette.getAccentViolet(), palette.getCyanSecondary()});

        drawHoverTooltip(g2, px, py, pw, ph, totalW, primarySeries.size(), idx -> {
            StringBuilder line = new StringBuilder(CurrencyConverter.format(primarySeries.get(idx), currency));
            if (idx < secondarySeries.size() && secondarySeries.get(idx) != null) {
                line.append(" | ").append(labelB).append(": ").append(CurrencyConverter.format(secondarySeries.get(idx), currency));
            }
            if (idx < tertiarySeries.size() && tertiarySeries.get(idx) != null) {
                line.append(" | ").append(labelC).append(": ").append(CurrencyConverter.format(tertiarySeries.get(idx), currency));
            }
            return new String[]{"Step P" + (idx + 1), line.toString()};
        });
    }

    private void drawRsiChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        String[] labels = buildStepLabels(primarySeries.size(), false);
        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, 0.0, 100.0,
                primarySeries.size(), labels, palette, "");

        // 70 (Overbought) & 30 (Oversold) guide lines
        Stroke guideStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0);
        g2.setStroke(guideStroke);
        int y70 = ChartRenderer.mapY(70.0, 0.0, 100.0, py, ph);
        int y30 = ChartRenderer.mapY(30.0, 0.0, 100.0, py, ph);
        g2.setColor(palette.getLossRed());
        g2.drawLine(px, y70, px + pw, y70);
        g2.setColor(palette.getGainGreen());
        g2.drawLine(px, y30, px + pw, y30);

        ChartRenderer.drawLineSeries(g2, primarySeries, 0, primarySeries.size(),
                0.0, 100.0, px, py, pw, ph,
                palette.getAccentViolet(), 2.3f, false, true, palette.getCardBg());

        drawLegend(g2, px, 16,
                new String[]{labelA, "Overbought (70)", "Oversold (30)"},
                new Color[]{palette.getAccentViolet(), palette.getLossRed(), palette.getGainGreen()});

        drawHoverTooltip(g2, px, py, pw, ph, totalW, primarySeries.size(), idx -> {
            Double v = primarySeries.get(idx);
            return new String[]{"Step P" + (idx + 1), v != null ? String.format("RSI: %.2f", v) : "Insufficient points"};
        });
    }

    private void drawMacdChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        double[] bounds = computeMinMax(primarySeries, secondarySeries);
        // Ensure 0 is included in range
        if (bounds[0] > 0) bounds[0] = -Math.max(1.0, bounds[1] * 0.15);
        if (bounds[1] < 0) bounds[1] = Math.max(1.0, Math.abs(bounds[0]) * 0.15);

        String[] labels = buildStepLabels(primarySeries.size(), false);
        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                primarySeries.size(), labels, palette, "");

        int yZero = ChartRenderer.mapY(0.0, bounds[0], bounds[1], py, ph);
        g2.setColor(palette.getTextMuted());
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(px, yZero, px + pw, yZero);

        // Draw histogram bars where both MACD and signal exist
        int barW = Math.max(4, Math.min(16, pw / Math.max(2, primarySeries.size() * 2)));
        for (int i = 0; i < primarySeries.size(); i++) {
            Double m = primarySeries.get(i);
            Double s = i < secondarySeries.size() ? secondarySeries.get(i) : null;
            if (m != null && s != null) {
                double hist = m - s;
                int cx = ChartRenderer.mapX(i, primarySeries.size(), px, pw);
                int yVal = ChartRenderer.mapY(hist, bounds[0], bounds[1], py, ph);
                g2.setColor(hist >= 0 ? palette.getGainGreen() : palette.getLossRed());
                int top = Math.min(yZero, yVal);
                int bh = Math.max(2, Math.abs(yVal - yZero));
                g2.fillRect(cx - barW / 2, top, barW, bh);
            }
        }

        ChartRenderer.drawLineSeries(g2, primarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.2f, false, false, null);
        ChartRenderer.drawLineSeries(g2, secondarySeries, 0, primarySeries.size(),
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getFlatAmber(), 2.0f, false, false, null);

        drawLegend(g2, px, 16,
                new String[]{labelA, labelB, "Histogram"},
                new Color[]{palette.getAccentViolet(), palette.getFlatAmber(), palette.getGainGreen()});
    }

    private void drawAccuracyChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        List<Double> all = new ArrayList<>(primarySeries);
        all.add(actualBacktestValue);
        all.add(predictedValue);
        double[] bounds = computeMinMax(all);
        int totalPoints = primarySeries.size() + 1;

        String[] labels = new String[totalPoints];
        for (int i = 0; i < primarySeries.size(); i++) {
            labels[i] = "P" + (i + 1);
        }
        labels[totalPoints - 1] = "P" + totalPoints + " (Test)";

        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                totalPoints, labels, palette, currency.getSymbol());

        ChartRenderer.drawAreaGradient(g2, primarySeries, totalPoints,
                bounds[0], bounds[1], px, py, pw, ph, palette.getAccentViolet());
        ChartRenderer.drawLineSeries(g2, primarySeries, 0, totalPoints,
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.3f, false, true, palette.getCardBg());

        // Dashed connectors from last training point to Actual (Cyan) and Predicted (Amber)
        double lastTrain = primarySeries.get(primarySeries.size() - 1);
        int lastX = ChartRenderer.mapX(primarySeries.size() - 1, totalPoints, px, pw);
        int lastY = ChartRenderer.mapY(lastTrain, bounds[0], bounds[1], py, ph);
        int testX = ChartRenderer.mapX(totalPoints - 1, totalPoints, px, pw);
        int actY = ChartRenderer.mapY(actualBacktestValue, bounds[0], bounds[1], py, ph);
        int predY = ChartRenderer.mapY(predictedValue, bounds[0], bounds[1], py, ph);

        Stroke dashed = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4f, 4f}, 0);
        g2.setStroke(dashed);
        g2.setColor(palette.getCyanSecondary());
        g2.drawLine(lastX, lastY, testX, actY);
        g2.setColor(palette.getFlatAmber());
        g2.drawLine(lastX, lastY, testX, predY);

        // Actual dot (Cyan)
        g2.setColor(palette.getCyanSecondary());
        g2.fillOval(testX - 6, actY - 6, 12, 12);
        // Predicted dot (Amber)
        g2.setColor(palette.getFlatAmber());
        g2.fillOval(testX - 6, predY - 6, 12, 12);

        drawLegend(g2, px, 16,
                new String[]{"Training History", "Actual P" + totalPoints, "Predicted P" + totalPoints},
                new Color[]{palette.getAccentViolet(), palette.getCyanSecondary(), palette.getFlatAmber()});
    }

    private void drawComparisonChart(Graphics2D g2, int px, int py, int pw, int ph, int totalW) {
        double[] bounds = computeMinMax(primarySeries, secondarySeries);
        int maxLen = Math.max(primarySeries.size(), secondarySeries.size());
        String[] labels = buildStepLabels(maxLen, false);

        ChartRenderer.drawGridAndAxes(g2, px, py, pw, ph, bounds[0], bounds[1],
                maxLen, labels, palette, "");

        // Base 100 reference line
        if (100.0 >= bounds[0] && 100.0 <= bounds[1]) {
            int y100 = ChartRenderer.mapY(100.0, bounds[0], bounds[1], py, ph);
            g2.setColor(palette.getTextMuted());
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
            g2.drawLine(px, y100, px + pw, y100);
        }

        ChartRenderer.drawLineSeries(g2, primarySeries, 0, maxLen,
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getAccentViolet(), 2.5f, false, true, palette.getCardBg());
        ChartRenderer.drawLineSeries(g2, secondarySeries, 0, maxLen,
                bounds[0], bounds[1], px, py, pw, ph,
                palette.getCyanSecondary(), 2.5f, false, true, palette.getCardBg());

        drawLegend(g2, px, 16,
                new String[]{labelA + " (Base 100)", labelB + " (Base 100)"},
                new Color[]{palette.getAccentViolet(), palette.getCyanSecondary()});

        drawHoverTooltip(g2, px, py, pw, ph, totalW, maxLen, idx -> {
            String aVal = idx < primarySeries.size() ? String.format("%.2f", primarySeries.get(idx)) : "N/A";
            String bVal = idx < secondarySeries.size() ? String.format("%.2f", secondarySeries.get(idx)) : "N/A";
            return new String[]{"Step P" + (idx + 1), labelA + ": " + aVal + " | " + labelB + ": " + bVal};
        });
    }

    private void drawLegend(Graphics2D g2, int startX, int y, String[] names, Color[] colors) {
        g2.setFont(ThemePalette.FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int curX = startX;
        for (int i = 0; i < names.length; i++) {
            g2.setColor(colors[i]);
            g2.fillRoundRect(curX, y - 7, 10, 6, 3, 3);
            curX += 14;
            g2.setColor(palette.getTextSecondary());
            g2.drawString(names[i], curX, y);
            curX += fm.stringWidth(names[i]) + 16;
        }
    }

    private interface TooltipTextProvider {
        String[] getLines(int index);
    }

    private void drawHoverTooltip(Graphics2D g2, int px, int py, int pw, int ph, int totalW,
                                  int totalPoints, TooltipTextProvider provider) {
        if (hoverX < px || hoverX > px + pw || hoverY < py || hoverY > py + ph || totalPoints <= 0) {
            return;
        }
        int idx = totalPoints == 1 ? 0 : (int) Math.round(((double) (hoverX - px) / pw) * (totalPoints - 1));
        idx = Math.max(0, Math.min(totalPoints - 1, idx));
        int snapX = ChartRenderer.mapX(idx, totalPoints, px, pw);

        g2.setColor(palette.getBorder());
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3f, 3f}, 0));
        g2.drawLine(snapX, py, snapX, py + ph);

        String[] lines = provider.getLines(idx);
        g2.setFont(ThemePalette.FONT_MONO_SMALL);
        FontMetrics fm = g2.getFontMetrics();
        int boxW = Math.max(fm.stringWidth(lines[0]), fm.stringWidth(lines[1])) + 16;
        int boxH = 38;
        int boxX = snapX + 10;
        if (boxX + boxW > totalW - 8) {
            boxX = snapX - boxW - 10;
        }
        int boxY = Math.max(py, Math.min(py + ph - boxH, hoverY - boxH / 2));

        g2.setColor(palette.getChromeBg());
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        g2.setColor(palette.getAccentViolet());
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);

        g2.setColor(palette.getTextSecondary());
        g2.drawString(lines[0], boxX + 8, boxY + 15);
        g2.setColor(palette.getTextPrimary());
        g2.drawString(lines[1], boxX + 8, boxY + 30);
    }

    @SafeVarargs
    private double[] computeMinMax(List<Double>... seriesList) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (List<Double> list : seriesList) {
            if (list == null) continue;
            for (Double d : list) {
                if (d != null && Double.isFinite(d)) {
                    if (d < min) min = d;
                    if (d > max) max = d;
                }
            }
        }
        if (min == Double.MAX_VALUE) {
            return new double[]{0.0, 100.0};
        }
        double span = max - min;
        if (span < 1e-6) {
            double pad = Math.max(1.0, Math.abs(max) * 0.05);
            return new double[]{min - pad, max + pad};
        }
        double pad = span * 0.12;
        return new double[]{min - pad, max + pad};
    }

    private String[] buildStepLabels(int count, boolean includePred) {
        int len = count + (includePred ? 1 : 0);
        String[] labels = new String[len];
        for (int i = 0; i < count; i++) {
            labels[i] = "P" + (i + 1);
        }
        if (includePred) {
            labels[count] = "P" + (count + 1) + " (Pred)";
        }
        return labels;
    }
}
