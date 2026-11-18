package ui;

import util.ThemePalette;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pure AWT modal Dialog for confirmations and informational alerts.
 */
public class ConfirmDialog extends Dialog {

    private boolean confirmed = false;

    private ConfirmDialog(Frame owner, String title, String message, String confirmLabel,
                          Color confirmColor, boolean isAlertOnly, ThemePalette palette) {
        super(owner, title, true);
        setLayout(new BorderLayout(12, 12));
        setBackground(palette.getCardBg());
        setResizable(false);

        Panel body = new Panel(new BorderLayout(0, 10)) {
            @Override
            public Insets getInsets() {
                return new Insets(18, 22, 12, 22);
            }
        };
        body.setBackground(palette.getCardBg());

        Label titleLbl = new Label(title);
        titleLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        titleLbl.setForeground(isAlertOnly ? palette.getAccentViolet() : palette.getLossRed());
        titleLbl.setBackground(palette.getCardBg());

        TextArea msgArea = new TextArea(message, 4, 46, TextArea.SCROLLBARS_NONE);
        msgArea.setEditable(false);
        msgArea.setFont(ThemePalette.FONT_BODY);
        msgArea.setBackground(palette.getCardBg());
        msgArea.setForeground(palette.getTextPrimary());

        body.add(titleLbl, BorderLayout.NORTH);
        body.add(msgArea, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        Panel footer = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(palette.getChromeBg());

        if (!isAlertOnly) {
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setFont(ThemePalette.FONT_BUTTON);
            cancelBtn.setBackground(palette.getHoverBg());
            cancelBtn.setForeground(palette.getTextPrimary());
            cancelBtn.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            footer.add(cancelBtn);
        }

        Button okBtn = new Button(confirmLabel);
        okBtn.setFont(ThemePalette.FONT_BUTTON);
        okBtn.setBackground(confirmColor);
        okBtn.setForeground(Color.WHITE);
        okBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        footer.add(okBtn);

        add(footer, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
                dispose();
            }
        });

        pack();
        setSize(Math.max(420, getWidth()), Math.max(190, getHeight()));
        if (owner != null) {
            Point loc = owner.getLocationOnScreen();
            setLocation(loc.x + (owner.getWidth() - getWidth()) / 2,
                    loc.y + (owner.getHeight() - getHeight()) / 2);
        }
    }

    public static boolean showConfirm(Frame owner, String title, String message,
                                      String confirmLabel, ThemePalette palette) {
        ConfirmDialog dlg = new ConfirmDialog(owner, title, message, confirmLabel,
                palette.getLossRed(), false, palette);
        dlg.setVisible(true);
        return dlg.confirmed;
    }

    public static void showAlert(Frame owner, String title, String message, ThemePalette palette) {
        ConfirmDialog dlg = new ConfirmDialog(owner, title, message, "OK",
                palette.getAccentViolet(), true, palette);
        dlg.setVisible(true);
    }
}
