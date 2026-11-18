# Stock Price Predictor — Pure Java AWT Desktop Application

**Course:** CSE 213 – Advanced Java Programming  
**Institution:** SRM University AP

## Technology Stack
- **Language:** Java 17
- **GUI Toolkit:** Pure Java AWT (`java.awt.*`, `java.awt.event.*`, `Graphics2D`, `java.awt.print.*`) — zero Swing, zero JavaFX, zero browser dependencies
- **Database:** SQLite via JDBC (`org.xerial:sqlite-jdbc`)
- **Build & Execution:** Apache Maven (`maven-compiler-plugin`, `exec-maven-plugin`)

## Running the Application

### Option A: Apache Maven
```bash
mvn clean compile
mvn exec:java
```

### Option B: Direct `javac` / `java` (with SQLite JDBC driver in classpath)
```bash
mkdir -p out
javac -encoding UTF-8 -cp "path/to/sqlite-jdbc-3.46.1.3.jar" -d out $(find src/main/java -name "*.java")
java -cp "out:path/to/sqlite-jdbc-3.46.1.3.jar:path/to/slf4j-api-2.0.16.jar:path/to/slf4j-nop-2.0.16.jar" Main
```

## Database Tables (`stock_predictor.db`)
- `users` — User & Admin accounts (`id`, `full_name`, `username`, `email`, `password_hash`, `password_salt`, `role`, `status`, `created_at`)
- `stocks` — Stock metadata (`id`, `name`, `symbol`, `notes`, `created_at`)
- `price_history` — Chronological price entries linked via `stock_id` (`ON DELETE CASCADE`)
- `predictions` — OLS regression slope, predicted next price, trend direction, accuracy %
- `technical_indicators` — Persisted SMA, EMA, Momentum, RSI, and MACD calculations
- `app_settings` — Persisted user preferences & migration version (`currency`, `theme`, `initial_seeded_v1`, `schema_version`)

## Security & Role-Based Access
- **PBKDF2WithHmacSHA256** (`65,536` iterations, random 128-bit salts, constant-time hash comparison)
- **First-Time Initial Admin Setup:** When zero `ADMIN` accounts exist in SQLite, `AdminSetupDialog` prompts for initial administrator creation without any hardcoded password and permanently locks once an admin exists.
- **Role-Based Routing:** `USER` accounts navigate to `MainFrame`; `ADMIN` accounts navigate to `AdminDashboard`.

## Modules Implemented
### User Predictor Modules (`MainFrame`)
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

### Administrator Console (`AdminDashboard`)
1. **Dashboard Overview** — Total users, total stocks, total predictions, recent registrations
2. **User Management** (`UserManagementPanel`) — Search, view, add, edit, activate/deactivate, role assignment, delete
3. **Stock Management** (`AdminStockPanel`) — View, add, edit stock details & prices, delete, inspect history
4. **Prediction Management** (`AdminPredictionPanel`) — Audit prediction records, per-stock history, chart details
5. **Admin Settings** — Profile inspector, PBKDF2 password change, and logout

