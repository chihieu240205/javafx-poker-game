package server.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import server.game.PlayerSession;
import server.net.PokerServer;

public class ServerMonitorController {

    @FXML private ListView<String> clientListView;
    @FXML private ListView<String> logListView;
    @FXML private ListView<String> gameEventsListView;
    @FXML private ListView<String> leaderboardListView;
    @FXML private TableView<GameRecord> gameTable;
    @FXML private TableColumn<GameRecord, String> clientCol;
    @FXML private TableColumn<GameRecord, String> handCol;
    @FXML private TableColumn<GameRecord, String> betsCol;
    @FXML private TableColumn<GameRecord, String> outcomeCol;
    @FXML private TableColumn<GameRecord, String> netCol;
    @FXML private TableColumn<GameRecord, String> balanceCol;
    @FXML private Label connectedCountLabel;
    @FXML private Label statusValueLabel;
    @FXML private Label portValueLabel;
    @FXML private Button backButton;

    private PokerServer pokerServer;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Map<Long, String> clientStatuses = new HashMap<>();

    public void setServer(PokerServer server) {
        this.pokerServer = server;
        pokerServer.setMonitor(this);
        setPort(server.getPort());
        setRunningState(server.isRunning());
        updateClientCount(0);
        log("Server monitor attached.");
    }

    @FXML
    public void initialize() {
        backButton.setOnAction(e -> goBack());

        if (gameTable != null) {
            clientCol.setCellValueFactory(new PropertyValueFactory<>("clientId"));
            handCol.setCellValueFactory(new PropertyValueFactory<>("handNumber"));
            betsCol.setCellValueFactory(new PropertyValueFactory<>("bets"));
            outcomeCol.setCellValueFactory(new PropertyValueFactory<>("outcome"));
            netCol.setCellValueFactory(new PropertyValueFactory<>("net"));
            balanceCol.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        }
    }

    public void log(String message) {
        Platform.runLater(() -> {
            logListView.getItems().add(stamp(message));
            logListView.scrollTo(logListView.getItems().size() - 1);
        });
    }

    public void addGameEvent(String event) {
        Platform.runLater(() -> {
            gameEventsListView.getItems().add(stamp(event));
            gameEventsListView.scrollTo(gameEventsListView.getItems().size() - 1);
        });
    }

    public void updateClientCount(int count) {
        Platform.runLater(() -> {
            connectedCountLabel.setText(String.valueOf(count));
        });
    }

    public void replaceClients(List<String> clientDescriptions) {
        Platform.runLater(() -> {
            clientListView.getItems().setAll(clientDescriptions);
            if (!clientDescriptions.isEmpty()) {
                clientListView.scrollTo(clientDescriptions.size() - 1);
            }
        });
    }

    public void setRunningState(boolean running) {
        Platform.runLater(() -> {
            statusValueLabel.setText(running ? "Running" : "Stopped");
            statusValueLabel.setTextFill(running ? Color.LIMEGREEN : Color.CRIMSON);
        });
    }

    public void setPort(int port) {
        Platform.runLater(() -> portValueLabel.setText(String.valueOf(port)));
    }

    private void goBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/server/ui/server_intro.fxml")
            );
            Scene scene = new Scene(loader.load());

            ServerIntroController introController = loader.getController();
            introController.attachExistingServer(pokerServer);

            stage.setScene(scene);
            stage.setTitle("3-Card Poker Server");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String stamp(String message) {
        return "[" + LocalTime.now().format(timeFormatter) + "] " + message;
    }

    public void upsertClientStatus(long clientId, String status) {
        Platform.runLater(() -> {
            clientStatuses.put(clientId, status);
            refreshClientList();
        });
    }

    public void removeClientStatus(long clientId) {
        Platform.runLater(() -> {
            clientStatuses.remove(clientId);
            refreshClientList();
        });
    }

    private void refreshClientList() {
        clientListView.getItems().setAll(
                clientStatuses.entrySet().stream()
                        .map(e -> "Client " + e.getKey() + " — " + e.getValue())
                        .sorted()
                        .collect(Collectors.toList())
        );
        updateClientCount(clientStatuses.size());
    }

    public void updateLeaderboard(List<PlayerSession> topPlayers) {
        Platform.runLater(() -> {
            leaderboardListView.getItems().clear();

            int rank = 1;
            for (PlayerSession p : topPlayers) {
                leaderboardListView.getItems().add(
                        String.format(
                            "%d. Client %d  |  Won: %+d  |  Balance: %d",
                            rank,
                            p.getSessionId(),
                            p.getTotalMoneyWon(),
                            p.getBalance()
                        )
                );
                rank++;
            }
        });
    }

    public void addGameRecord(GameRecord record) {
        Platform.runLater(() -> {
            gameTable.getItems().add(record);
            gameTable.scrollTo(gameTable.getItems().size() - 1);
        });
    }

    public static class GameRecord {
        private final String clientId;
        private final String handNumber;
        private final String bets;
        private final String outcome;
        private final String net;
        private final String balanceAfter;

        public GameRecord(long clientId,
                          int handNumber,
                          int ante,
                          int pairPlus,
                          int play,
                          String outcome,
                          int net,
                          int balanceAfter) {
            this.clientId = String.valueOf(clientId);
            this.handNumber = "#" + handNumber;
            this.bets = String.format("Ante $%d | Pair+ $%d | Play $%d", ante, pairPlus, play);
            this.outcome = outcome;
            this.net = (net >= 0 ? "+" : "-") + "$" + Math.abs(net);
            this.balanceAfter = "$" + balanceAfter;
        }

        public String getClientId() { return clientId; }
        public String getHandNumber() { return handNumber; }
        public String getBets() { return bets; }
        public String getOutcome() { return outcome; }
        public String getNet() { return net; }
        public String getBalanceAfter() { return balanceAfter; }
    }
}
