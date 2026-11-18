package ui;

import model.Currency;
import model.Stock;
import model.User;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated Pure Java AWT Admin Dashboard Frame.
 * Strictly restricted to active ADMIN accounts and provides:
 * 1. Dashboard Overview (total users, stocks, predictions, recent user registrations)
 * 2. User Management (search, add, edit, activate/deactivate, role change, delete)
 * 3. Stock Management (view, add, edit, delete, inspect history)
 * 4. Prediction Management (view prediction records, history, and chart details)
 * 5. Admin Settings (profile view, PBKDF2 password change, logout)
 */
public class AdminDashboard extends Frame {

    private static final String PREF_CURRENCY = "currency";
    private static final String PREF_THEME = "theme";

    private static final String[][] SIDEBAR_ITEMS = {
            {"overview", "1. Dashboard Overview"},
            {"users", "2. User Management"},
            {"stocks", "3. Stock Management"},
            {"predictions", "4. Prediction Management"},
            {"settings", "5. Admin Settings"}
    };

    private final AppContext appContext;
    private User adminUser;
    private ThemePalette palette;
    private Currency currency;
    private String activeSection = "overview";

    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final LogoCanvas logoCanvas = new LogoCanvas(36);
    private final Label adminBadgeLbl = new Label("");
    private final Button inrBtn = new Button("INR (₹)");
    private final Button usdBtn = new Button("USD ($)");
    private final Button themeBtn = new Button("Light Mode");
    private final Button logoutBtn = new Button("Logout");

    private final ScrollPane scrollPane;
    private final Panel contentStage;

    private final UserManagementPanel userManagementPanel;
    private final AdminStockPanel adminStockPanel;
    private final AdminPredictionPanel adminPredictionPanel;

    private final Panel headerPanel;
    private final Panel sidebarPanel;
    private final Panel footerPanel;

    public AdminDashboard(AppContext appContext, User adminUser) {
        super("Admin Dashboard — Stock Price Predictor (CSE 213 | SRM University AP)");
        if (adminUser == null || !adminUser.isAdmin() || !adminUser.isActive()) {
            throw new SecurityException("Unauthorized access: Active Administrator account is required.");
        }
        this.appContext = appContext;
        this.adminUser = adminUser;
        this.palette = appContext.getSavedPalette();
        this.currency = Currency.fromCode(appContext.getSettingsRepository().getSetting(PREF_CURRENCY, "INR"));

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(1020, 680));
        setSize(1280, 820);
        setLocationRelativeTo(null);

        // Top Bar
        this.headerPanel = buildTopBar();
        add(headerPanel, BorderLayout.NORTH);

        // Left Sidebar
        this.sidebarPanel = buildSidebar();
        add(sidebarPanel, BorderLayout.WEST);

