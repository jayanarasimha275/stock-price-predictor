package ui;

import model.User;
import model.UserRole;
import model.UserStatus;
import service.UserService;
import util.DatabaseException;
import util.ThemePalette;
import util.ValidationException;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Admin Module 2: User Management.
 * Supports viewing all registered users, searching, adding new users, editing details,
 * activating/deactivating accounts, authorized role changes, and deleting users.
 */
public class UserManagementPanel extends Panel implements ThemeAware {

    private final UserService userService;
    private User adminActor;
    private final Runnable onUsersChanged;
    private ThemePalette palette;

    private List<User> displayedUsers = new ArrayList<>();
    private User selectedUser = null;
    private boolean createMode = false;

    private TextField searchField;
    private java.awt.List userList;
    private TextField fullNameField;
    private TextField usernameField;
    private TextField emailField;
    private TextField passwordField;
    private TextField confirmPasswordField;
    private Choice roleChoice;
    private Choice statusChoice;
    private Label formModeTitle;
    private Label feedbackLbl;
    private Button saveBtn;
    private Button toggleStatusBtn;
    private Button toggleRoleBtn;
    private Button deleteBtn;
    private Panel passwordRowPanel;

    public UserManagementPanel(UserService userService, User adminActor,
                               ThemePalette palette, Runnable onUsersChanged) {
        this.userService = userService;
        this.adminActor = adminActor;
        this.palette = palette != null ? palette : ThemePalette.DARK;
        this.onUsersChanged = onUsersChanged;
        setLayout(new BorderLayout(0, 14));
        setBackground(this.palette.getPageBg());
    }

    @Override
    public Insets getInsets() {
        return new Insets(18, 22, 22, 22);
    }

    public void setAdminActor(User adminActor) {
        this.adminActor = adminActor;
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        refreshData();
    }

