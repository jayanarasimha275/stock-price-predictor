package util;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.List;

/**
 * Shared Graphics2D rendering primitives for anti-aliased financial charts,
 * gradient area fills, dashed prediction lines, grid lines, and badges.
 */
public final class ChartRenderer {

    private ChartRenderer() {
    }

    public static void enableQualityHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void drawGridAndAxes(Graphics2D g2, int x, int y, int w, int h,
                                       double minVal, double maxVal, int pointCount,
                                       String[] xLabels, ThemePalette palette, String currencySymbol) {
        int horizontalLines = 4;
        Stroke dashedGrid = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{4f, 4f}, 0);
        Color gridColor = new Color(palette.getTextMuted().getRed(), palette.getTextMuted().getGreen(),
                palette.getTextMuted().getBlue(), 45);

        g2.setFont(ThemePalette.FONT_MONO_SMALL);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i <= horizontalLines; i++) {
            double ratio = (double) i / horizontalLines;
            int py = y + h - (int) Math.round(ratio * h);

            g2.setColor(gridColor);
            g2.setStroke(dashedGrid);
            g2.drawLine(x, py, x + w, py);

            double val = minVal + ratio * (maxVal - minVal);
            String label = currencySymbol + String.format("%.1f", val);
            g2.setColor(palette.getTextSecondary());
            int lw = fm.stringWidth(label);
            g2.drawString(label, x - lw - 8, py + fm.getAscent() / 2 - 1);
        }

        // Baseline axes
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(palette.getBorder());
        g2.drawLine(x, y + h, x + w, y + h);
        g2.drawLine(x, y, x, y + h);

        // X-axis tick labels
        if (xLabels != null && xLabels.length > 0) {
            int maxTicks = Math.min(xLabels.length, 8);
            int step = Math.max(1, (int) Math.ceil((double) xLabels.length / maxTicks));
            g2.setColor(palette.getTextSecondary());
            for (int i = 0; i < xLabels.length; i++) {
                if (i % step == 0 || i == xLabels.length - 1) {
                    int px = mapX(i, Math.max(2, pointCount), x, w);
                    String lbl = xLabels[i];
                    int lw = fm.stringWidth(lbl);
                    int drawX = Math.max(x - 10, Math.min(x + w - lw + 10, px - lw / 2));
                    g2.drawString(lbl, drawX, y + h + fm.getAscent() + 6);
                }
            }
        }
    }

    public static void drawAreaGradient(Graphics2D g2, List<Double> values, int totalPoints,
                                        double minVal, double maxVal, int plotX, int plotY,
                                        int plotW, int plotH, Color baseColor) {
        if (values == null || values.size() < 2) {
            return;
        }
        GeneralPath path = new GeneralPath();
        int firstX = mapX(0, totalPoints, plotX, plotW);
        int firstY = mapY(values.get(0), minVal, maxVal, plotY, plotH);
        path.moveTo(firstX, plotY + plotH);
        path.lineTo(firstX, firstY);

        int lastX = firstX;
        for (int i = 1; i < values.size(); i++) {
            lastX = mapX(i, totalPoints, plotX, plotW);
            int py = mapY(values.get(i), minVal, maxVal, plotY, plotH);
            path.lineTo(lastX, py);
        }
        path.lineTo(lastX, plotY + plotH);
        path.closePath();

        GradientPaint gp = new GradientPaint(
                0, plotY, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 65),
                0, plotY + plotH, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0)
        );
        g2.setPaint(gp);
        g2.fill(path);
    }

    public static void drawLineSeries(Graphics2D g2, List<Double> values, int startIndex, int totalPoints,
                                      double minVal, double maxVal, int plotX, int plotY, int plotW, int plotH,
                                      Color color, float width, boolean dashed, boolean drawDots, Color dotBorder) {
        if (values == null || values.isEmpty()) {
            return;
        }

        Stroke stroke = dashed
                ? new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6f, 5f}, 0)
                : new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
        g2.setColor(color);

        int prevX = -1;
        int prevY = -1;
        for (int i = 0; i < values.size(); i++) {
            Double v = values.get(i);
            if (v == null || Double.isNaN(v)) {
                prevX = -1;
                prevY = -1;
                continue;
            }
            int px = mapX(startIndex + i, totalPoints, plotX, plotW);
            int py = mapY(v, minVal, maxVal, plotY, plotH);
            if (prevX != -1) {
                g2.drawLine(prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }

        if (drawDots) {
            for (int i = 0; i < values.size(); i++) {
                Double v = values.get(i);
                if (v == null || Double.isNaN(v)) {
                    continue;
                }
                int px = mapX(startIndex + i, totalPoints, plotX, plotW);
                int py = mapY(v, minVal, maxVal, plotY, plotH);
                int r = 4;
                g2.setColor(color);
                g2.fillOval(px - r, py - r, r * 2, r * 2);
                if (dotBorder != null) {
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.setColor(dotBorder);
                    g2.drawOval(px - r, py - r, r * 2, r * 2);
                }
            }
        }
    }

    public static int mapX(int idx, int totalPoints, int plotX, int plotW) {
        if (totalPoints <= 1) {
            return plotX + plotW / 2;
        }
        return plotX + (int) Math.round(((double) idx / (totalPoints - 1)) * plotW);
    }

    public static int mapY(double value, double minVal, double maxVal, int plotY, int plotH) {
        double range = maxVal - minVal;
        if (range <= 1e-9) {
            return plotY + plotH / 2;
        }
        double norm = (value - minVal) / range;
        return plotY + plotH - (int) Math.round(norm * plotH);
    }
}
