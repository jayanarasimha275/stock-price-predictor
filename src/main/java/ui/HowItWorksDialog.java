package ui;

import util.ThemePalette;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Pure AWT Dialog explaining the mathematical formulas and CSE 213 Advanced Java architecture.
 */
public class HowItWorksDialog extends Dialog {

    public HowItWorksDialog(Frame owner, ThemePalette palette) {
        super(owner, "How It Works: Core Quantitative Algorithms", true);
        setLayout(new BorderLayout());
        setBackground(palette.getCardBg());
        setSize(680, 560);

        Panel header = new Panel(new BorderLayout()) {
            @Override
            public Insets getInsets() {
                return new Insets(14, 20, 14, 20);
            }
        };
        header.setBackground(palette.getChromeBg());

        Panel titleStack = new Panel(new GridLayout(2, 1, 0, 2));
        titleStack.setBackground(palette.getChromeBg());
        Label titleLbl = new Label("How It Works: Core Quantitative Algorithms");
        titleLbl.setFont(ThemePalette.FONT_CARD_TITLE);
        titleLbl.setForeground(palette.getTextPrimary());
        titleLbl.setBackground(palette.getChromeBg());

        Label subLbl = new Label("CSE 213 Advanced Java Programming Specification | SRM University AP");
        subLbl.setFont(ThemePalette.FONT_SUBTITLE);
        subLbl.setForeground(palette.getTextSecondary());
        subLbl.setBackground(palette.getChromeBg());

        titleStack.add(titleLbl);
        titleStack.add(subLbl);
        header.add(titleStack, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        String guideText = """
                All computations run locally in Java 17 and persist permanently in SQLite via JDBC.

                1. Simple Moving Average (SMA)
                   Unweighted mean of the last p closing prices:
                   SMA(d, p) = (d[n-p] + d[n-p+1] + ... + d[n-1]) / p
                   Returns 0 when p <= 0 or history length < p.

                2. Exponential Moving Average (EMA)
                   Weights recent prices higher using k = 2 / (p + 1):
                   Seed: EMA_0 = SMA of first p prices.
                   Iteration (i = p .. n-1): EMA_i = (d[i] - EMA_{i-1}) * k + EMA_{i-1}.

                3. Momentum
                   Velocity of price movement over p steps:
                   Momentum(d, p) = d[n-1] - d[n-1-p]   (requires n > p).

                4. Relative Strength Index (RSI)
                   Measures overbought (>= 70) and oversold (<= 30) conditions using Wilder smoothing:
                   RS = AvgGain / AvgLoss,   RSI = 100 - (100 / (1 + RS)).

                5. Moving Average Convergence Divergence (MACD)
                   MACD Line   = EMA(fast) - EMA(slow)   [Default: 12, 26]
                   Signal Line = EMA(MACD Line, signalPeriod)   [Default: 9]
                   Histogram   = MACD Line - Signal Line

                6. Linear Regression Slope & Next Price Prediction
                   Fits Ordinary Least Squares over indices x = 0..n-1:
                   Slope m = (n * sum(xy) - sum(x) * sum(y)) / (n * sum(x^2) - (sum(x))^2)
                   PredictNext(d) = d[last] + Slope
                   Trend: Upward (pred > last), Downward (pred < last), Flat (equal).

                7. Prediction Accuracy Backtesting (requires >= 3 prices)
                   Trains on d[0..n-2], predicts d[n-1], and measures error:
                   Error = |Predicted - Actual|
                   Accuracy % = max(0, 100 - (Error / Actual) * 100)
                """;

        TextArea contentArea = new TextArea(guideText, 20, 72, TextArea.SCROLLBARS_VERTICAL_ONLY);
        contentArea.setEditable(false);
        contentArea.setFont(ThemePalette.FONT_MONO_SMALL);
        contentArea.setBackground(palette.getCardBg());
        contentArea.setForeground(palette.getTextPrimary());
        add(contentArea, BorderLayout.CENTER);

        Panel footer = new Panel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(palette.getChromeBg());
        Button closeBtn = new Button("Close");
        closeBtn.setFont(ThemePalette.FONT_BUTTON);
        closeBtn.setBackground(palette.getAccentViolet());
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        if (owner != null) {
            Point loc = owner.getLocationOnScreen();
            setLocation(loc.x + (owner.getWidth() - getWidth()) / 2,
                    loc.y + (owner.getHeight() - getHeight()) / 2);
        }
    }
}
