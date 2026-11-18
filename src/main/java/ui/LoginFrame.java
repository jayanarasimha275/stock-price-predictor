package ui;

import model.User;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pure Java AWT Login Frame for Stock Price Predictor.
 * Supports username/email login, password masking + Show/Hide toggle, Clear,
 * registration navigation, protected first-time Admin setup, and role-based routing.
 */
public class LoginFrame extends Frame {

    private final AppContext appContext;
    private ThemePalette palette;

    private final LogoCanvas logoCanvas;
    private final TextField usernameField;
    private final TextField passwordField;
    private final Button showHideBtn;
    private final Button loginBtn;
    private final Button clearBtn;
    private final Button registerBtn;
    private final Button adminSetupBtn;
    private final Label feedbackLbl;
    private final Panel setupBanner;

    private boolean passwordVisible = false;

    public LoginFrame(AppContext appContext, String initialNotice) {
        super("Login — Stock Price Predictor (CSE 213 | SRM University AP)");
        this.appContext = appContext;
        this.palette = appContext.getSavedPalette();

        setLayout(new GridBagLayout());
        setBackground(palette.getPageBg());
        setMinimumSize(new Dimension(740, 580));
        setSize(860, 650);
        setLocationRelativeTo(null);

        // Centered auth card
        AwtCardPanel card = new AwtCardPanel(new BorderLayout(0, 16), 24, 28);
        card.applyTheme(palette);
        card.setPreferredSize(new Dimension(470, 510));

        // Top Brand Block
        Panel brandPanel = new Panel(new BorderLayout(14, 0));
        brandPanel.setBackground(palette.getCardBg());

        this.logoCanvas = new LogoCanvas(48);
        logoCanvas.setHostBackground(palette.getCardBg());
        logoCanvas.applyTheme(palette);
        brandPanel.add(logoCanvas, BorderLayout.WEST);

        Panel titles = new Panel(new GridLayout(3, 1, 0, 2));
        titles.setBackground(palette.getCardBg());

        Label appTitle = new Label("Stock Price Predictor");
        appTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        appTitle.setForeground(palette.getTextPrimary());
        appTitle.setBackground(palette.getCardBg());

        Label subTitle = new Label("CSE 213 – Advanced Java Programming | SRM University AP");
        subTitle.setFont(ThemePalette.FONT_SUBTITLE);
        subTitle.setForeground(palette.getAccentViolet());
        subTitle.setBackground(palette.getCardBg());

        Label signInPrompt = new Label("Sign in with your account credentials to continue.");
        signInPrompt.setFont(ThemePalette.FONT_BODY);
        signInPrompt.setForeground(palette.getTextSecondary());
        signInPrompt.setBackground(palette.getCardBg());

        titles.add(appTitle);
        titles.add(subTitle);
        titles.add(signInPrompt);
        brandPanel.add(titles, BorderLayout.CENTER);
        card.add(brandPanel, BorderLayout.NORTH);

        // Form Fields
        Panel form = new Panel(new GridLayout(0, 1, 0, 10));
        form.setBackground(palette.getCardBg());

        // Username row
        this.usernameField = new TextField("", 28);
        usernameField.setFont(ThemePalette.FONT_MONO);
        usernameField.setBackground(palette.getInputBg());
        usernameField.setForeground(palette.getTextPrimary());
        form.add(labeledField("USERNAME OR EMAIL", usernameField));

        // Password row with Show/Hide toggle
        this.passwordField = new TextField("", 22);
        passwordField.setFont(ThemePalette.FONT_MONO);
        passwordField.setBackground(palette.getInputBg());
        passwordField.setForeground(palette.getTextPrimary());
        passwordField.setEchoChar('*');

        this.showHideBtn = new Button("Show");
        showHideBtn.setFont(ThemePalette.FONT_LABEL);
        showHideBtn.setBackground(palette.getHoverBg());
        showHideBtn.setForeground(palette.getTextPrimary());
        showHideBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showHideBtn.addActionListener(e -> togglePasswordVisibility());

        Panel passWrap = new Panel(new BorderLayout(8, 0));
        passWrap.setBackground(palette.getCardBg());
        passWrap.add(passwordField, BorderLayout.CENTER);
        passWrap.add(showHideBtn, BorderLayout.EAST);
        form.add(labeledField("PASSWORD", passWrap));

        // Inline feedback banner
        this.feedbackLbl = new Label(
                (initialNotice != null && !initialNotice.isEmpty())
                        ? initialNotice
                        : "Passwords are verified securely via PBKDF2WithHmacSHA256."
        );
        feedbackLbl.setFont(ThemePalette.FONT_BODY);
        feedbackLbl.setForeground((initialNotice != null && !initialNotice.isEmpty())
                ? palette.getGainGreen()
                : palette.getTextSecondary());
        feedbackLbl.setBackground(palette.getCardBg());
        form.add(feedbackLbl);

        // Primary action buttons: Login + Clear
        Panel primaryBtns = new Panel(new GridLayout(1, 2, 10, 0));
        primaryBtns.setBackground(palette.getCardBg());

        this.loginBtn = new Button("Login");
        loginBtn.setFont(ThemePalette.FONT_BUTTON);
        loginBtn.setBackground(palette.getAccentViolet());
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.clearBtn = new Button("Clear");
        clearBtn.setFont(ThemePalette.FONT_BUTTON);
        clearBtn.setBackground(palette.getHoverBg());
        clearBtn.setForeground(palette.getTextPrimary());
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        primaryBtns.add(loginBtn);
        primaryBtns.add(clearBtn);
        form.add(primaryBtns);

        // First-time Admin setup notice banner (visible ONLY when no admin exists yet)
        this.setupBanner = new Panel(new BorderLayout(8, 0));
        setupBanner.setBackground(palette.getAmberBg());

        Label setupHint = new Label(" No Admin configured yet — initial setup required");
        setupHint.setFont(ThemePalette.FONT_LABEL);
        setupHint.setForeground(palette.getFlatAmber());
        setupHint.setBackground(palette.getAmberBg());

        this.adminSetupBtn = new Button("Initial Admin Setup");
        adminSetupBtn.setFont(ThemePalette.FONT_LABEL);
        adminSetupBtn.setBackground(palette.getFlatAmber());
        adminSetupBtn.setForeground(Color.BLACK);
        adminSetupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adminSetupBtn.addActionListener(e -> openAdminSetup());

        setupBanner.add(setupHint, BorderLayout.CENTER);
        setupBanner.add(adminSetupBtn, BorderLayout.EAST);
        form.add(setupBanner);

        card.add(form, BorderLayout.CENTER);

        // Bottom Registration CTA
        Panel footer = new Panel(new BorderLayout(8, 6));
        footer.setBackground(palette.getCardBg());

        Panel regRow = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        regRow.setBackground(palette.getCardBg());
        Label newHere = new Label("New to Stock Price Predictor?");
        newHere.setFont(ThemePalette.FONT_BODY);
        newHere.setForeground(palette.getTextSecondary());
        newHere.setBackground(palette.getCardBg());

        this.registerBtn = new Button("Create User Account");
        registerBtn.setFont(ThemePalette.FONT_BUTTON);
        registerBtn.setBackground(palette.getAccentVioletSoft());
        registerBtn.setForeground(palette.getAccentViolet());
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        regRow.add(newHere);
        regRow.add(registerBtn);

        Label secNote = new Label("Pure Java AWT Desktop · SQLite JDBC", Label.CENTER);
        secNote.setFont(ThemePalette.FONT_SUBTITLE);
        secNote.setForeground(palette.getTextMuted());
        secNote.setBackground(palette.getCardBg());

        footer.add(regRow, BorderLayout.CENTER);
        footer.add(secNote, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        add(card, new GridBagConstraints());

        // Event bindings
        ActionListener submitAction = e -> handleLogin();
        loginBtn.addActionListener(submitAction);
        usernameField.addActionListener(submitAction);
        passwordField.addActionListener(submitAction);

        clearBtn.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            feedbackLbl.setForeground(palette.getTextSecondary());
            feedbackLbl.setText("Fields cleared. Enter username and password.");
            usernameField.requestFocus();
        });

