package database;

import model.Stock;
import repository.SettingsRepository;
import service.StockService;
import util.DatabaseException;
import util.ValidationException;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds initial demo stocks into SQLite ONLY on first launch if database has never been seeded,
 * and provides an explicit method to load sample data on demand.
 */
public final class DatabaseSeeder {

    private static final String SEEDED_FLAG_KEY = "initial_seeded_v1";

    private DatabaseSeeder() {
    }

    public static void seedIfFirstRun(StockService stockService, SettingsRepository settingsRepo)
            throws DatabaseException {
        String alreadySeeded = settingsRepo.getSetting(SEEDED_FLAG_KEY, "false");
        if ("true".equalsIgnoreCase(alreadySeeded)) {
            return;
        }

        List<Stock> existing = stockService.getAllStocks();
        if (existing.isEmpty()) {
            insertSamplePortfolio(stockService);
        }
        settingsRepo.setSetting(SEEDED_FLAG_KEY, "true");
    }

    public static void loadSamplePortfolio(StockService stockService) throws DatabaseException {
        insertSamplePortfolio(stockService);
        SettingsRepository settingsRepo = new SettingsRepository();
        settingsRepo.setSetting(SEEDED_FLAG_KEY, "true");
    }

    private static void insertSamplePortfolio(StockService stockService) throws DatabaseException {
        List<Stock> samples = Arrays.asList(
                new Stock("RELIANCE", "RELIANCE", "Sample Indian Conglomerate",
                        Arrays.asList(2410.0, 2435.0, 2420.0, 2465.0, 2480.0, 2475.0, 2510.0, 2535.0, 2560.0, 2590.0)),
                new Stock("TCS", "TCS", "Sample IT Services Leader",
                        Arrays.asList(3500.0, 3520.0, 3510.0, 3540.0, 3530.0, 3565.0, 3580.0, 3610.0, 3640.0)),
                new Stock("INFY", "INFY", "Sample Tech Firm (Downtrend)",
                        Arrays.asList(1520.0, 1505.0, 1490.0, 1480.0, 1495.0, 1470.0, 1460.0, 1450.0, 1435.0))
        );

        for (Stock s : samples) {
            try {
                if (!stockService.existsByName(s.getName())) {
                    stockService.addStock(s.getName(), s.getSymbol(), s.getHistory(), s.getNotes());
                }
            } catch (ValidationException ignored) {
                // Skip if already exists
            }
        }
    }
}
