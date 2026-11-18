package ui;

import util.ThemePalette;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Left navigation Sidebar Panel containing all 10 module buttons,
 * active violet selection state, and SRM AP CSE 213 info card.
 */
public class Sidebar extends Panel implements ThemeAware {

    public interface SidebarListener {
        void onModuleSelected(String moduleId);
    }

    private static final String[][] NAV_ITEMS = {
            {"overview", "1. All Stocks Overview"},
            {"add", "2. Add / Load Stock"},
            {"predict", "3. Predict Next Price"},
            {"indicators", "4. Technical Indicators"},
            {"history", "5. Price History"},
            {"accuracy", "6. Prediction Accuracy"},
            {"forecast", "7. Forecast Report"},
            {"price-change", "8. Price Change"},
            {"compare", "9. Compare Two Stocks"},
            {"delete", "10. Delete Stock"}
    };

    private final SidebarListener listener;
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Label sectionTitle;
    private final AwtCardPanel infoBox;
    private final Label infoHeading;
    private final Label infoLine1;
    private final Label infoLine2;

    private ThemePalette palette = ThemePalette.DARK;
    private String activeModuleId = "overview";
    private int stockCount = 0;

    public Sidebar(SidebarListener listener) {
        this.listener = listener;
        setLayout(new BorderLayout(0, 10));
        setPreferredSize(new Dimension(235, 600));

        Panel top = new Panel(new BorderLayout(0, 8));
        this.sectionTitle = new Label("  PREDICTOR MODULES");
        sectionTitle.setFont(ThemePalette.FONT_LABEL);
        top.add(sectionTitle, BorderLayout.NORTH);

        Panel navList = new Panel(new GridLayout(NAV_ITEMS.length, 1, 0, 5));
        for (String[] item : NAV_ITEMS) {
            String id = item[0];
            String label = item[1];
            Button btn = new Button(" " + label);
            btn.setFont(ThemePalette.FONT_BUTTON);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                setActiveModule(id);
                if (this.listener != null) {
                    this.listener.onModuleSelected(id);
                }
            });
            navButtons.put(id, btn);
            navList.add(btn);
        }
        top.add(navList, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Bottom academic info box
        this.infoBox = new AwtCardPanel(new GridLayout(3, 1, 0, 2), 10, 12);
        this.infoHeading = new Label("SRM AP CSE 213");
        infoHeading.setFont(ThemePalette.FONT_BUTTON);
        this.infoLine1 = new Label("Advanced Java Programming");
        infoLine1.setFont(ThemePalette.FONT_SUBTITLE);
        this.infoLine2 = new Label("Pure Java AWT + SQLite JDBC");
        infoLine2.setFont(ThemePalette.FONT_SUBTITLE);
        infoBox.add(infoHeading);
        infoBox.add(infoLine1);
        infoBox.add(infoLine2);
        add(infoBox, BorderLayout.SOUTH);

        applyTheme(ThemePalette.DARK);
    }

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

    public void setActiveModule(String moduleId) {
        this.activeModuleId = moduleId;
        refreshButtonStyles();
    }

    public void setStockCount(int count) {
        this.stockCount = count;
        Button overviewBtn = navButtons.get("overview");
        if (overviewBtn != null) {
            overviewBtn.setLabel(" 1. All Stocks Overview (" + stockCount + ")");
        }
    }

    private void refreshButtonStyles() {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(activeModuleId);
            Button btn = entry.getValue();
            if (isActive) {
                btn.setBackground(palette.getAccentViolet());
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(palette.getChromeBg());
                btn.setForeground(palette.getTextSecondary());
            }
        }
    }

    @Override
    public void applyTheme(ThemePalette palette) {
        this.palette = palette;
        setBackground(palette.getChromeBg());
        for (Component c : getComponents()) {
            if (c instanceof Panel p && !(c instanceof AwtCardPanel)) {
                p.setBackground(palette.getChromeBg());
                for (Component inner : p.getComponents()) {
                    if (inner instanceof Panel innerP) {
                        innerP.setBackground(palette.getChromeBg());
                    }
                }
            }
        }
        sectionTitle.setBackground(palette.getChromeBg());
        sectionTitle.setForeground(palette.getTextSecondary());

        infoBox.applyTheme(palette);
        infoHeading.setBackground(palette.getCardBg());
        infoHeading.setForeground(palette.getTextPrimary());
        infoLine1.setBackground(palette.getCardBg());
        infoLine1.setForeground(palette.getTextSecondary());
        infoLine2.setBackground(palette.getCardBg());
        infoLine2.setForeground(palette.getAccentViolet());

        refreshButtonStyles();
        repaint();
    }
}
