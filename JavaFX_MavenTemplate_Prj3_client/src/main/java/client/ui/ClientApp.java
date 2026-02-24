package client.ui;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import client.model.CardViewModel;
import client.model.ClientGameState;
import client.net.PokerClientConnection;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import shared.GamePhase;
import shared.PokerInfo;

public class ClientApp extends Application {

    private Stage primaryStage;
    private final Map<ClientScene, Scene> scenes = new EnumMap<>(ClientScene.class);
    private PokerClientConnection connection;
    private ClientGameState gameState;
    private GameController gameController;
    private boolean newLookOn = false;
    private String newLookCss;
    private String classicCss;
    private final AudioManager audioManager = AudioManager.getInstance();
    private Scene loadingScene;
    private StackPane loadingRoot;
    private Label loadingLabel;
    private final Random loadingHumor = new Random();
    private static final List<String> GAMEPLAY_LOADING_LINES = List.of(
            "Shuffling sassier cards...",
            "Polishing chips with sarcasm...",
            "Dealer stretching their poker face...",
            "Convincing RNG to be nice...",
            "Greasing the shuffle machine..."
    );
    private static final List<String> RESULT_LOADING_LINES = List.of(
            "Counting winnings, creative math optional...",
            "Consulting the drama llama about payouts...",
            "Checking if the dealer owes you coffee...",
            "Adding zeros for dramatic effect...",
            "Balancing the books with jazz hands..."
    );
    private static final List<String> DEFAULT_LOADING_LINES = List.of(
            "Setting the stage for the next bluff...",
            "Buffering table banter...",
            "Warming up the dealer's eyebrow raise...",
            "Teaching the deck new tricks...",
            "Aligning the stars for better luck..."
    );

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        this.gameState = new ClientGameState();
        this.connection = new PokerClientConnection();

        connection.setListener(this::onInfoReceived);

        loadScenes();

        primaryStage.setTitle("Networked 3 Card Poker - Client");
        maximizeToScreen(primaryStage);
        audioManager.playBackground();
        showScene(ClientScene.WELCOME, false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void loadScenes() throws IOException {
        classicCss = getClass()
                .getResource("/client/ui/classic.css")
                .toExternalForm();

        newLookCss = getClass()
                .getResource("/client/ui/newlook.css")
                .toExternalForm();

        FXMLLoader welcomeLoader =
                new FXMLLoader(getClass().getResource("/client/ui/welcome.fxml"));
        Parent welcomeRoot = welcomeLoader.load();
        WelcomeController welcomeController = welcomeLoader.getController();
        welcomeController.setClientApp(this);
        Scene welcomeScene = new Scene(welcomeRoot, 900, 600);
        applyTheme(welcomeScene);
        scenes.put(ClientScene.WELCOME, welcomeScene);

        FXMLLoader gameLoader =
                new FXMLLoader(getClass().getResource("/client/ui/gameplay.fxml"));
        Parent gameRoot = gameLoader.load();
        this.gameController = gameLoader.getController();
        this.gameController.setClientApp(this, gameState);

        Scene gameScene = new Scene(gameRoot, 900, 700);
        applyTheme(gameScene);
        scenes.put(ClientScene.GAMEPLAY, gameScene);

        FXMLLoader resultLoader =
                new FXMLLoader(getClass().getResource("/client/ui/result.fxml"));
        Parent resultRoot = resultLoader.load();
        ResultController resultController = resultLoader.getController();
        resultController.setClientApp(this);

        Scene resultScene = new Scene(resultRoot, 900, 700);
        resultScene.setUserData(resultLoader);
        applyTheme(resultScene);
        scenes.put(ClientScene.RESULT, resultScene);

        createLoadingScene();
    }

    private void maximizeToScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
    }

