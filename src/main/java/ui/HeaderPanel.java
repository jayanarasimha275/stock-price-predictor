package ui;

import model.Currency;
import util.ChartRenderer;
import util.ThemePalette;

import java.awt.*;

/**
 * Top navigation header Panel featuring a Graphics2D violet trend logo Canvas,
 * stock count badge, CSE 213 subtitle, INR/USD currency switcher, How It Works, and Dark/Light toggle.
 */
public class HeaderPanel extends Panel implements ThemeAware {

    public interface HeaderListener {
        void onCurrencyChanged(Currency newCurrency);
        void onToggleTheme();
        void onOpenHowItWorks();
    }

    private final HeaderListener listener;
    private final LogoCanvas logoCanvas;
    private final Label titleLbl;
    private final Label countBadge;
    private final Label subtitleLbl;
    private final Button inrBtn;
    private final Button usdBtn;
    private final Button howItWorksBtn;
    private final Button themeBtn;

    private ThemePalette palette = ThemePalette.DARK;
    private Currency activeCurrency = Currency.INR;
    private int stockCount = 0;

    public HeaderPanel(HeaderListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(16, 0));

        // Left: Logo + Title + Subtitle
        Panel left = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        this.logoCanvas = new LogoCanvas();
        left.add(logoCanvas);

        Panel textStack = new Panel(new GridLayout(2, 1, 0, 2));
        Panel titleRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.titleLbl = new Label("Stock Price Predictor");
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        this.countBadge = new Label("0 Stocks");
        countBadge.setFont(ThemePalette.FONT_LABEL);
        titleRow.add(titleLbl);
        titleRow.add(countBadge);

        this.subtitleLbl = new Label("CSE 213 – Advanced Java Programming | SRM University AP");
        subtitleLbl.setFont(ThemePalette.FONT_SUBTITLE);

        textStack.add(titleRow);
        textStack.add(subtitleLbl);
        left.add(textStack);
        add(left, BorderLayout.WEST);

        // Right: Currency toggle, How It Works, Theme toggle
        Panel right = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 14));

        this.inrBtn = new Button("INR (₹)");
        inrBtn.setFont(ThemePalette.FONT_BUTTON);
        inrBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        inrBtn.addActionListener(e -> {
            setCurrency(Currency.INR);
            if (this.listener != null) this.listener.onCurrencyChanged(Currency.INR);
        });

        this.usdBtn = new Button("USD ($)");
        usdBtn.setFont(ThemePalette.FONT_BUTTON);
        usdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usdBtn.addActionListener(e -> {
            setCurrency(Currency.USD);
            if (this.listener != null) this.listener.onCurrencyChanged(Currency.USD);
        });

        this.howItWorksBtn = new Button("? How It Works");
        howItWorksBtn.setFont(ThemePalette.FONT_BUTTON);
        howItWorksBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        howItWorksBtn.addActionListener(e -> {
            if (this.listener != null) this.listener.onOpenHowItWorks();
        });

        this.themeBtn = new Button("Light Mode");
        themeBtn.setFont(ThemePalette.FONT_BUTTON);
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.addActionListener(e -> {
            if (this.listener != null) this.listener.onToggleTheme();
        });

        right.add(inrBtn);
        right.add(usdBtn);
        right.add(howItWorksBtn);
        right.add(themeBtn);
        add(right, BorderLayout.EAST);

        applyTheme(ThemePalette.DARK);
    }

    @Override
    public Insets getInsets() {
        return new Insets(2, 12, 4, 12);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(palette.getBorder());
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }

    public void setStockCount(int count) {
        this.stockCount = count;
        countBadge.setText(count + (count == 1 ? " Stock" : " Stocks"));
        validate();
    }

    public void setCurrency(Currency currency) {
        this.activeCurrency = currency != null ? currency : Currency.INR;
        updateCurrencyButtons();
    }

    private void updateCurrencyButtons() {
        if (activeCurrency == Currency.INR) {
            inrBtn.setBackground(palette.getAccentViolet());
            inrBtn.setForeground(Color.WHITE);
            usdBtn.setBackground(palette.getCardBg());
            usdBtn.setForeground(palette.getTextSecondary());
        } else {
            usdBtn.setBackground(palette.getAccentViolet());
            usdBtn.setForeground(Color.WHITE);
            inrBtn.setBackground(palette.getCardBg());
            inrBtn.setForeground(palette.getTextSecondary());
        }
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        setBackground(palette.getChromeBg());
        applyContainerBg(this, palette.getChromeBg());

        logoCanvas.applyTheme(palette);
        titleLbl.setForeground(palette.getTextPrimary());
        subtitleLbl.setForeground(palette.getTextSecondary());

        countBadge.setBackground(palette.getAccentVioletSoft());
        countBadge.setForeground(palette.getAccentViolet());

        howItWorksBtn.setBackground(palette.getCardBg());
        howItWorksBtn.setForeground(palette.getTextPrimary());

        themeBtn.setLabel(palette.isDark() ? "Light Mode" : "Dark Mode");
        themeBtn.setBackground(palette.getCardBg());
        themeBtn.setForeground(palette.getFlatAmber());

        updateCurrencyButtons();
        repaint();
    }

    private void applyContainerBg(Container c, Color bg) {
        c.setBackground(bg);
        for (Component child : c.getComponents()) {
            if (child instanceof Panel p) {
                applyContainerBg(p, bg);
            } else if (child instanceof Label l && l != countBadge) {
                l.setBackground(bg);
            }
        }
    }

    /**
     * Custom AWT Canvas rendering the violet rounded square icon with white upward trend line.
     */
    private static class LogoCanvas extends Canvas implements ThemeAware {
        private ThemePalette palette = ThemePalette.DARK;

        LogoCanvas() {
            setPreferredSize(new Dimension(38, 38));
            setSize(38, 38);
        }

        @Override
        public void applyTheme(ThemePalette palette) {
            this.palette = palette;
            setBackground(palette.getChromeBg());
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            ChartRenderer.enableQualityHints(g2);
            g2.setColor(palette.getChromeBg());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(palette.getAccentViolet());
            g2.fillRoundRect(1, 1, 36, 36, 10, 10);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int[] xs = {9, 16, 21, 29};
            int[] ys = {25, 18, 21, 12};
            g2.drawPolyline(xs, ys, 4);
            // Arrow head
            g2.drawLine(24, 12, 29, 12);
            g2.drawLine(29, 12, 29, 17);
            g2.dispose();
        }
    }
}
