package util;

import java.awt.Color;
import java.awt.Font;

/**
 * Encapsulates the neutral charcoal & violet dark theme and clean light theme palettes,
 * preserving the exact visual identity of Stock Price Predictor.
 */
public final class ThemePalette {

    public static final ThemePalette DARK = new ThemePalette(
            true,
            new Color(0x12, 0x12, 0x14), // pageBg: #121214
            new Color(0x17, 0x17, 0x1B), // chromeBg (header/sidebar): #17171B
            new Color(0x1C, 0x1C, 0x21), // cardBg: #1C1C21
            new Color(0x24, 0x24, 0x2B), // hoverBg: #24242B
            new Color(0x14, 0x14, 0x18), // inputBg
            new Color(0x2E, 0x2E, 0x36), // border: #2E2E36
            new Color(0xED, 0xED, 0xF0), // textPrimary: #EDEDF0
            new Color(0x9A, 0x9A, 0xA5), // textSecondary: #9A9AA5
            new Color(0x6B, 0x6B, 0x76), // textMuted: #6B6B76
            new Color(0x8B, 0x5C, 0xF6), // accentViolet: #8B5CF6
            new Color(0x7C, 0x3A, 0xED), // accentVioletHover: #7C3AED
            new Color(0x2B, 0x22, 0x45), // accentVioletSoft
            new Color(0x22, 0xC5, 0x5E), // gainGreen: #22C55E
            new Color(0x14, 0x2B, 0x1E), // gainBg
            new Color(0xF4, 0x3F, 0x5E), // lossRed: #F43F5E
            new Color(0x2E, 0x17, 0x1D), // lossBg
            new Color(0xF5, 0x9E, 0x0B), // flatAmber: #F59E0B
            new Color(0x2B, 0x22, 0x14), // amberBg
            new Color(0x22, 0xD3, 0xEE)  // cyanSecondary: #22D3EE
    );

    public static final ThemePalette LIGHT = new ThemePalette(
            false,
            new Color(0xF6, 0xF6, 0xF9), // pageBg: #F6F6F9
            new Color(0xFF, 0xFF, 0xFF), // chromeBg: #FFFFFF
            new Color(0xFF, 0xFF, 0xFF), // cardBg: #FFFFFF
            new Color(0xF1, 0xF1, 0xF5), // hoverBg
            new Color(0xF9, 0xF9, 0xFB), // inputBg
            new Color(0xE4, 0xE4, 0xEB), // border: #E4E4EB
            new Color(0x18, 0x18, 0x1B), // textPrimary: #18181B
            new Color(0x6B, 0x6B, 0x76), // textSecondary: #6B6B76
            new Color(0x8E, 0x8E, 0x9A), // textMuted
            new Color(0x7C, 0x3A, 0xED), // accentViolet: #7C3AED
            new Color(0x6D, 0x28, 0xD9), // accentVioletHover: #6D28D9
            new Color(0xED, 0xE9, 0xFE), // accentVioletSoft
            new Color(0x16, 0xA3, 0x4A), // gainGreen: #16A34A
            new Color(0xEC, 0xFD, 0xF5), // gainBg
            new Color(0xE1, 0x1D, 0x48), // lossRed: #E11D48
            new Color(0xFF, 0xF1, 0xF2), // lossBg
            new Color(0xD9, 0x77, 0x06), // flatAmber: #D97706
            new Color(0xFF, 0xFB, 0xEB), // amberBg
            new Color(0x08, 0x91, 0xB2)  // cyanSecondary: #0891B2
    );

    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_CARD_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    public static final Font FONT_BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_BUTTON = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static final Font FONT_KPI = new Font(Font.MONOSPACED, Font.BOLD, 20);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.BOLD, 13);
    public static final Font FONT_MONO_SMALL = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    private final boolean dark;
    private final Color pageBg;
    private final Color chromeBg;
    private final Color cardBg;
    private final Color hoverBg;
    private final Color inputBg;
    private final Color border;
    private final Color textPrimary;
    private final Color textSecondary;
    private final Color textMuted;
    private final Color accentViolet;
    private final Color accentVioletHover;
    private final Color accentVioletSoft;
    private final Color gainGreen;
    private final Color gainBg;
    private final Color lossRed;
    private final Color lossBg;
    private final Color flatAmber;
    private final Color amberBg;
    private final Color cyanSecondary;

    private ThemePalette(boolean dark, Color pageBg, Color chromeBg, Color cardBg, Color hoverBg,
                         Color inputBg, Color border, Color textPrimary, Color textSecondary,
                         Color textMuted, Color accentViolet, Color accentVioletHover,
                         Color accentVioletSoft, Color gainGreen, Color gainBg, Color lossRed,
                         Color lossBg, Color flatAmber, Color amberBg, Color cyanSecondary) {
        this.dark = dark;
        this.pageBg = pageBg;
        this.chromeBg = chromeBg;
        this.cardBg = cardBg;
        this.hoverBg = hoverBg;
        this.inputBg = inputBg;
        this.border = border;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.accentViolet = accentViolet;
        this.accentVioletHover = accentVioletHover;
        this.accentVioletSoft = accentVioletSoft;
        this.gainGreen = gainGreen;
        this.gainBg = gainBg;
        this.lossRed = lossRed;
        this.lossBg = lossBg;
        this.flatAmber = flatAmber;
        this.amberBg = amberBg;
        this.cyanSecondary = cyanSecondary;
    }

    public boolean isDark() { return dark; }
    public Color getPageBg() { return pageBg; }
    public Color getChromeBg() { return chromeBg; }
    public Color getCardBg() { return cardBg; }
    public Color getHoverBg() { return hoverBg; }
    public Color getInputBg() { return inputBg; }
    public Color getBorder() { return border; }
    public Color getTextPrimary() { return textPrimary; }
    public Color getTextSecondary() { return textSecondary; }
    public Color getTextMuted() { return textMuted; }
    public Color getAccentViolet() { return accentViolet; }
    public Color getAccentVioletHover() { return accentVioletHover; }
    public Color getAccentVioletSoft() { return accentVioletSoft; }
    public Color getGainGreen() { return gainGreen; }
    public Color getGainBg() { return gainBg; }
    public Color getLossRed() { return lossRed; }
    public Color getLossBg() { return lossBg; }
    public Color getFlatAmber() { return flatAmber; }
    public Color getAmberBg() { return amberBg; }
    public Color getCyanSecondary() { return cyanSecondary; }
}