        // Scrollable Center Stage
        this.scrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        this.contentStage = new Panel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int vpW = scrollPane.getViewportSize().width;
                if (vpW > 100) {
                    return new Dimension(Math.max(vpW - 4, 740), Math.max(d.height, scrollPane.getViewportSize().height));
                }
                return d;
            }
        };
        scrollPane.add(contentStage);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        this.footerPanel = buildFooter();
        add(footerPanel, BorderLayout.SOUTH);

        // Reusable Admin Sub-panels
        this.userManagementPanel = new UserManagementPanel(
                appContext.getUserService(), adminUser, palette, this::refreshActiveOverviewIfNeeded
        );
        this.adminStockPanel = new AdminStockPanel(
                appContext.getStockService(), appContext.getPredictionService(), palette, currency, this::refreshActiveOverviewIfNeeded
        );
        this.adminPredictionPanel = new AdminPredictionPanel(
                appContext.getStockService(), appContext.getPredictionService(), palette, currency, this::refreshActiveOverviewIfNeeded
        );

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

        applyChromeTheme();
        navigateTo("overview");
    }

    private Panel buildTopBar() {
        Panel header = new Panel(new BorderLayout(16, 0)) {
            @Override
            public Insets getInsets() {
                return new Insets(6, 14, 6, 14);
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(palette.getBorder());
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };

        Panel left = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        left.add(logoCanvas);

        Panel titleStack = new Panel(new GridLayout(2, 1, 0, 2));
        Panel titleRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        Label titleLbl = new Label("Stock Price Predictor · Admin Console");
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        adminBadgeLbl.setText("ADMIN: @" + adminUser.getUsername());
        adminBadgeLbl.setFont(ThemePalette.FONT_LABEL);
        titleRow.add(titleLbl);
        titleRow.add(adminBadgeLbl);

        Label subLbl = new Label("CSE 213 – Advanced Java Programming | Role-Based Administration & Audit");
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        titleStack.add(titleRow);
        titleStack.add(subLbl);
        left.add(titleStack);
        header.add(left, BorderLayout.WEST);

        Panel right = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        inrBtn.setFont(ThemePalette.FONT_BUTTON);
        inrBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        inrBtn.addActionListener(e -> handleCurrencySwitch(Currency.INR));

        usdBtn.setFont(ThemePalette.FONT_BUTTON);
        usdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usdBtn.addActionListener(e -> handleCurrencySwitch(Currency.USD));

        themeBtn.setFont(ThemePalette.FONT_BUTTON);
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.addActionListener(e -> handleThemeToggle());

        logoutBtn.setFont(ThemePalette.FONT_BUTTON);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> handleLogout());

        right.add(inrBtn);
        right.add(usdBtn);
        right.add(themeBtn);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private Panel buildSidebar() {
        Panel sb = new Panel(new BorderLayout(0, 10)) {
            @Override
            public Insets getInsets() {
                return new Insets(14, 12, 14, 12);
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(palette.getBorder());
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        };
        sb.setPreferredSize(new Dimension(235, 600));

        Panel top = new Panel(new BorderLayout(0, 8));
        Label secLbl = new Label("  ADMIN CONTROLS");
        secLbl.setFont(ThemePalette.FONT_LABEL);
        top.add(secLbl, BorderLayout.NORTH);

        Panel navList = new Panel(new GridLayout(SIDEBAR_ITEMS.length, 1, 0, 6));
        for (String[] item : SIDEBAR_ITEMS) {
            String id = item[0];
            Button btn = new Button(" " + item[1]);
            btn.setFont(ThemePalette.FONT_BUTTON);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> navigateTo(id));
            navButtons.put(id, btn);
            navList.add(btn);
        }
        top.add(navList, BorderLayout.CENTER);
        sb.add(top, BorderLayout.NORTH);

        AwtCardPanel infoBox = new AwtCardPanel(new GridLayout(3, 1, 0, 2), 10, 12);
        Label l1 = new Label("PBKDF2 + SQLite RBAC");
        l1.setFont(ThemePalette.FONT_BUTTON);
        Label l2 = new Label("Role: " + adminUser.getRole().getLabel());
        l2.setFont(ThemePalette.FONT_SUBTITLE);
        Label l3 = new Label("SRM University AP");
        l3.setFont(ThemePalette.FONT_SUBTITLE);
        infoBox.add(l1);
        infoBox.add(l2);
        infoBox.add(l3);
        sb.add(infoBox, BorderLayout.SOUTH);

        return sb;
    }

    private Panel buildFooter() {
        Panel footer = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 6)) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(palette.getBorder());
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        Label lbl = new Label("Administrator Console · Stock Price Predictor · CSE 213 Advanced Java Programming, SRM University AP");
        lbl.setFont(ThemePalette.FONT_SUBTITLE);
        footer.add(lbl);
        return footer;
    }

    public void navigateTo(String sectionId) {
        this.activeSection = sectionId;
        refreshSidebarStyles();

        Component view;
        switch (sectionId) {
            case "users" -> {
                userManagementPanel.setAdminActor(adminUser);
                userManagementPanel.applyTheme(palette);
                view = userManagementPanel;
            }
            case "stocks" -> {
                adminStockPanel.setCurrency(currency);
                adminStockPanel.applyTheme(palette);
                view = adminStockPanel;
            }
            case "predictions" -> {
                adminPredictionPanel.setCurrency(currency);
                adminPredictionPanel.applyTheme(palette);
                view = adminPredictionPanel;
            }
            case "settings" -> view = buildAdminSettingsPanel();
            default -> view = buildOverviewPanel();
        }

        contentStage.removeAll();
        contentStage.add(view, BorderLayout.NORTH);
        contentStage.validate();
        scrollPane.doLayout();
        scrollPane.setScrollPosition(0, 0);
        validate();
        repaint();
    }

    private void refreshActiveOverviewIfNeeded() {
        if ("overview".equals(activeSection)) {
            navigateTo("overview");
        }
    }

    private Panel buildOverviewPanel() {
        Panel panel = new Panel(new BorderLayout(0, 14)) {
            @Override
            public Insets getInsets() {
                return new Insets(18, 22, 22, 22);
            }
        };
        panel.setBackground(palette.getPageBg());

        // Header
        Panel header = new Panel(new BorderLayout(12, 4));
        header.setBackground(palette.getPageBg());
        Panel titleStack = new Panel(new GridLayout(2, 1, 0, 2));
        titleStack.setBackground(palette.getPageBg());
        Label title = new Label("Administrator Overview");
        title.setFont(ThemePalette.FONT_TITLE);
        title.setForeground(palette.getTextPrimary());
        title.setBackground(palette.getPageBg());
        Label sub = new Label("Live system metrics across registered users, SQLite portfolio stocks, and generated predictions.");
        sub.setFont(ThemePalette.FONT_SUBTITLE);
        sub.setForeground(palette.getTextSecondary());
        sub.setBackground(palette.getPageBg());
        titleStack.add(title);
        titleStack.add(sub);
        header.add(titleStack, BorderLayout.CENTER);

        Button refreshBtn = styledBtn("Refresh Metrics", palette.getAccentViolet(), Color.WHITE);
        refreshBtn.addActionListener(e -> navigateTo("overview"));
        header.add(refreshBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        int totalUsers = 0;
        int totalStocks = 0;
        int totalPredictions = 0;
        int activeUsers = 0;
        List<User> recentUsers = List.of();
        List<Stock> stocks = List.of();
        try {
            List<User> allUsers = appContext.getUserService().getAllUsers(adminUser);
            totalUsers = allUsers.size();
            activeUsers = (int) allUsers.stream().filter(User::isActive).count();
            recentUsers = appContext.getUserService().getRecentRegistrations(adminUser, 8);
            stocks = appContext.getStockService().getAllStocks();
            totalStocks = stocks.size();
            totalPredictions = appContext.getPredictionService().getTotalPredictionCount();
        } catch (ValidationException | DatabaseException ex) {
            ConfirmDialog.showAlert(this, "Admin Metrics Error", ex.getMessage(), palette);
        }

        Panel body = new Panel(new BorderLayout(0, 14));
        body.setBackground(palette.getPageBg());

        // KPI Row
        Panel kpiRow = new Panel(new GridLayout(1, 4, 12, 12));
        kpiRow.setBackground(palette.getPageBg());
        kpiRow.add(kpiCard("Total Registered Users", String.valueOf(totalUsers), activeUsers + " active accounts", palette.getAccentViolet()));
        kpiRow.add(kpiCard("Total Stocks", String.valueOf(totalStocks), "Stored in SQLite", palette.getGainGreen()));
        kpiRow.add(kpiCard("Total Predictions", String.valueOf(totalPredictions), "OLS forecasts saved", palette.getCyanSecondary()));
        kpiRow.add(kpiCard("Signed-In Admin", "@" + adminUser.getUsername(), "Active RBAC Session", palette.getFlatAmber()));
        body.add(kpiRow, BorderLayout.NORTH);

        // Recent Registrations Card + Quick Admin Shortcuts
        Panel middle = new Panel(new BorderLayout(0, 14));
        middle.setBackground(palette.getPageBg());

        AwtCardPanel recentCard = new AwtCardPanel(new BorderLayout(0, 8), 14, 16);
        recentCard.applyTheme(palette);
        Label recentTitle = new Label("Recent User Registrations");
        recentTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        recentTitle.setForeground(palette.getTextPrimary());
        recentTitle.setBackground(palette.getCardBg());

        String hdr = String.format("%-4s | %-20s | %-16s | %-26s | %-6s | %-8s | %-16s",
                "ID", "FULL NAME", "USERNAME", "EMAIL", "ROLE", "STATUS", "REGISTERED AT");
        Label hdrLbl = new Label(hdr);
        hdrLbl.setFont(ThemePalette.FONT_MONO_SMALL);
        hdrLbl.setForeground(palette.getAccentViolet());
        hdrLbl.setBackground(palette.getChromeBg());

        java.awt.List recentList = new java.awt.List(7, false);
        recentList.setFont(ThemePalette.FONT_MONO_SMALL);
        recentList.setBackground(palette.getInputBg());
        recentList.setForeground(palette.getTextPrimary());

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (User u : recentUsers) {
            recentList.add(String.format("%-4d | %-20s | %-16s | %-26s | %-6s | %-8s | %-16s",
                    u.getId(),
                    truncate(u.getFullName(), 20),
                    truncate(u.getUsername(), 16),
                    truncate(u.getEmail(), 26),
                    u.getRole().getLabel(),
                    u.getStatus().getLabel(),
                    fmt.format(new Date(u.getCreatedAt()))));
        }

        Panel recentHead = new Panel(new GridLayout(2, 1, 0, 4));
        recentHead.setBackground(palette.getCardBg());
        recentHead.add(recentTitle);
        recentHead.add(hdrLbl);
        recentCard.add(recentHead, BorderLayout.NORTH);
        recentCard.add(recentList, BorderLayout.CENTER);
        middle.add(recentCard, BorderLayout.CENTER);

        AwtCardPanel quickCard = new AwtCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 4), 12, 16);
        quickCard.applyTheme(palette);
        Label qLbl = new Label("Quick Management Actions:");
        qLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        qLbl.setForeground(palette.getTextPrimary());
        qLbl.setBackground(palette.getCardBg());

        Button uBtn = styledBtn("Manage Users", palette.getAccentViolet(), Color.WHITE);
        uBtn.addActionListener(e -> navigateTo("users"));
        Button sBtn = styledBtn("Manage Stocks (" + stocks.size() + ")", palette.getHoverBg(), palette.getTextPrimary());
        sBtn.addActionListener(e -> navigateTo("stocks"));
        Button pBtn = styledBtn("Inspect Predictions", palette.getHoverBg(), palette.getTextPrimary());
        pBtn.addActionListener(e -> navigateTo("predictions"));
        Button cfgBtn = styledBtn("Admin Settings", palette.getHoverBg(), palette.getTextPrimary());
        cfgBtn.addActionListener(e -> navigateTo("settings"));

        quickCard.add(qLbl);
        quickCard.add(uBtn);
        quickCard.add(sBtn);
        quickCard.add(pBtn);
        quickCard.add(cfgBtn);
        middle.add(quickCard, BorderLayout.SOUTH);

        body.add(middle, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private Panel buildAdminSettingsPanel() {
        Panel panel = new Panel(new BorderLayout(0, 14)) {
            @Override
            public Insets getInsets() {
                return new Insets(18, 22, 22, 22);
            }
        };
        panel.setBackground(palette.getPageBg());

        Panel header = new Panel(new GridLayout(2, 1, 0, 2));
        header.setBackground(palette.getPageBg());
        Label title = new Label("Admin Settings & Profile");
        title.setFont(ThemePalette.FONT_TITLE);
        title.setForeground(palette.getTextPrimary());
        title.setBackground(palette.getPageBg());
        Label sub = new Label("Review administrator profile, rotate your PBKDF2 password, or end your session.");
        sub.setFont(ThemePalette.FONT_SUBTITLE);
        sub.setForeground(palette.getTextSecondary());
        sub.setBackground(palette.getPageBg());
        header.add(title);
        header.add(sub);
        panel.add(header, BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(1, 2, 14, 0));
        grid.setBackground(palette.getPageBg());

        // Left: Admin Profile Card
        AwtCardPanel profileCard = new AwtCardPanel(new BorderLayout(0, 12), 16, 18);
        profileCard.applyTheme(palette);
        Label profTitle = new Label("Administrator Profile");
        profTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        profTitle.setForeground(palette.getTextPrimary());
        profTitle.setBackground(palette.getCardBg());

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Panel rows = new Panel(new GridLayout(6, 1, 0, 8));
        rows.setBackground(palette.getCardBg());
        rows.add(profileRow("USER ID", "#" + adminUser.getId()));
        rows.add(profileRow("FULL NAME", adminUser.getFullName()));
        rows.add(profileRow("USERNAME", "@" + adminUser.getUsername()));
        rows.add(profileRow("EMAIL", adminUser.getEmail()));
        rows.add(profileRow("ROLE / STATUS", adminUser.getRole().getLabel() + " · " + adminUser.getStatus().getLabel()));
        rows.add(profileRow("CREATED AT", fmt.format(new Date(adminUser.getCreatedAt()))));

        Button signOutBtn = styledBtn("Logout to Sign-In Screen", palette.getLossRed(), Color.WHITE);
        signOutBtn.addActionListener(e -> handleLogout());

        profileCard.add(profTitle, BorderLayout.NORTH);
        profileCard.add(rows, BorderLayout.CENTER);
        profileCard.add(signOutBtn, BorderLayout.SOUTH);

        // Right: Change Password Card
        AwtCardPanel passCard = new AwtCardPanel(new BorderLayout(0, 12), 16, 18);
        passCard.applyTheme(palette);
        Label passTitle = new Label("Change Administrator Password");
        passTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        passTitle.setForeground(palette.getTextPrimary());
        passTitle.setBackground(palette.getCardBg());

        Panel form = new Panel(new GridLayout(0, 1, 0, 8));
        form.setBackground(palette.getCardBg());

        TextField currentPass = new TextField("", 24);
        currentPass.setFont(ThemePalette.FONT_MONO);
        currentPass.setBackground(palette.getInputBg());
        currentPass.setForeground(palette.getTextPrimary());
        currentPass.setEchoChar('*');

        TextField newPass = new TextField("", 24);
        newPass.setFont(ThemePalette.FONT_MONO);
        newPass.setBackground(palette.getInputBg());
        newPass.setForeground(palette.getTextPrimary());
        newPass.setEchoChar('*');

        TextField confirmNewPass = new TextField("", 24);
        confirmNewPass.setFont(ThemePalette.FONT_MONO);
        confirmNewPass.setBackground(palette.getInputBg());
        confirmNewPass.setForeground(palette.getTextPrimary());
        confirmNewPass.setEchoChar('*');

        Label passStatus = new Label("Minimum 8 characters with letters and digits.");
        passStatus.setFont(ThemePalette.FONT_BODY);
        passStatus.setForeground(palette.getTextSecondary());
        passStatus.setBackground(palette.getCardBg());

        form.add(labeledInput("CURRENT PASSWORD", currentPass));
        form.add(labeledInput("NEW PASSWORD", newPass));
        form.add(labeledInput("CONFIRM NEW PASSWORD", confirmNewPass));
        form.add(passStatus);

        Button updatePassBtn = styledBtn("Update Password", palette.getAccentViolet(), Color.WHITE);
        updatePassBtn.addActionListener(e -> {
            try {
                User updated = appContext.getAuthenticationService().changePassword(
                        adminUser,
                        currentPass.getText(),
                        newPass.getText(),
                        confirmNewPass.getText()
                );
                this.adminUser = updated;
                userManagementPanel.setAdminActor(updated);
                currentPass.setText("");
                newPass.setText("");
                confirmNewPass.setText("");
                passStatus.setForeground(palette.getGainGreen());
                passStatus.setText("Administrator password updated securely in SQLite.");
            } catch (ValidationException | DatabaseException ex) {
                passStatus.setForeground(palette.getLossRed());
                passStatus.setText(ex.getMessage());
            }
        });

        passCard.add(passTitle, BorderLayout.NORTH);
        passCard.add(form, BorderLayout.CENTER);
        passCard.add(updatePassBtn, BorderLayout.SOUTH);

        grid.add(profileCard);
        grid.add(passCard);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private void handleCurrencySwitch(Currency next) {
        this.currency = next;
        appContext.getSettingsRepository().setSetting(PREF_CURRENCY, next.getCode());
        updateCurrencyButtons();
        navigateTo(activeSection);
    }

    private void handleThemeToggle() {
        this.palette = palette.isDark() ? ThemePalette.LIGHT : ThemePalette.DARK;
        appContext.getSettingsRepository().setSetting(PREF_THEME, palette.isDark() ? "dark" : "light");
        applyChromeTheme();
        navigateTo(activeSection);
    }

    private void handleLogout() {
        appContext.showLoginFrame(this, "Administrator signed out safely.");
    }

    private void applyChromeTheme() {
        setBackground(palette.getPageBg());
        scrollPane.setBackground(palette.getPageBg());
        contentStage.setBackground(palette.getPageBg());

        applyContainerTheme(headerPanel, palette.getChromeBg());
        logoCanvas.setHostBackground(palette.getChromeBg());
        logoCanvas.applyTheme(palette);
        adminBadgeLbl.setBackground(palette.getAccentVioletSoft());
        adminBadgeLbl.setForeground(palette.getAccentViolet());

        themeBtn.setLabel(palette.isDark() ? "Light Mode" : "Dark Mode");
        themeBtn.setBackground(palette.getCardBg());
        themeBtn.setForeground(palette.getFlatAmber());

        logoutBtn.setBackground(palette.getLossBg());
        logoutBtn.setForeground(palette.getLossRed());

        updateCurrencyButtons();

        applyContainerTheme(sidebarPanel, palette.getChromeBg());
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof AwtCardPanel card) {
                card.applyTheme(palette);
                applyContainerTheme(card, palette.getCardBg());
            }
        }
        refreshSidebarStyles();

        applyContainerTheme(footerPanel, palette.getChromeBg());
        repaint();
    }

    private void updateCurrencyButtons() {
        if (currency == Currency.INR) {
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

    private void refreshSidebarStyles() {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(activeSection);
            Button btn = entry.getValue();
            if (active) {
                btn.setBackground(palette.getAccentViolet());
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(palette.getChromeBg());
                btn.setForeground(palette.getTextSecondary());
            }
        }
    }

    private void applyContainerTheme(Container root, Color bg) {
        root.setBackground(bg);
        for (Component comp : root.getComponents()) {
            if (comp instanceof Button || comp == adminBadgeLbl) {
                continue;
            }
            if (comp instanceof Label l) {
                l.setBackground(bg);
                l.setForeground(palette.getTextPrimary());
            } else if (comp instanceof Panel p && !(comp instanceof AwtCardPanel)) {
                applyContainerTheme(p, bg);
            }
        }
    }

    private AwtCardPanel kpiCard(String caption, String value, String sub, Color valueColor) {
        AwtCardPanel card = new AwtCardPanel(new GridLayout(3, 1, 0, 3), 12, 14);
        card.applyTheme(palette);

        Label capLbl = new Label(caption.toUpperCase());
        capLbl.setFont(ThemePalette.FONT_LABEL);
        capLbl.setForeground(palette.getTextSecondary());
        capLbl.setBackground(palette.getCardBg());

        Label valLbl = new Label(value);
        valLbl.setFont(ThemePalette.FONT_KPI);
        valLbl.setForeground(valueColor);
        valLbl.setBackground(palette.getCardBg());

        Label subLbl = new Label(sub);
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        subLbl.setForeground(palette.getTextSecondary());
        subLbl.setBackground(palette.getCardBg());

        card.add(capLbl);
        card.add(valLbl);
        card.add(subLbl);
        return card;
    }

    private Panel profileRow(String label, String value) {
        Panel row = new Panel(new BorderLayout(8, 0));
        row.setBackground(palette.getCardBg());
        Label l = new Label(label + ":");
        l.setFont(ThemePalette.FONT_LABEL);
        l.setForeground(palette.getTextSecondary());
        l.setBackground(palette.getCardBg());
        Label v = new Label(value);
        v.setFont(ThemePalette.FONT_MONO);
        v.setForeground(palette.getTextPrimary());
        v.setBackground(palette.getCardBg());
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        return row;
    }

    private Panel labeledInput(String caption, Component input) {
        Panel p = new Panel(new BorderLayout(0, 3));
        p.setBackground(palette.getCardBg());
        Label l = new Label(caption);
        l.setFont(ThemePalette.FONT_LABEL);
        l.setForeground(palette.getTextSecondary());
        l.setBackground(palette.getCardBg());
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private Button styledBtn(String label, Color bg, Color fg) {
        Button b = new Button(label);
        b.setFont(ThemePalette.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
