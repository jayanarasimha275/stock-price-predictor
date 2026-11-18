package service;

import model.PriceChangeSummary;
import model.Stock;
import repository.HistoryRepository;
import util.DatabaseException;
import util.ValidationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service handling price history updates, daily/overall price changes, and gainers/losers ranking.
 */
public class HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryService() {
        this.historyRepository = new HistoryRepository();
    }

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public PriceChangeSummary calculatePriceChange(Stock stock) {
        List<Double> prices = stock.getHistory();
        if (prices.isEmpty()) {
            return new PriceChangeSummary(stock.getName(), stock.getSymbol(), 0, 0, 0, 0, 0, 0);
        }
        int n = prices.size();
        double first = prices.get(0);
        double latest = prices.get(n - 1);
        double previous = n >= 2 ? prices.get(n - 2) : latest;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double p : prices) {
            if (p < min) min = p;
            if (p > max) max = p;
        }
        return new PriceChangeSummary(stock.getName(), stock.getSymbol(), first, latest, previous, min, max, n);
    }

    public List<PriceChangeSummary> getRankedSummaries(List<Stock> stocks, boolean sortByDailyChange) {
        List<PriceChangeSummary> summaries = new ArrayList<>();
        for (Stock s : stocks) {
            summaries.add(calculatePriceChange(s));
        }
        summaries.sort(Comparator.comparingDouble(
                (PriceChangeSummary s) -> sortByDailyChange ? s.getDailyPercentChange() : s.getPercentChange()
        ).reversed());
        return summaries;
    }

    public Stock appendPrice(Stock stock, double newPrice) throws ValidationException, DatabaseException {
        if (!Double.isFinite(newPrice) || newPrice <= 0) {
            throw new ValidationException("Price must be a finite positive number (> 0).");
        }
        List<Double> updated = new ArrayList<>(stock.getHistory());
        updated.add(newPrice);
        stock.setHistory(updated);
        historyRepository.replaceHistory(stock.getId(), updated);
        return stock;
    }

    public Stock updatePriceAt(Stock stock, int index, double newPrice) throws ValidationException, DatabaseException {
        if (!Double.isFinite(newPrice) || newPrice <= 0) {
            throw new ValidationException("Updated price must be a finite positive number (> 0).");
        }
        List<Double> updated = new ArrayList<>(stock.getHistory());
        if (index < 0 || index >= updated.size()) {
            throw new ValidationException("Selected index is out of bounds.");
        }
        updated.set(index, newPrice);
        stock.setHistory(updated);
        historyRepository.replaceHistory(stock.getId(), updated);
        return stock;
    }

    public Stock deletePriceAt(Stock stock, int index) throws ValidationException, DatabaseException {
        List<Double> current = stock.getHistory();
        if (current.size() <= 2) {
            throw new ValidationException("Cannot delete price: A stock must keep at least 2 price points.");
        }
        if (index < 0 || index >= current.size()) {
            throw new ValidationException("Selected index is out of bounds.");
        }
        List<Double> updated = new ArrayList<>(current);
        updated.remove(index);
        stock.setHistory(updated);
        historyRepository.replaceHistory(stock.getId(), updated);
        return stock;
    }
}