        registerBtn.addActionListener(e -> appContext.showRegisterFrame(this));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        refreshAdminSetupBanner();
        validate();
    }

    public void checkInitialAdminSetup() {
        try {
            if (!appContext.getAuthenticationService().hasAnyAdmin()) {
                openAdminSetup();
            }
        } catch (DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void refreshAdminSetupBanner() {
        try {
            boolean hasAdmin = appContext.getAuthenticationService().hasAnyAdmin();
            setupBanner.setVisible(!hasAdmin);
            validate();
        } catch (DatabaseException ex) {
            setupBanner.setVisible(false);
        }
    }

    private void openAdminSetup() {
        try {
            if (appContext.getAuthenticationService().hasAnyAdmin()) {
                refreshAdminSetupBanner();
                setError("Administrator setup is already completed and locked.");
                return;
            }
            AdminSetupDialog dlg = new AdminSetupDialog(this, appContext.getAuthenticationService(), palette);
            dlg.setVisible(true);
            User created = dlg.getCreatedAdmin();
            if (created != null) {
                usernameField.setText(created.getUsername());
                passwordField.setText("");
                feedbackLbl.setForeground(palette.getGainGreen());
                feedbackLbl.setText("Initial Administrator @" + created.getUsername() + " created. Sign in now.");
                passwordField.requestFocus();
            }
            refreshAdminSetupBanner();
        } catch (DatabaseException ex) {
            setError("Database error: " + ex.getMessage());
        }
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '*');
        showHideBtn.setLabel(passwordVisible ? "Hide" : "Show");
    }

    private void handleLogin() {
        String identifier = usernameField.getText();
        String password = passwordField.getText();
        if (identifier == null || identifier.trim().isEmpty()) {
            setError("Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password == null || password.isEmpty()) {
            setError("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        try {
            User user = appContext.getAuthenticationService().authenticate(identifier, password);
            passwordField.setText("");
            appContext.openWorkspaceForUser(user, this);
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    private void setError(String message) {
        feedbackLbl.setForeground(palette.getLossRed());
        feedbackLbl.setText(message);
    }

    private Panel labeledField(String label, Component input) {
        Panel p = new Panel(new BorderLayout(0, 4));
        p.setBackground(palette.getCardBg());
        Label l = new Label(label);
        l.setFont(ThemePalette.FONT_LABEL);
        l.setForeground(palette.getTextSecondary());
        l.setBackground(palette.getCardBg());
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }
}