    private void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = newLookOn ? newLookCss : classicCss;
        if (css != null) {
            scene.getStylesheets().add(css);
        }
        if (scene == loadingScene && loadingRoot != null) {
            loadingRoot.getStyleClass().removeAll("loading-classic", "loading-newlook");
            loadingRoot.getStyleClass().add(newLookOn ? "loading-newlook" : "loading-classic");
        }
    }

    private void createLoadingScene() {
        loadingRoot = new StackPane();
        loadingRoot.getStyleClass().add("loading-screen");
        loadingLabel = new Label("Spinning up the table...");
        loadingLabel.getStyleClass().add("loading-text");
        StackPane.setAlignment(loadingLabel, Pos.CENTER);
        loadingRoot.getChildren().add(loadingLabel);
        loadingScene = new Scene(loadingRoot, 900, 700);
        applyTheme(loadingScene);
    }

    private void showWithLoading(ClientScene target, Scene actualScene) {
        if (loadingScene == null || loadingRoot == null) {
            primaryStage.setScene(actualScene);
            return;
        }
        updateLoadingMessage(target);
        loadingRoot.setOpacity(0);
        primaryStage.setScene(loadingScene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(320), loadingRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        PauseTransition pause = new PauseTransition(Duration.millis(300));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(320), loadingRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);
        fadeOut.setOnFinished(evt -> {
            primaryStage.setScene(actualScene);
            Parent root = actualScene.getRoot();
            if (root != null) {
                root.setOpacity(0.92);
                FadeTransition settle = new FadeTransition(Duration.millis(200), root);
                settle.setFromValue(0.92);
                settle.setToValue(1);
                settle.setInterpolator(Interpolator.EASE_BOTH);
                settle.play();
            }
        });

        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
        sequence.play();
    }

    private void updateLoadingMessage(ClientScene target) {
        if (loadingLabel == null) return;
        String message;
        switch (target) {
            case RESULT:
                message = pickLoadingLine(RESULT_LOADING_LINES);
                break;
            case GAMEPLAY:
                message = pickLoadingLine(GAMEPLAY_LOADING_LINES);
                break;
            default:
                message = pickLoadingLine(DEFAULT_LOADING_LINES);
        }
        loadingLabel.setText(message);
    }

    private String pickLoadingLine(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "Loading...";
        }
        return options.get(loadingHumor.nextInt(options.size()));
    }


    public void toggleNewLook() {
        newLookOn = !newLookOn;
        scenes.values().forEach(this::applyTheme);
        applyTheme(loadingScene);
        CardViewModel.setUseNewBack(newLookOn);
    }


    public void showScene(ClientScene which) {
        showScene(which, true);
    }

    public void showScene(ClientScene which, boolean animate) {
        Scene scene = scenes.get(which);
        if (scene == null) return;
        if (animate) {
            showWithLoading(which, scene);
        } else {
            primaryStage.setScene(scene);
        }
    }

    public PokerClientConnection getConnection() {
        return connection;
    }

    public ClientGameState getGameState() {
        return gameState;
    }

    public void showResult(PokerInfo info) {
        Scene scene = scenes.get(ClientScene.RESULT);
        FXMLLoader loader = (FXMLLoader) scene.getUserData();
        ResultController c = loader.getController();
        c.showResult(info);
        showScene(ClientScene.RESULT);
    }

    private void onInfoReceived(PokerInfo info) {
        Platform.runLater(() -> {

            PokerInfo last = gameState.getLastInfo();

            if (last != null &&
                    info.getTimestamp() != null &&
                    info.getTimestamp().equals(last.getTimestamp())) {
                return;
            }

            if (info.getTotalBalance() > 0) {
                gameState.setBalance(info.getTotalBalance());
            }

            GamePhase phase = info.getGamePhase();
            if (phase == null) phase = GamePhase.WAITING_BETS;

            switch (phase) {
                case DEALT:
                case AWAITING_ACTION:
                    gameController.updateView(info);
                    showScene(ClientScene.GAMEPLAY, false);
                    break;

                case RESOLVED:
                    gameController.updateView(info);
                    if (info.getRoundResult() != null) {
                        gameController.showEndOfRoundPopup(info);
                    }
                    break;

                case WAITING_BETS:
                    gameController.updateView(info);
                    break;
            }

            gameState.setLastInfo(info);
        });
    }



    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void resetGameplayUI() {
        if (gameController != null) {
            gameController.resetUI();
        }
    }

    public void shutdown() {
        if (connection != null) {
            connection.disconnect();
        }
        audioManager.dispose();
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
