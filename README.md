# Stock Price Predictor — Pure Java AWT Desktop Application

**Course:** CSE 213 – Advanced Java Programming  
**Institution:** SRM University AP

## Technology Stack
- **Language:** Java 17
- **GUI Toolkit:** Pure Java AWT (`java.awt.*`, `java.awt.event.*`, `Graphics2D`, `java.awt.print.*`) — zero Swing, zero JavaFX, zero browser dependencies
- **Database:** SQLite via JDBC (`org.xerial:sqlite-jdbc`)
- **Build & Execution:** Apache Maven (`maven-compiler-plugin`, `exec-maven-plugin`)

## Running the Application
```bash
mvn clean compile
mvn exec:java
```

## Database Tables (`stock_predictor.db`)
- `stocks` — Stock metadata (`id`, `name`, `symbol`, `notes`, `created_at`)
- `price_history` — Chronological price entries linked via `stock_id` (`ON DELETE CASCADE`)
- `predictions` — OLS regression slope, predicted next price, trend direction, accuracy %
- `technical_indicators` — Persisted SMA, EMA, Momentum, RSI, and MACD calculations
- `app_settings` — Persisted user preferences (`currency`, `theme`, `initial_seeded_v1`)

## Modules Implemented
1. **All Stocks Overview** (`DashboardPanel`, `StockCard`)
2. **Add / Load Stock** (`AddStockPanel`) — Paste list, one-by-one, CSV import (`FileDialog`)
3. **Predict Next Price** (`PredictionPanel`) — OLS linear regression slope + trajectory chart
4. **Technical Indicators** (`IndicatorPanel`) — SMA, EMA, Momentum, RSI, MACD + overlay/oscillator charts
5. **Price History** (`HistoryPanel`) — Historical curve, append, edit, and delete records
6. **Prediction Accuracy** (`AccuracyPanel`) — Walk-forward train/test backtesting
7. **Forecast Report** (`ForecastPanel`) — Filterable table, CSV export, native `PrinterJob` print support
8. **Price Change** (`PriceChangePanel`) — Daily & inception-to-date gainers and losers
9. **Compare Two Stocks** (`ComparePanel`) — Normalized Base-100 dual-stock comparison
10. **Delete Stock** (`DeleteStockPanel`) — Modal AWT `Dialog` confirmation & cascading SQLite delete
