package ui;

import util.ChartRenderer;
import util.ThemePalette;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Abstract double-buffered AWT Canvas base class for Graphics2D financial visualizations.
 * Prevents flicker via offscreen Image buffering and provides mouse-hover coordinate tracking.
 */
public abstract class BaseChartCanvas extends Canvas implements ThemeAware {

    protected ThemePalette palette = ThemePalette.DARK;
    protected int hoverX = -1;
    protected int hoverY = -1;

    private Image offscreenBuffer;
    private int bufferW = -1;
    private int bufferH = -1;

    protected BaseChartCanvas() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverX = e.getX();
                hoverY = e.getY();
                repaint();
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverX = -1;
                hoverY = -1;
                repaint();
            }
        });
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        setBackground(palette.getCardBg());
        repaint();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (offscreenBuffer == null || bufferW != w || bufferH != h) {
            offscreenBuffer = createImage(w, h);
            bufferW = w;
            bufferH = h;
        }
        if (offscreenBuffer == null) {
            Graphics2D g2 = (Graphics2D) g;
            ChartRenderer.enableQualityHints(g2);
            drawChartContent(g2, w, h);
            return;
        }
        Graphics bg = offscreenBuffer.getGraphics();
        try {
            Graphics2D g2 = (Graphics2D) bg;
            ChartRenderer.enableQualityHints(g2);
            g2.setColor(palette.getCardBg());
            g2.fillRect(0, 0, w, h);
            drawChartContent(g2, w, h);
        } finally {
            bg.dispose();
        }
        g.drawImage(offscreenBuffer, 0, 0, this);
    }

    protected abstract void drawChartContent(Graphics2D g2, int width, int height);
}
