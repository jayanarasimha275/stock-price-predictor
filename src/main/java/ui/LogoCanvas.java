package ui;

import util.ChartRenderer;
import util.ThemePalette;

import java.awt.*;

/**
 * Reusable Graphics2D vector logo Canvas for Stock Price Predictor
 * used across LoginFrame, RegisterFrame, HeaderPanel, and AdminDashboard.
 */
public class LogoCanvas extends Canvas implements ThemeAware {

    private final int iconSize;
    private ThemePalette palette = ThemePalette.DARK;
    private Color hostBg;

    public LogoCanvas() {
        this(38);
    }

    public LogoCanvas(int size) {
        this.iconSize = Math.max(28, size);
        this.hostBg = ThemePalette.DARK.getChromeBg();
        setPreferredSize(new Dimension(iconSize, iconSize));
        setSize(iconSize, iconSize);
    }

    public void setHostBackground(Color hostBg) {
        this.hostBg = hostBg;
        setBackground(hostBg);
        repaint();
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        if (this.hostBg == null) {
            this.hostBg = palette.getChromeBg();
        }
        setBackground(this.hostBg);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ChartRenderer.enableQualityHints(g2);

        Color bg = hostBg != null ? hostBg : palette.getChromeBg();
        g2.setColor(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int s = Math.min(getWidth(), getHeight()) - 2;
        int arc = Math.max(8, s / 4);

        g2.setColor(palette.getAccentViolet());
        g2.fillRoundRect(1, 1, s, s, arc, arc);

        double sc = s / 36.0;
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke((float) (2.3 * sc), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int[] xs = {
                (int) Math.round(9 * sc),
                (int) Math.round(16 * sc),
                (int) Math.round(21 * sc),
                (int) Math.round(29 * sc)
        };
        int[] ys = {
                (int) Math.round(25 * sc),
                (int) Math.round(18 * sc),
                (int) Math.round(21 * sc),
                (int) Math.round(12 * sc)
        };
        g2.drawPolyline(xs, ys, 4);
        g2.drawLine((int) Math.round(24 * sc), (int) Math.round(12 * sc),
                (int) Math.round(29 * sc), (int) Math.round(12 * sc));
        g2.drawLine((int) Math.round(29 * sc), (int) Math.round(12 * sc),
                (int) Math.round(29 * sc), (int) Math.round(17 * sc));
        g2.dispose();
    }
}
