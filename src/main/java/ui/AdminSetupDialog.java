package ui;

import model.User;
import service.AuthenticationService;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Protected first-time Administrator setup Dialog shown ONLY when zero ADMIN accounts exist in SQLite.
 * Ensures zero hardcoded administrator passwords in source code and permanently locks after creation.
 */
public class AdminSetupDialog extends Dialog {

    private final AuthenticationService authService;
    private final ThemePalette palette;

    private final TextField fullNameField;
    private final TextField usernameField;
    private final TextField emailField;
    private final TextField passwordField;
    private final TextField confirmField;
    private final Button showPassBtn;
    private final Label statusLbl;

    private boolean passwordVisible = false;
    private User createdAdmin = null;

    public AdminSetupDialog(Frame owner, AuthenticationService authService, ThemePalette palette) {
        super(owner, "First-Time Administrator Setup — Stock Price Predictor", true);
        this.authService = authService;
        this.palette = palette != null ? palette : ThemePalette.DARK;

        setLayout(new BorderLayout(0, 0));
        setBackground(this.palette.getPageBg());
        setResizable(false);

        // Header banner
        Panel header = new Panel(new FlowLayout(FlowLayout.LEFT, 14, 14));
        header.setBackground(this.palette.getChromeBg());
        LogoCanvas logo = new LogoCanvas(42);
        logo.setHostBackground(this.palette.getChromeBg());
        logo.applyTheme(this.palette);
        header.add(logo);

        Panel headerText = new Panel(new GridLayout(2, 1, 0, 2));
        headerText.setBackground(this.palette.getChromeBg());
        Label title = new Label("First-Time Administrator Setup");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        title.setForeground(this.palette.getTextPrimary());
        title.setBackground(this.palette.getChromeBg());

        Label subtitle = new Label("No ADMIN account exists yet. Create the initial Administrator to unlock system management.");
        subtitle.setFont(ThemePalette.FONT_SUBTITLE);
        subtitle.setForeground(this.palette.getTextSecondary());
        subtitle.setBackground(this.palette.getChromeBg());

        headerText.add(title);
        headerText.add(subtitle);
        header.add(headerText);
        add(header, BorderLayout.NORTH);

        // Form card
        Panel formWrap = new Panel(new BorderLayout(0, 12)) {
            @Override
            public Insets getInsets() {
                return new Insets(18, 22, 16, 22);
            }
        };
        formWrap.setBackground(this.palette.getPageBg());

        AwtCardPanel card = new AwtCardPanel(new GridLayout(0, 1, 0, 8), 16, 18);
        card.applyTheme(this.palette);

        this.fullNameField = createInput("", false);
        this.usernameField = createInput("admin", false);
        this.emailField = createInput("", false);
        this.passwordField = createInput("", true);
        this.confirmField = createInput("", true);

        this.showPassBtn = new Button("Show");
        showPassBtn.setFont(ThemePalette.FONT_LABEL);
        showPassBtn.setBackground(this.palette.getHoverBg());
        showPassBtn.setForeground(this.palette.getTextPrimary());
        showPassBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassBtn.addActionListener(e -> togglePasswordMask());

        card.add(buildFieldRow("FULL NAME", fullNameField));
        card.add(buildFieldRow("ADMIN USERNAME", usernameField));
        card.add(buildFieldRow("ADMIN EMAIL", emailField));
        card.add(buildPasswordRow("ADMIN PASSWORD (min 8 chars, letters + digits)", passwordField, showPassBtn));
        card.add(buildFieldRow("CONFIRM PASSWORD", confirmField));

        this.statusLbl = new Label("PBKDF2WithHmacSHA256 salt+hash is generated locally. Setup locks automatically once created.");
        statusLbl.setFont(ThemePalette.FONT_SUBTITLE);
        statusLbl.setForeground(this.palette.getTextSecondary());
        statusLbl.setBackground(this.palette.getCardBg());
        card.add(statusLbl);

        formWrap.add(card, BorderLayout.CENTER);
        add(formWrap, BorderLayout.CENTER);

        // Footer buttons
        Panel footer = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(this.palette.getChromeBg());

        Button cancelBtn = new Button("Skip for Now");
        cancelBtn.setFont(ThemePalette.FONT_BUTTON);
        cancelBtn.setBackground(this.palette.getHoverBg());
        cancelBtn.setForeground(this.palette.getTextPrimary());
        cancelBtn.addActionListener(e -> dispose());

        Button createBtn = new Button("Create Initial Administrator");
        createBtn.setFont(ThemePalette.FONT_BUTTON);
        createBtn.setBackground(this.palette.getAccentViolet());
        createBtn.setForeground(Color.WHITE);
        createBtn.addActionListener(e -> handleCreateAdmin());

        footer.add(cancelBtn);
        footer.add(createBtn);
        add(footer, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        setSize(540, 500);
        if (owner != null && owner.isShowing()) {
            Point loc = owner.getLocationOnScreen();
            setLocation(loc.x + (owner.getWidth() - getWidth()) / 2,
                    loc.y + (owner.getHeight() - getHeight()) / 2);
        } else {
            setLocationRelativeTo(null);
        }
    }

    public User getCreatedAdmin() {
        return createdAdmin;
    }

    private void togglePasswordMask() {
        passwordVisible = !passwordVisible;
        char echo = passwordVisible ? (char) 0 : '*';
        passwordField.setEchoChar(echo);
        confirmField.setEchoChar(echo);
        showPassBtn.setLabel(passwordVisible ? "Hide" : "Show");
    }

    private void handleCreateAdmin() {
        try {
            User admin = authService.setupInitialAdmin(
                    fullNameField.getText(),
                    usernameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmField.getText()
            );
            this.createdAdmin = admin;
            dispose();
        } catch (ValidationException | DatabaseException ex) {
            statusLbl.setForeground(palette.getLossRed());
            statusLbl.setText("Error: " + ex.getMessage());
        }
    }

    private TextField createInput(String initial, boolean masked) {
        TextField tf = new TextField(initial, 30);
        tf.setFont(ThemePalette.FONT_MONO);
        tf.setBackground(palette.getInputBg());
        tf.setForeground(palette.getTextPrimary());
        if (masked) {
            tf.setEchoChar('*');
        }
        return tf;
    }

    private Panel buildFieldRow(String caption, Component input) {
        Panel row = new Panel(new BorderLayout(0, 3));
        row.setBackground(palette.getCardBg());
        Label lbl = new Label(caption);
        lbl.setFont(ThemePalette.FONT_LABEL);
        lbl.setForeground(palette.getTextSecondary());
        lbl.setBackground(palette.getCardBg());
        row.add(lbl, BorderLayout.NORTH);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    private Panel buildPasswordRow(String caption, TextField input, Button toggleBtn) {
        Panel row = new Panel(new BorderLayout(0, 3));
        row.setBackground(palette.getCardBg());
        Label lbl = new Label(caption);
        lbl.setFont(ThemePalette.FONT_LABEL);
        lbl.setForeground(palette.getTextSecondary());
        lbl.setBackground(palette.getCardBg());

        Panel inner = new Panel(new BorderLayout(6, 0));
        inner.setBackground(palette.getCardBg());
        inner.add(input, BorderLayout.CENTER);
        inner.add(toggleBtn, BorderLayout.EAST);

        row.add(lbl, BorderLayout.NORTH);
        row.add(inner, BorderLayout.CENTER);
        return row;
    }
}
