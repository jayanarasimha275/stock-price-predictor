package ui;

import model.Currency;
import model.User;
import util.ThemePalette;

import java.awt.*;

/**
 * Top navigation header Panel featuring the shared Graphics2D violet trend LogoCanvas,
 * stock count badge, authenticated user badge, INR/USD currency switcher, How It Works,
 * Dark/Light toggle, and Logout action.
 */
public class HeaderPanel extends Panel implements ThemeAware {

    public interface HeaderListener {
        void onCurrencyChanged(Currency newCurrency);
        void onToggleTheme();
        void onOpenHowItWorks();
        default void onLogout() {
        }
    }

    private final HeaderListener listener;
    private final LogoCanvas logoCanvas;
    private final Label titleLbl;
    private final Label countBadge;
    private final Label userBadge;
    private final Label subtitleLbl;
    private final Button inrBtn;
    private final Button usdBtn;
    private final Button howItWorksBtn;
    private final Button themeBtn;
    private final Button logoutBtn;

    private ThemePalette palette = ThemePalette.DARK;
    private Currency activeCurrency = Currency.INR;
    private int stockCount = 0;

    public HeaderPanel(HeaderListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(16, 0));

        // Left: Logo + Title + Subtitle
        Panel left = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        this.logoCanvas = new LogoCanvas(38);
        left.add(logoCanvas);

        Panel textStack = new Panel(new GridLayout(2, 1, 0, 2));
        Panel titleRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        this.titleLbl = new Label("Stock Price Predictor");
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        this.countBadge = new Label("0 Stocks");
        countBadge.setFont(ThemePalette.FONT_LABEL);
        this.userBadge = new Label("");
        userBadge.setFont(ThemePalette.FONT_LABEL);
        userBadge.setVisible(false);

        titleRow.add(titleLbl);
        titleRow.add(countBadge);
        titleRow.add(userBadge);

        this.subtitleLbl = new Label("CSE 213 – Advanced Java Programming | SRM University AP");
        subtitleLbl.setFont(ThemePalette.FONT_SUBTITLE);

        textStack.add(titleRow);
        textStack.add(subtitleLbl);
        left.add(textStack);
        add(left, BorderLayout.WEST);

        // Right: Currency toggle, How It Works, Theme toggle, Logout
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

        this.logoutBtn = new Button("Logout");
        logoutBtn.setFont(ThemePalette.FONT_BUTTON);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setVisible(false);
        logoutBtn.addActionListener(e -> {
            if (this.listener != null) this.listener.onLogout();
        });

        right.add(inrBtn);
        right.add(usdBtn);
        right.add(howItWorksBtn);
        right.add(themeBtn);
        right.add(logoutBtn);
        add(right, BorderLayout.EAST);

        applyTheme(ThemePalette.DARK);
    }

    public void setAuthenticatedUser(User user, boolean allowLogout) {
        if (user != null) {
            userBadge.setText("@" + user.getUsername() + " [" + user.getRole().getLabel() + "]");
            userBadge.setVisible(true);
        } else {
            userBadge.setText("");
            userBadge.setVisible(false);
        }
        logoutBtn.setVisible(allowLogout);
        validate();
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

        logoCanvas.setHostBackground(palette.getChromeBg());
        logoCanvas.applyTheme(palette);
        titleLbl.setForeground(palette.getTextPrimary());
        subtitleLbl.setForeground(palette.getTextSecondary());

        countBadge.setBackground(palette.getAccentVioletSoft());
        countBadge.setForeground(palette.getAccentViolet());

        userBadge.setBackground(palette.getGainBg());
        userBadge.setForeground(palette.getGainGreen());

        howItWorksBtn.setBackground(palette.getCardBg());
        howItWorksBtn.setForeground(palette.getTextPrimary());

        themeBtn.setLabel(palette.isDark() ? "Light Mode" : "Dark Mode");
        themeBtn.setBackground(palette.getCardBg());
        themeBtn.setForeground(palette.getFlatAmber());

        logoutBtn.setBackground(palette.getLossBg());
        logoutBtn.setForeground(palette.getLossRed());

        updateCurrencyButtons();
        repaint();
    }

    private void applyContainerBg(Container c, Color bg) {
        c.setBackground(bg);
        for (Component child : c.getComponents()) {
            if (child instanceof Panel p) {
                applyContainerBg(p, bg);
            } else if (child instanceof Label l && l != countBadge && l != userBadge) {
                l.setBackground(bg);
            }
        }
    }
}

