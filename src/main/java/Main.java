import database.DatabaseSeeder;
import database.SQLiteConnection;
import repository.HistoryRepository;
import repository.IndicatorRepository;
import repository.PredictionRepository;
import repository.SettingsRepository;
import repository.StockRepository;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import ui.MainFrame;
import util.DatabaseException;

import java.awt.EventQueue;

/**
 * Application entry point for Stock Price Predictor (Pure Java AWT + JDBC + SQLite).
 * Course: CSE 213 – Advanced Java Programming, SRM University AP.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Initialize SQLite JDBC schema
            SQLiteConnection.initializeSchema();

            HistoryRepository historyRepo = new HistoryRepository();
            StockRepository stockRepo = new StockRepository(historyRepo);
            PredictionRepository predictionRepo = new PredictionRepository();
            IndicatorRepository indicatorRepo = new IndicatorRepository();
            SettingsRepository settingsRepo = new SettingsRepository();

            PredictionService predictionService = new PredictionService(predictionRepo);
            IndicatorService indicatorService = new IndicatorService(indicatorRepo, predictionService);
            HistoryService historyService = new HistoryService(historyRepo);
            StockService stockService = new StockService(stockRepo, predictionService, indicatorService);

            // Seed initial portfolio only if first-ever run
            DatabaseSeeder.seedIfFirstRun(stockService, settingsRepo);

            EventQueue.invokeLater(() -> {
                MainFrame frame = new MainFrame(
                        stockService,
                        predictionService,
                        indicatorService,
                        historyService,
                        settingsRepo
                );
                frame.setVisible(true);
            });
        } catch (DatabaseException ex) {
            System.err.println("Fatal database initialization error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
