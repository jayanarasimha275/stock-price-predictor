package ui;

import model.User;
import repository.SettingsRepository;
import service.*;
import util.ThemePalette;

import java.awt.*;

/**
 * Coordinates services and authenticated frame navigation between LoginFrame,
 * RegisterFrame, MainFrame (User), and AdminDashboard (Admin).
 */
public class AppContext {

    private static final String PREF_THEME = "theme";

    private final StockService stockService;
    private final PredictionService predictionService;
    private final IndicatorService indicatorService;
    private final HistoryService historyService;
    private final SettingsRepository settingsRepository;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AppContext(StockService stockService,
                      PredictionService predictionService,
                      IndicatorService indicatorService,
                      HistoryService historyService,
                      SettingsRepository settingsRepository,
                      AuthenticationService authenticationService,
                      UserService userService) {
        this.stockService = stockService;
        this.predictionService = predictionService;
        this.indicatorService = indicatorService;
        this.historyService = historyService;
        this.settingsRepository = settingsRepository;
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    public StockService getStockService() { return stockService; }
    public PredictionService getPredictionService() { return predictionService; }
    public IndicatorService getIndicatorService() { return indicatorService; }
    public HistoryService getHistoryService() { return historyService; }
    public SettingsRepository getSettingsRepository() { return settingsRepository; }
    public AuthenticationService getAuthenticationService() { return authenticationService; }
    public UserService getUserService() { return userService; }

    public ThemePalette getSavedPalette() {
        String savedTheme = settingsRepository.getSetting(PREF_THEME, "dark");
        return "light".equalsIgnoreCase(savedTheme) ? ThemePalette.LIGHT : ThemePalette.DARK;
    }

    public void showLoginFrame(Frame currentFrame, String noticeMessage) {
        LoginFrame login = new LoginFrame(this, noticeMessage);
        login.setVisible(true);
        if (currentFrame != null) {
            currentFrame.dispose();
        }
        login.checkInitialAdminSetup();
    }

    public void showRegisterFrame(Frame currentFrame) {
        RegisterFrame register = new RegisterFrame(this);
        register.setVisible(true);
        if (currentFrame != null) {
            currentFrame.dispose();
        }
    }

    public void openWorkspaceForUser(User user, Frame currentFrame) {
        if (user == null || !user.isActive()) {
            return;
        }
        if (user.isAdmin()) {
            AdminDashboard adminDashboard = new AdminDashboard(this, user);
            adminDashboard.setVisible(true);
        } else {
            MainFrame mainFrame = new MainFrame(
                    stockService,
                    predictionService,
                    indicatorService,
                    historyService,
                    settingsRepository,
                    user,
                    () -> showLoginFrame(null, "You have signed out cleanly.")
            );
            mainFrame.setVisible(true);
        }
        if (currentFrame != null) {
            currentFrame.dispose();
        }
    }
}
