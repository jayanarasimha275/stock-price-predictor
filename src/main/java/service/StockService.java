package service;

import model.Stock;
import repository.StockRepository;
import util.DatabaseException;
import util.ValidationException;
import util.ValidationUtil;

import java.util.List;

/**
 * Facade service coordinating Stock CRUD, validation, and automatic Prediction/Indicator persistence.
 */
public class StockService {

    private final StockRepository stockRepository;
    private final PredictionService predictionService;
    private final IndicatorService indicatorService;

    public StockService(StockRepository stockRepository,
                        PredictionService predictionService,
                        IndicatorService indicatorService) {
        this.stockRepository = stockRepository;
        this.predictionService = predictionService;
        this.indicatorService = indicatorService;
    }

    public List<Stock> getAllStocks() throws DatabaseException {
        return stockRepository.findAll();
    }

    public boolean existsByName(String name) throws DatabaseException {
        return stockRepository.findByNameIgnoreCase(name).isPresent();
    }

    /**
     * Overloaded addStock without notes.
     */
    public Stock addStock(String name, String symbol, List<Double> prices)
            throws ValidationException, DatabaseException {
        return addStock(name, symbol, prices, "User imported");
    }

    /**
     * Overloaded addStock with notes, validating inputs and persisting stock, history, prediction, and default indicators.
     */
    public Stock addStock(String name, String symbol, List<Double> prices, String notes)
            throws ValidationException, DatabaseException {
        List<Stock> existing = stockRepository.findAll();
        ValidationUtil.validateStockInput(name, symbol, prices, existing, null);

        Stock newStock = new Stock(name.trim(), symbol.trim().toUpperCase(), notes, prices);
        Stock saved = stockRepository.save(newStock);
        predictionService.computeAndPersistPrediction(saved);
        indicatorService.computeAndPersist(saved, 5, 5, 3, 14, 12, 26, 9);
        return saved;
    }

    public void syncDerivedAnalytics(Stock stock) throws DatabaseException {
        predictionService.computeAndPersistPrediction(stock);
        indicatorService.computeAndPersist(stock, 5, 5, 3, 14, 12, 26, 9);
    }

    public void deleteStock(String name) throws DatabaseException {
        stockRepository.deleteByName(name);
    }

    public void deleteAllStocks() throws DatabaseException {
        stockRepository.deleteAll();
    }
}
