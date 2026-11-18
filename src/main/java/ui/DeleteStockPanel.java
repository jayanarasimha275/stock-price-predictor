package ui;

import database.DatabaseSeeder;
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
 * Module 10: Delete Stock.
 * Lists all stocks in SQLite, asks for explicit AWT Dialog confirmation before deleting,
 * and removes cascading records from stocks, price_history, predictions, and technical_indicators.
 */
public class DeleteStockPanel extends BaseModulePanel {

    public DeleteStockPanel(StockService stockService, PredictionService predictionService,
                            IndicatorService indicatorService, HistoryService historyService,
                            StockSelectionListener navigator) {
        super(stockService, predictionService, indicatorService, historyService, navigator);
    }

    @Override
    public String getModuleId() {
        return "delete";
    }

    @Override
    public String getModuleTitle() {
        return "Delete Stock";
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
            add(buildModuleHeader("Manage & Delete Stocks", "Remove stock records from SQLite.", null), BorderLayout.NORTH);
            add(buildEmptyStatePanel("Delete Stock", () -> {
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

        Button clearAllBtn = createStyledButton("Clear All Stocks", palette.getLossRed(), Color.WHITE);
        clearAllBtn.addActionListener(e -> handleClearAll(stocks.size()));

        add(buildModuleHeader(
                "Manage & Delete Stocks",
                "Remove individual stocks or reset all SQLite tables (deletes related history, predictions & indicators).",
                clearAllBtn
        ), BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(0, 2, 12, 12));
        grid.setBackground(palette.getPageBg());

        for (Stock stock : stocks) {
            AwtCardPanel card = new AwtCardPanel(new BorderLayout(12, 4), 14, 16);
            card.applyTheme(palette);

            Panel info = new Panel(new GridLayout(2, 1, 0, 2));
            info.setBackground(palette.getCardBg());
            Label nameLbl = new Label(stock.getName() + " [" + stock.getSymbol() + "]");
            nameLbl.setFont(ThemePalette.FONT_CARD_TITLE);
            nameLbl.setForeground(palette.getTextPrimary());
            nameLbl.setBackground(palette.getCardBg());

            Label detailLbl = new Label(stock.getPointCount() + " points · Latest: "
                    + CurrencyConverter.format(stock.getLatestPrice(), currency));
            detailLbl.setFont(ThemePalette.FONT_MONO_SMALL);
            detailLbl.setForeground(palette.getTextSecondary());
            detailLbl.setBackground(palette.getCardBg());

            info.add(nameLbl);
            info.add(detailLbl);

            Button delBtn = createStyledButton("Delete", palette.getLossBg(), palette.getLossRed());
            delBtn.addActionListener(e -> handleDeleteOne(stock));

            card.add(info, BorderLayout.CENTER);
            card.add(delBtn, BorderLayout.EAST);
            grid.add(card);
        }

        Panel gridWrap = new Panel(new BorderLayout());
        gridWrap.setBackground(palette.getPageBg());
        gridWrap.add(grid, BorderLayout.NORTH);
        add(gridWrap, BorderLayout.CENTER);

        validate();
        repaint();
    }

    private void handleDeleteOne(Stock stock) {
        Frame owner = findParentFrame();
        boolean confirmed = ConfirmDialog.showConfirm(
                owner,
                "Delete Stock \"" + stock.getName() + "\"?",
                "Are you sure you want to permanently delete \"" + stock.getName() + "\" ("
                        + stock.getPointCount() + " price points) and all related predictions and technical indicators from SQLite?",
                "Yes, Delete Stock",
                palette
        );
        if (confirmed) {
            try {
                stockService.deleteStock(stock.getName());
                navigator.onPortfolioChanged(null);
            } catch (DatabaseException ex) {
                showErrorDialog("Database Error", ex.getMessage());
            }
        }
    }

    private void handleClearAll(int totalCount) {
        Frame owner = findParentFrame();
        boolean confirmed = ConfirmDialog.showConfirm(
                owner,
                "Clear All Portfolio Stocks?",
                "This will permanently wipe all " + totalCount
                        + " stocks and their price history, prediction, and indicator records from SQLite.",
                "Clear Everything",
                palette
        );
        if (confirmed) {
            try {
                stockService.deleteAllStocks();
                navigator.onPortfolioChanged(null);
            } catch (DatabaseException ex) {
                showErrorDialog("Database Error", ex.getMessage());
            }
        }
    }
}
