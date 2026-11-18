package ui;

import util.ThemePalette;

import java.awt.*;

/**
 * Reusable bordered card Panel with configurable inner padding and custom 1px border rendering.
 */
public class AwtCardPanel extends Panel implements ThemeAware {

    private Color borderColor;
    private int padTop = 14;
    private int padLeft = 16;
    private int padBottom = 14;
    private int padRight = 16;

    public AwtCardPanel(LayoutManager layout) {
        super(layout);
        this.borderColor = ThemePalette.DARK.getBorder();
    }

    public AwtCardPanel(LayoutManager layout, int padV, int padH) {
        super(layout);
        this.padTop = padV;
        this.padBottom = padV;
        this.padLeft = padH;
        this.padRight = padH;
        this.borderColor = ThemePalette.DARK.getBorder();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setPadding(int top, int left, int bottom, int right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
    }

    @Override
    public Insets getInsets() {
        return new Insets(padTop, padLeft, padBottom, padRight);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(borderColor != null ? borderColor : ThemePalette.DARK.getBorder());
        g2.drawRoundRect(0, 0, Math.max(1, getWidth() - 1), Math.max(1, getHeight() - 1), 12, 12);
        g2.dispose();
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        setBackground(palette.getCardBg());
        this.borderColor = palette.getBorder();
        repaint();
    }
}
