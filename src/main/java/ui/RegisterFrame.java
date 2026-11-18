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
 * Pure Java AWT Registration Window for new Stock Price Predictor users.
 * Validates full name, username, email, and matching passwords, prevents duplicates,
 * enforces USER role, and hashes passwords using PBKDF2WithHmacSHA256.
 */
public class RegisterFrame extends Frame {

    private final AppContext appContext;
    private final ThemePalette palette;

    private final TextField fullNameField;
    private final TextField usernameField;
    private final TextField emailField;
    private final TextField passwordField;
    private final TextField confirmPasswordField;
    private final Button showPassBtn;
    private final Label feedbackLbl;

    private boolean passwordVisible = false;

    public RegisterFrame(AppContext appContext) {
        super("Create Account — Stock Price Predictor (CSE 213 | SRM University AP)");
        this.appContext = appContext;
        this.palette = appContext.getSavedPalette();

        setLayout(new GridBagLayout());
        setBackground(palette.getPageBg());
        setMinimumSize(new Dimension(760, 640));
        setSize(880, 700);
        setLocationRelativeTo(null);

        AwtCardPanel card = new AwtCardPanel(new BorderLayout(0, 14), 22, 28);
        card.applyTheme(palette);
        card.setPreferredSize(new Dimension(500, 580));

        // Top Header
        Panel header = new Panel(new BorderLayout(12, 0));
        header.setBackground(palette.getCardBg());

        LogoCanvas logo = new LogoCanvas(44);
        logo.setHostBackground(palette.getCardBg());
        logo.applyTheme(palette);
        header.add(logo, BorderLayout.WEST);

        Panel headerText = new Panel(new GridLayout(2, 1, 0, 2));
        headerText.setBackground(palette.getCardBg());

        Label titleLbl = new Label("Register New User Account");
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titleLbl.setForeground(palette.getTextPrimary());
        titleLbl.setBackground(palette.getCardBg());

        Label subLbl = new Label("Standard USER role · PBKDF2 password hashing · SQLite persistence");
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        subLbl.setForeground(palette.getTextSecondary());
        subLbl.setBackground(palette.getCardBg());

        headerText.add(titleLbl);
        headerText.add(subLbl);
        header.add(headerText, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        // Form Fields
        Panel form = new Panel(new GridLayout(0, 1, 0, 8));
        form.setBackground(palette.getCardBg());

        this.fullNameField = createInput(false);
        this.usernameField = createInput(false);
        this.emailField = createInput(false);
        this.passwordField = createInput(true);
        this.confirmPasswordField = createInput(true);

        this.showPassBtn = new Button("Show");
        showPassBtn.setFont(ThemePalette.FONT_LABEL);
        showPassBtn.setBackground(palette.getHoverBg());
        showPassBtn.setForeground(palette.getTextPrimary());
        showPassBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassBtn.addActionListener(e -> togglePasswordVisibility());

        form.add(labeledRow("FULL NAME", fullNameField));
        form.add(labeledRow("USERNAME (3–32 chars, no spaces)", usernameField));
        form.add(labeledRow("EMAIL ADDRESS", emailField));

        Panel passWrap = new Panel(new BorderLayout(6, 0));
        passWrap.setBackground(palette.getCardBg());
        passWrap.add(passwordField, BorderLayout.CENTER);
        passWrap.add(showPassBtn, BorderLayout.EAST);
        form.add(labeledRow("PASSWORD (min 8 chars, letters + numbers)", passWrap));
        form.add(labeledRow("CONFIRM PASSWORD", confirmPasswordField));

        this.feedbackLbl = new Label("All fields are required. Duplicate usernames or emails are rejected.");
        feedbackLbl.setFont(ThemePalette.FONT_BODY);
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setBackground(palette.getCardBg());
        form.add(feedbackLbl);

        card.add(form, BorderLayout.CENTER);

        // Action Buttons
        Panel actions = new Panel(new GridLayout(1, 2, 12, 0));
        actions.setBackground(palette.getCardBg());

        Button backBtn = new Button("Back to Login");
        backBtn.setFont(ThemePalette.FONT_BUTTON);
        backBtn.setBackground(palette.getHoverBg());
        backBtn.setForeground(palette.getTextPrimary());
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> appContext.showLoginFrame(this, null));

        Button registerBtn = new Button("Register Account");
        registerBtn.setFont(ThemePalette.FONT_BUTTON);
        registerBtn.setBackground(palette.getAccentViolet());
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ActionListener submitAction = e -> handleRegister();
        registerBtn.addActionListener(submitAction);
        confirmPasswordField.addActionListener(submitAction);

        actions.add(backBtn);
        actions.add(registerBtn);
        card.add(actions, BorderLayout.SOUTH);

        add(card, new GridBagConstraints());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        char echo = passwordVisible ? (char) 0 : '*';
        passwordField.setEchoChar(echo);
        confirmPasswordField.setEchoChar(echo);
        showPassBtn.setLabel(passwordVisible ? "Hide" : "Show");
    }

    private void handleRegister() {
        try {
            User created = appContext.getAuthenticationService().registerUser(
                    fullNameField.getText(),
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText()
            );
            passwordField.setText("");
            confirmPasswordField.setText("");
            appContext.showLoginFrame(
                    this,
                    "Account @" + created.getUsername() + " registered successfully! Sign in below."
            );
        } catch (ValidationException | DatabaseException ex) {
            feedbackLbl.setForeground(palette.getLossRed());
            feedbackLbl.setText(ex.getMessage());
        }
    }

    private TextField createInput(boolean masked) {
        TextField tf = new TextField("", 28);
        tf.setFont(ThemePalette.FONT_MONO);
        tf.setBackground(palette.getInputBg());
        tf.setForeground(palette.getTextPrimary());
        if (masked) {
            tf.setEchoChar('*');
        }
        return tf;
    }

    private Panel labeledRow(String caption, Component input) {
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
}
