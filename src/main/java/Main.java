import database.DatabaseSeeder;
import database.SQLiteConnection;
import repository.HistoryRepository;
import repository.IndicatorRepository;
import repository.PredictionRepository;
import repository.SettingsRepository;
import repository.StockRepository;
import repository.UserRepository;
import service.AuthenticationService;
import service.HistoryService;
import service.IndicatorService;
import service.PredictionService;
import service.StockService;
import service.UserService;
import ui.AppContext;
import ui.PreviewServer;
import util.DatabaseException;

import java.awt.EventQueue;

/**
 * Application entry point for Stock Price Predictor (Pure Java AWT + JDBC + SQLite).
 * Launches the secure LoginFrame with role-based redirection to User or Admin dashboards.
 * Course: CSE 213 – Advanced Java Programming, SRM University AP.
 */
public class Main {

    public static void main(String[] args) {
        int previewPort = parsePreviewPort(args);
        if (previewPort > 0) {
            PreviewServer.start(previewPort);
        }

        try {
            // Initialize SQLite JDBC schema & run non-destructive migrations
            SQLiteConnection.initializeSchema();

            HistoryRepository historyRepo = new HistoryRepository();
            StockRepository stockRepo = new StockRepository(historyRepo);
            PredictionRepository predictionRepo = new PredictionRepository();
            IndicatorRepository indicatorRepo = new IndicatorRepository();
            SettingsRepository settingsRepo = new SettingsRepository();
            UserRepository userRepo = new UserRepository();

            PredictionService predictionService = new PredictionService(predictionRepo);
            IndicatorService indicatorService = new IndicatorService(indicatorRepo, predictionService);
            HistoryService historyService = new HistoryService(historyRepo);
            StockService stockService = new StockService(stockRepo, predictionService, indicatorService);
            AuthenticationService authService = new AuthenticationService(userRepo);
            UserService userService = new UserService(userRepo);

            // Seed initial portfolio only if first-ever run (never deletes existing records)
            DatabaseSeeder.seedIfFirstRun(stockService, settingsRepo);

            AppContext appContext = new AppContext(
                    stockService,
                    predictionService,
                    indicatorService,
                    historyService,
                    settingsRepo,
                    authService,
                    userService
            );

            EventQueue.invokeLater(() -> appContext.showLoginFrame(null, null));
        } catch (DatabaseException ex) {
            System.err.println("Fatal database initialization error: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static int parsePreviewPort(String[] args) {
        if (args == null) return -1;
        for (String arg : args) {
            if (arg != null && arg.startsWith("--preview-port=")) {
                try {
                    return Integer.parseInt(arg.substring("--preview-port=".length()).trim());
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }
}


