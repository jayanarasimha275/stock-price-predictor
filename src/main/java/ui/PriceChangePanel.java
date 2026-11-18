package ui;

import database.DatabaseSeeder;
import model.PriceChangeSummary;
import model.Stock;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import util.CurrencyConverter;
import util.DatabaseException;
import util.ThemePalette;

import java.awt.*;
import java.util.List;

/**
 * Module 8: Price Change.
 * Calculates daily price changes, overall inception-to-date changes, percentage changes,
 * High-Low spread, and ranks Gainers & Losers across the portfolio.
 */
public class PriceChangePanel extends BaseModulePanel {

    private Choice stockChoice;

    public PriceChangePanel(StockService stockService, PredictionService predictionService,
                            IndicatorService indicatorService, HistoryService historyService,
                            StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "price-change";
    }

    @Override
    public String getModuleTitle() {
        return "Price Change";
    }

    @Override
    public void refreshData() {
        removeAll();
        setBackground(palette.getPageBg());

        List<Stock> stocks;
        try {
            stocks = stockService.getAllStocks();
        } catch (DatabaseException ex) {
            showErrorDialog("Database Error", ex.getMessage());
            return;
        }

        if (stocks.isEmpty()) {
            add(buildModuleHeader("Price Change Analysis", "Daily and total return performance leaderboard.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Price Change", () -> {
                try {
                    DatabaseSeeder.loadSamplePortfolio(stockService);
                    navigator.onPortfolioChanged(null);
                } catch (DatabaseException ex) {
                    showErrorDialog("Database Error", ex.getMessage());
                }
            }), BorderLayout.CENTER);
            validate();
            repaint();
            return;
        }

        Stock active = resolveActiveStock(stocks);
        this.stockChoice = createStyledChoice();
        for (Stock s : stocks) {
            stockChoice.add(s.getName());
        }
        stockChoice.select(active.getName());
        stockChoice.addItemListener(e -> {
            setSelectedStockName(stockChoice.getSelectedItem());
            refreshData();
        });

        Panel selectorWrap = new Panel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        selectorWrap.setBackground(palette.getPageBg());
        Label selLbl = new Label("Focus Stock:");
        selLbl.setFont(ThemePalette.FONT_BUTTON);
        selLbl.setForeground(palette.getTextSecondary());
        selLbl.setBackground(palette.getPageBg());
        selectorWrap.add(selLbl);
        selectorWrap.add(stockChoice);

        add(buildModuleHeader(
                "Price Change Analysis — " + active.getName(),
                "Daily step change, total return from first price, and portfolio gainers & losers.",
                selectorWrap
        ), BorderLayout.NORTH);

        PriceChangeSummary summary = historyService.calculatePriceChange(active);
        boolean dailyPos = summary.getDailyChange() >= 0;
        boolean totalPos = summary.getAbsoluteChange() >= 0;

        Panel content = new Panel(new BorderLayout(0, 12));
        content.setBackground(palette.getPageBg());

        Panel kpiRow = new Panel(new GridLayout(1, 4, 12, 12));
        kpiRow.setBackground(palette.getPageBg());
        kpiRow.add(createKpiCard(
                "Initial Price (P1)",
                CurrencyConverter.format(summary.getFirstPrice(), currency),
                "High: " + CurrencyConverter.format(summary.getMaxPrice(), currency),
                palette.getTextPrimary()
        ));
        kpiRow.add(createKpiCard(
                "Latest Price (P" + summary.getPoints() + ")",
                CurrencyConverter.format(summary.getLatestPrice(), currency),
                "Low: " + CurrencyConverter.format(summary.getMinPrice(), currency),
                palette.getTextPrimary()
        ));
        kpiRow.add(createKpiCard(
                "Daily Change",
                CurrencyConverter.formatSigned(summary.getDailyChange(), currency),
                CurrencyConverter.formatSignedPercent(summary.getDailyPercentChange()) + " vs prev step",
                dailyPos ? palette.getGainGreen() : palette.getLossRed()
        ));
        kpiRow.add(createKpiCard(
                "Total Return",
                CurrencyConverter.formatSigned(summary.getAbsoluteChange(), currency),
                CurrencyConverter.formatSignedPercent(summary.getPercentChange()) + " | Spread: "
                        + CurrencyConverter.format(summary.getSpread(), currency),
                totalPos ? palette.getGainGreen() : palette.getLossRed()
        ));
        content.add(kpiRow, BorderLayout.NORTH);

        // Gainers vs Losers Split + Full Leaderboard
        List<PriceChangeSummary> rankedByDaily = historyService.getRankedSummaries(stocks, true);
        List<PriceChangeSummary> gainers = rankedByDaily.stream().filter(s -> s.getDailyChange() >= 0).toList();
        List<PriceChangeSummary> losers = rankedByDaily.stream().filter(s -> s.getDailyChange() < 0).toList();

        Panel split = new Panel(new GridLayout(1, 2, 12, 12));
        split.setBackground(palette.getPageBg());
        split.add(buildGainerLoserCard("Daily Gainers (" + gainers.size() + ")", gainers, true));
        split.add(buildGainerLoserCard("Daily Losers (" + losers.size() + ")", losers, false));

        AwtCardPanel fullBoard = new AwtCardPanel(new BorderLayout(0, 8), 12, 14);
        fullBoard.applyTheme(palette);
        Label boardTitle = new Label("Portfolio Inception-to-Date & Daily Performance Ranking");
        boardTitle.setFont(ThemePalette.FONT_CARD_TITLE);
        boardTitle.setForeground(palette.getTextPrimary());
        boardTitle.setBackground(palette.getCardBg());

        java.awt.List rankList = new java.awt.List(8, false);
        rankList.setFont(ThemePalette.FONT_MONO_SMALL);
        rankList.setBackground(palette.getInputBg());
        rankList.setForeground(palette.getTextPrimary());

        List<PriceChangeSummary> rankedTotal = historyService.getRankedSummaries(stocks, false);
        for (int i = 0; i < rankedTotal.size(); i++) {
            PriceChangeSummary r = rankedTotal.get(i);
            rankList.add(String.format("#%-2d %-12s | Latest: %-12s | Daily: %-18s | Total: %s (%s)",
                    i + 1,
                    r.getStockName(),
                    CurrencyConverter.format(r.getLatestPrice(), currency),
                    CurrencyConverter.formatSigned(r.getDailyChange(), currency) + " " + CurrencyConverter.formatSignedPercent(r.getDailyPercentChange()),
                    CurrencyConverter.formatSigned(r.getAbsoluteChange(), currency),
                    CurrencyConverter.formatSignedPercent(r.getPercentChange())
            ));
        }

        fullBoard.add(boardTitle, BorderLayout.NORTH);
        fullBoard.add(rankList, BorderLayout.CENTER);

        Panel lower = new Panel(new BorderLayout(0, 12));
        lower.setBackground(palette.getPageBg());
        lower.add(split, BorderLayout.NORTH);
        lower.add(fullBoard, BorderLayout.CENTER);

        content.add(lower, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private AwtCardPanel buildGainerLoserCard(String title, List<PriceChangeSummary> items, boolean isGainer) {
        AwtCardPanel card = new AwtCardPanel(new BorderLayout(0, 6), 12, 14);
        card.applyTheme(palette);

        Label hdr = new Label(title);
        hdr.setFont(ThemePalette.FONT_CARD_TITLE);
        hdr.setForeground(isGainer ? palette.getGainGreen() : palette.getLossRed());
        hdr.setBackground(palette.getCardBg());

        java.awt.List list = new java.awt.List(4, false);
        list.setFont(ThemePalette.FONT_MONO_SMALL);
        list.setBackground(palette.getInputBg());
        list.setForeground(isGainer ? palette.getGainGreen() : palette.getLossRed());

        if (items.isEmpty()) {
            list.add(isGainer ? "No daily gainers in current portfolio." : "No daily losers in current portfolio.");
        } else {
            for (PriceChangeSummary s : items) {
                list.add(String.format("%-12s %s (%s) -> Latest %s",
                        s.getStockName(),
                        CurrencyConverter.formatSigned(s.getDailyChange(), currency),
                        CurrencyConverter.formatSignedPercent(s.getDailyPercentChange()),
                        CurrencyConverter.format(s.getLatestPrice(), currency)));
            }
        }
        card.add(hdr, BorderLayout.NORTH);
        card.add(list, BorderLayout.CENTER);
        return card;
    }
}