    public void refreshData() {
        removeAll();
        setBackground(palette.getPageBg());

        // Header
        Panel header = new Panel(new BorderLayout(12, 4));
        header.setBackground(palette.getPageBg());
        Panel titleStack = new Panel(new GridLayout(2, 1, 0, 2));
        titleStack.setBackground(palette.getPageBg());

        Label title = new Label("User Management");
        title.setFont(ThemePalette.FONT_TITLE);
        title.setForeground(palette.getTextPrimary());
        title.setBackground(palette.getPageBg());

        Label sub = new Label("View all accounts, search users, create/edit users, activate/deactivate, change roles, or delete.");
        sub.setFont(ThemePalette.FONT_SUBTITLE);
        sub.setForeground(palette.getTextSecondary());
        sub.setBackground(palette.getPageBg());

        titleStack.add(title);
        titleStack.add(sub);
        header.add(titleStack, BorderLayout.CENTER);

        Panel headerActions = new Panel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        headerActions.setBackground(palette.getPageBg());

        Button newUserBtn = styledBtn("+ Add New User", palette.getAccentViolet(), Color.WHITE);
        newUserBtn.addActionListener(e -> switchToCreateMode());

        Button refreshBtn = styledBtn("Refresh", palette.getHoverBg(), palette.getTextPrimary());
        refreshBtn.addActionListener(e -> loadUsers(searchField != null ? searchField.getText() : ""));

        headerActions.add(newUserBtn);
        headerActions.add(refreshBtn);
        header.add(headerActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        Panel body = new Panel(new BorderLayout(0, 12));
        body.setBackground(palette.getPageBg());

        // Search card
        AwtCardPanel searchCard = new AwtCardPanel(new BorderLayout(10, 0), 10, 14);
        searchCard.applyTheme(palette);
        Label searchLbl = new Label("Search Users (Name, Username, Email, Role):");
        searchLbl.setFont(ThemePalette.FONT_BUTTON);
        searchLbl.setForeground(palette.getTextPrimary());
        searchLbl.setBackground(palette.getCardBg());

        this.searchField = styledField("", 28);
        searchField.addTextListener(e -> loadUsers(searchField.getText()));

        Button clearSearchBtn = styledBtn("Clear", palette.getHoverBg(), palette.getTextPrimary());
        clearSearchBtn.addActionListener(e -> {
            searchField.setText("");
            loadUsers("");
        });

        searchCard.add(searchLbl, BorderLayout.WEST);
        searchCard.add(searchField, BorderLayout.CENTER);
        searchCard.add(clearSearchBtn, BorderLayout.EAST);
        body.add(searchCard, BorderLayout.NORTH);

        // Center: Table + Editor form
        Panel split = new Panel(new BorderLayout(0, 12));
        split.setBackground(palette.getPageBg());

        // Table card
        AwtCardPanel tableCard = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        tableCard.applyTheme(palette);

        String cols = String.format("%-4s | %-18s | %-15s | %-24s | %-6s | %-8s | %-16s",
                "ID", "FULL NAME", "USERNAME", "EMAIL", "ROLE", "STATUS", "CREATED AT");
        Label colHeader = new Label(cols);
        colHeader.setFont(ThemePalette.FONT_MONO_SMALL);
        colHeader.setForeground(palette.getAccentViolet());
        colHeader.setBackground(palette.getChromeBg());

        this.userList = new java.awt.List(9, false);
        userList.setFont(ThemePalette.FONT_MONO_SMALL);
        userList.setBackground(palette.getInputBg());
        userList.setForeground(palette.getTextPrimary());
        userList.addItemListener(e -> handleRowSelection());

        tableCard.add(colHeader, BorderLayout.NORTH);
        tableCard.add(userList, BorderLayout.CENTER);
        split.add(tableCard, BorderLayout.NORTH);

        // Form / Inspector card
        AwtCardPanel editorCard = new AwtCardPanel(new BorderLayout(0, 10), 14, 16);
        editorCard.applyTheme(palette);

        this.formModeTitle = new Label("Select a user above to edit, or click '+ Add New User'");
        formModeTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        formModeTitle.setForeground(palette.getTextPrimary());
        formModeTitle.setBackground(palette.getCardBg());
        editorCard.add(formModeTitle, BorderLayout.NORTH);

        Panel fieldsGrid = new Panel(new GridLayout(0, 3, 12, 8));
        fieldsGrid.setBackground(palette.getCardBg());

        this.fullNameField = styledField("", 20);
        this.usernameField = styledField("", 20);
        this.emailField = styledField("", 24);
        this.roleChoice = styledChoice();
        roleChoice.add(UserRole.USER.getLabel());
        roleChoice.add(UserRole.ADMIN.getLabel());

        this.statusChoice = styledChoice();
        statusChoice.add(UserStatus.ACTIVE.getLabel());
        statusChoice.add(UserStatus.INACTIVE.getLabel());

        fieldsGrid.add(fieldBlock("FULL NAME", fullNameField));
        fieldsGrid.add(fieldBlock("USERNAME", usernameField));
        fieldsGrid.add(fieldBlock("EMAIL", emailField));
        fieldsGrid.add(fieldBlock("ROLE", roleChoice));
        fieldsGrid.add(fieldBlock("ACCOUNT STATUS", statusChoice));

        // Password fields for Add New User mode
        this.passwordField = styledField("", 16);
        passwordField.setEchoChar('*');
        this.confirmPasswordField = styledField("", 16);
        confirmPasswordField.setEchoChar('*');

        this.passwordRowPanel = new Panel(new GridLayout(1, 2, 10, 0));
        passwordRowPanel.setBackground(palette.getCardBg());
        passwordRowPanel.add(fieldBlock("INITIAL PASSWORD", passwordField));
        passwordRowPanel.add(fieldBlock("CONFIRM PASSWORD", confirmPasswordField));
        fieldsGrid.add(passwordRowPanel);

        editorCard.add(fieldsGrid, BorderLayout.CENTER);

        // Action row + feedback
        Panel editorFooter = new Panel(new BorderLayout(0, 6));
        editorFooter.setBackground(palette.getCardBg());

        Panel btnRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnRow.setBackground(palette.getCardBg());

        this.saveBtn = styledBtn("Save User Changes", palette.getAccentViolet(), Color.WHITE);
        saveBtn.addActionListener(e -> handleSave());

        this.toggleStatusBtn = styledBtn("Activate / Deactivate", palette.getHoverBg(), palette.getFlatAmber());
        toggleStatusBtn.addActionListener(e -> handleToggleStatus());

        this.toggleRoleBtn = styledBtn("Switch Role (USER/ADMIN)", palette.getHoverBg(), palette.getCyanSecondary());
        toggleRoleBtn.addActionListener(e -> handleQuickRoleSwitch());

        this.deleteBtn = styledBtn("Delete User", palette.getLossBg(), palette.getLossRed());
        deleteBtn.addActionListener(e -> handleDeleteUser());

        btnRow.add(saveBtn);
        btnRow.add(toggleStatusBtn);
        btnRow.add(toggleRoleBtn);
        btnRow.add(deleteBtn);

        this.feedbackLbl = new Label(" ");
        feedbackLbl.setFont(ThemePalette.FONT_BODY);
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setBackground(palette.getCardBg());

        editorFooter.add(btnRow, BorderLayout.NORTH);
        editorFooter.add(feedbackLbl, BorderLayout.SOUTH);
        editorCard.add(editorFooter, BorderLayout.SOUTH);

        split.add(editorCard, BorderLayout.CENTER);
        body.add(split, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        loadUsers("");
        validate();
        repaint();
    }

    private void loadUsers(String query) {
        try {
            this.displayedUsers = userService.searchUsers(adminActor, query);
            userList.removeAll();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (User u : displayedUsers) {
                String row = String.format("%-4d | %-18s | %-15s | %-24s | %-6s | %-8s | %-16s",
                        u.getId(),
                        truncate(u.getFullName(), 18),
                        truncate(u.getUsername(), 15),
                        truncate(u.getEmail(), 24),
                        u.getRole().getLabel(),
                        u.getStatus().getLabel(),
                        fmt.format(new Date(u.getCreatedAt())));
                userList.add(row);
            }
            if (!createMode && !displayedUsers.isEmpty()) {
                int selectIdx = 0;
                if (selectedUser != null) {
                    for (int i = 0; i < displayedUsers.size(); i++) {
                        if (displayedUsers.get(i).getId() == selectedUser.getId()) {
                            selectIdx = i;
                            break;
                        }
                    }
                }
                userList.select(selectIdx);
                populateEditorFromSelected(displayedUsers.get(selectIdx));
            }
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleRowSelection() {
        int idx = userList.getSelectedIndex();
        if (idx >= 0 && idx < displayedUsers.size()) {
            createMode = false;
            populateEditorFromSelected(displayedUsers.get(idx));
        }
    }

    private void populateEditorFromSelected(User u) {
        this.selectedUser = u;
        this.createMode = false;
        formModeTitle.setText("Editing User #" + u.getId() + " (@" + u.getUsername() + ")");
        fullNameField.setText(u.getFullName());
        usernameField.setText(u.getUsername());
        emailField.setText(u.getEmail());
        roleChoice.select(u.getRole().getLabel());
        statusChoice.select(u.getStatus().getLabel());
        passwordField.setText("");
        confirmPasswordField.setText("");
        passwordRowPanel.setVisible(false);
        saveBtn.setLabel("Save User Changes");
        toggleStatusBtn.setLabel(u.isActive() ? "Deactivate Account" : "Activate Account");
        toggleStatusBtn.setEnabled(true);
        toggleRoleBtn.setEnabled(true);
        deleteBtn.setEnabled(true);
        validate();
    }

    private void switchToCreateMode() {
        this.createMode = true;
        this.selectedUser = null;
        formModeTitle.setText("Create New User Account (Authorized Admin Action)");
        fullNameField.setText("");
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        roleChoice.select(UserRole.USER.getLabel());
        statusChoice.select(UserStatus.ACTIVE.getLabel());
        passwordRowPanel.setVisible(true);
        saveBtn.setLabel("Create User");
        toggleStatusBtn.setEnabled(false);
        toggleRoleBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        setInfo("Enter details and an initial password (min 8 chars with letter + number).");
        validate();
        fullNameField.requestFocus();
    }

    private void handleSave() {
        try {
            UserRole chosenRole = UserRole.fromString(roleChoice.getSelectedItem());
            UserStatus chosenStatus = UserStatus.fromString(statusChoice.getSelectedItem());
            if (createMode || selectedUser == null) {
                User created = userService.createUser(
                        adminActor,
                        fullNameField.getText(),
                        usernameField.getText(),
                        emailField.getText(),
                        passwordField.getText(),
                        confirmPasswordField.getText(),
                        chosenRole,
                        chosenStatus
                );
                createMode = false;
                selectedUser = created;
                loadUsers(searchField.getText());
                setSuccess("Created user @" + created.getUsername() + " [" + created.getRole().getLabel() + "].");
            } else {
                User updated = userService.updateUser(
                        adminActor,
                        selectedUser.getId(),
                        fullNameField.getText(),
                        usernameField.getText(),
                        emailField.getText(),
                        chosenRole,
                        chosenStatus
                );
                selectedUser = updated;
                loadUsers(searchField.getText());
                setSuccess("Updated user @" + updated.getUsername() + ".");
            }
            if (onUsersChanged != null) onUsersChanged.run();
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleToggleStatus() {
        if (selectedUser == null) {
            setError("Select a user first.");
            return;
        }
        UserStatus next = selectedUser.isActive() ? UserStatus.INACTIVE : UserStatus.ACTIVE;
        try {
            userService.toggleStatus(adminActor, selectedUser.getId(), next);
            loadUsers(searchField.getText());
            setSuccess("Account @" + selectedUser.getUsername() + " set to " + next.getLabel() + ".");
            if (onUsersChanged != null) onUsersChanged.run();
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleQuickRoleSwitch() {
        if (selectedUser == null) {
            setError("Select a user first.");
            return;
        }
        UserRole next = selectedUser.isAdmin() ? UserRole.USER : UserRole.ADMIN;
        try {
            userService.changeRole(adminActor, selectedUser.getId(), next);
            loadUsers(searchField.getText());
            setSuccess("Changed role for @" + selectedUser.getUsername() + " to " + next.getLabel() + ".");
            if (onUsersChanged != null) onUsersChanged.run();
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void handleDeleteUser() {
        if (selectedUser == null) {
            setError("Select a user first.");
            return;
        }
        Frame owner = findParentFrame();
        boolean confirmed = ConfirmDialog.showConfirm(
                owner,
                "Delete User @" + selectedUser.getUsername() + "?",
                "Permanently delete user \"" + selectedUser.getFullName() + "\" (@" + selectedUser.getUsername() + ") from SQLite?",
                "Delete User",
                palette
        );
        if (!confirmed) {
            return;
        }
        try {
            String removedUsername = selectedUser.getUsername();
            userService.deleteUser(adminActor, selectedUser.getId());
            selectedUser = null;
            loadUsers(searchField.getText());
            setSuccess("Deleted user @" + removedUsername + ".");
            if (onUsersChanged != null) onUsersChanged.run();
        } catch (ValidationException | DatabaseException ex) {
            setError(ex.getMessage());
        }
    }

    private void setError(String msg) {
        feedbackLbl.setForeground(palette.getLossRed());
        feedbackLbl.setText(msg);
    }

    private void setSuccess(String msg) {
        feedbackLbl.setForeground(palette.getGainGreen());
        feedbackLbl.setText(msg);
    }

    private void setInfo(String msg) {
        feedbackLbl.setForeground(palette.getTextSecondary());
        feedbackLbl.setText(msg);
    }

    private Button styledBtn(String label, Color bg, Color fg) {
        Button b = new Button(label);
        b.setFont(ThemePalette.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private TextField styledField(String text, int cols) {
        TextField tf = new TextField(text, cols);
        tf.setFont(ThemePalette.FONT_MONO);
        tf.setBackground(palette.getInputBg());
        tf.setForeground(palette.getTextPrimary());
        return tf;
    }

    private Choice styledChoice() {
        Choice c = new Choice();
        c.setFont(ThemePalette.FONT_MONO);
        c.setBackground(palette.getInputBg());
        c.setForeground(palette.getTextPrimary());
        return c;
    }

    private Panel fieldBlock(String caption, Component input) {
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

    private Frame findParentFrame() {
        Component c = this;
        while (c != null) {
            if (c instanceof Frame f) return f;
            c = c.getParent();
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
