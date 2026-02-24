import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class JavaFXTemplate extends Application {

    private Stage stage;
    private Scene introScene;
    private Scene dashboardScene;

    private TextField portField;
    private Label statusLabel;
    private Label portLabel;
    private Label clientCountLabel;
    private Label handStatusLabel;
    private Button turnOnButton;
    private Button turnOffButton;

    private TextField clientNameField;
    private TextField betField;
    private ChoiceBox<String> outcomeChoice;

    private ListView<String> eventListView;
    private ObservableList<String> eventItems;

    private boolean serverRunning = false;
    private int clientCount = 0;
    private int handCounter = 0;
    private String currentPort = "";

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Random random = new Random();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("3-Card Poker Server");

        eventItems = FXCollections.observableArrayList();
        introScene = buildIntroScene();
        dashboardScene = buildDashboardScene();

        stage.setScene(introScene);
        stage.show();
    }

    private Scene buildIntroScene() {
        Label title = new Label("Server Control");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label prompt = new Label("Enter the port number and start the server.");
        prompt.setStyle("-fx-font-size: 14px;");

        portField = new TextField("7777");
        portField.setPromptText("Port number");
        portField.setMaxWidth(200);

        Button startButton = new Button("Start Server");
        startButton.setDefaultButton(true);
        startButton.setPrefWidth(200);

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.CRIMSON);

        startButton.setOnAction(e -> {
            String portText = portField.getText().trim();
            if (isValidPort(portText)) {
                currentPort = portText;
                startServer();
                stage.setScene(dashboardScene);
            } else {
                errorLabel.setText("Please enter a valid port (1-65535).");
            }
        });

        VBox root = new VBox(12, title, prompt, portField, startButton, errorLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1f3b4d, #182633); -fx-text-fill: white;");
        title.setTextFill(Color.WHITE);
        prompt.setTextFill(Color.LIGHTGRAY);

        return new Scene(root, 720, 520);
    }

    private Scene buildDashboardScene() {
        statusLabel = new Label("Stopped");
        statusLabel.setTextFill(Color.CRIMSON);
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        portLabel = new Label("-");
        portLabel.setStyle("-fx-font-size: 14px;");

        clientCountLabel = new Label("0");
        clientCountLabel.setStyle("-fx-font-size: 14px;");

        handStatusLabel = new Label("No hand in progress.");
        handStatusLabel.setWrapText(true);

        turnOnButton = new Button("Turn On");
        turnOffButton = new Button("Turn Off");
        turnOffButton.setDisable(true);

        turnOnButton.setOnAction(e -> startServer());
        turnOffButton.setOnAction(e -> stopServer());

        HBox statusRow = new HBox(12,
                labeledBox("Status", statusLabel),
                labeledBox("Port", portLabel),
                labeledBox("Clients", clientCountLabel),
                turnOnButton,
                turnOffButton);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        eventListView = new ListView<>(eventItems);
        eventListView.setPlaceholder(new Label("No events yet."));

        VBox left = new VBox(14, statusRow, createClientControls(), createGameRecorder(), handStatusLabel);
        left.setPadding(new Insets(16));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setLeft(left);
        root.setCenter(eventListView);
        BorderPane.setMargin(eventListView, new Insets(0, 0, 0, 16));

        root.setStyle("-fx-background-color: #0f1722; -fx-text-fill: white;");

        return new Scene(root, 960, 600);
    }

    private VBox createClientControls() {
        Button clientJoinButton = new Button("Client Joins");
        Button clientLeaveButton = new Button("Client Drops");

        clientJoinButton.setOnAction(e -> {
            clientCount++;
            updateStatusLabels();
            String clientName = "Client-" + (100 + random.nextInt(900));
            addEvent(clientName + " connected.");
        });

        clientLeaveButton.setOnAction(e -> {
            if (clientCount > 0) {
                clientCount--;
                updateStatusLabels();
                addEvent("A client disconnected.");
            } else {
                addEvent("No clients to drop.");
            }
        });

        HBox row = new HBox(10, clientJoinButton, clientLeaveButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, new Label("Connections"), row);
    }

    private VBox createGameRecorder() {
        clientNameField = new TextField("Client-101");
        betField = new TextField("25");
        outcomeChoice = new ChoiceBox<>(FXCollections.observableArrayList("Win", "Loss", "Push"));
        outcomeChoice.setValue("Win");

        Button recordButton = new Button("Record Game Result");
        recordButton.setOnAction(e -> recordGame());

        Button nextHandButton = new Button("Client Requests Another Hand");
        nextHandButton.setOnAction(e -> {
            handCounter++;
            handStatusLabel.setText("Client is ready for hand #" + handCounter);
            addEvent("Client requested another hand (hand #" + handCounter + ").");
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(6, 6, 6, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(new Label("Client ID"), 0, 0);
        grid.add(clientNameField, 1, 0);
        grid.add(new Label("Bet"), 0, 1);
        grid.add(betField, 1, 1);
        grid.add(new Label("Result"), 0, 2);
        grid.add(outcomeChoice, 1, 2);

        VBox box = new VBox(8, new Label("Game Tracker"), grid, recordButton, nextHandButton);
        return box;
    }

    private HBox labeledBox(String title, Label value) {
        Label header = new Label(title + ":");
        header.setTextFill(Color.LIGHTGRAY);
        header.setStyle("-fx-font-size: 12px;");

        HBox box = new HBox(4, header, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void startServer() {
        if (serverRunning) {
            addEvent("Server already running on port " + currentPort + ".");
            return;
        }
        if (!isValidPort(currentPort)) {
            addEvent("Cannot start: invalid port.");
            return;
        }
        serverRunning = true;
        updateStatusLabels();
        addEvent("Server started on port " + currentPort + ".");
    }

    private void stopServer() {
        if (!serverRunning) {
            addEvent("Server already stopped.");
            return;
        }
        serverRunning = false;
        handStatusLabel.setText("No hand in progress.");
        updateStatusLabels();
        addEvent("Server stopped.");
    }

    private void recordGame() {
        String clientId = clientNameField.getText().trim();
        String betText = betField.getText().trim();
        if (clientId.isEmpty()) {
            clientId = "Client-" + (100 + random.nextInt(900));
        }
        double betAmount;
        try {
            betAmount = Double.parseDouble(betText);
        } catch (NumberFormatException ex) {
            addEvent("Invalid bet amount. Please enter a number.");
            return;
        }

        String result = outcomeChoice.getValue();
        double delta = 0;
        switch (result) {
            case "Win":
                delta = betAmount;
                break;
            case "Loss":
                delta = -betAmount;
                break;
            default:
                delta = 0;
                break;
        }

        handCounter++;
        handStatusLabel.setText(clientId + " completed hand #" + handCounter + ".");

        String outcomeText = String.format("%s bet $%.2f and %s $%.2f on hand #%d.",
                clientId,
                betAmount,
                delta > 0 ? "won" : delta < 0 ? "lost" : "pushed",
                Math.abs(delta),
                handCounter);
        addEvent(outcomeText);
    }

    private void updateStatusLabels() {
        statusLabel.setText(serverRunning ? "Running" : "Stopped");
        statusLabel.setTextFill(serverRunning ? Color.LIMEGREEN : Color.CRIMSON);
        portLabel.setText(currentPort.isEmpty() ? "-" : currentPort);
        clientCountLabel.setText(String.valueOf(clientCount));
        turnOnButton.setDisable(serverRunning);
        turnOffButton.setDisable(!serverRunning);
    }

    private boolean isValidPort(String portText) {
        try {
            int port = Integer.parseInt(portText);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void addEvent(String message) {
        String stamped = "[" + LocalTime.now().format(timeFormatter) + "] " + message;
        eventItems.add(stamped);
        eventListView.scrollTo(eventItems.size() - 1);
    }
}
