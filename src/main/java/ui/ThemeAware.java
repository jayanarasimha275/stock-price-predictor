package ui;

import util.ThemePalette;

/**
 * Interface implemented by all AWT UI components that support live Dark / Light theme switching.
 */
public interface ThemeAware {
    void applyTheme(ThemePalette palette);
}
