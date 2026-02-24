package server.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.net.PokerServer;

public class ServerIntroController {

    @FXML private TextField portField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button monitorButton;
    @FXML private Label statusLabel;

    private PokerServer pokerServer;

    @FXML
    public void initialize() {
        stopButton.setDisable(true);
        monitorButton.setDisable(true);

        startButton.setOnAction(e -> startServer());
        stopButton.setOnAction(e -> stopServer());
        monitorButton.setOnAction(e -> openMonitorScene());
    }

    private void startServer() {
        if (pokerServer != null && pokerServer.isRunning()) {
            statusLabel.setText(
                "Status: Already running on port " + pokerServer.getPort()
            );
            return;
        }

        String portText = portField.getText().trim();
        if (portText.isEmpty()) {
            statusLabel.setText("Status: Enter a port number");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Status: Invalid port");
            return;
        }

        pokerServer = new PokerServer(port);
        pokerServer.startServer();

        statusLabel.setText("Status: Running on port " + port);
        portField.setDisable(true);
        startButton.setDisable(true);
        stopButton.setDisable(false);
        monitorButton.setDisable(false);

        openMonitorScene();
    }


    private void stopServer() {
        if (pokerServer != null) {
            pokerServer.stopServer();
        }
        statusLabel.setText("Status: Stopped (clients disconnected)");
        portField.setDisable(false);
        startButton.setDisable(false);
        stopButton.setDisable(true);
        monitorButton.setDisable(true);
    }

    public void attachExistingServer(PokerServer server) {
        this.pokerServer = server;

        if (server != null && server.isRunning()) {
            portField.setText(String.valueOf(server.getPort()));
            statusLabel.setText("Status: Running on port " + server.getPort());

            portField.setDisable(true);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            monitorButton.setDisable(false);
        } else {
            statusLabel.setText("Status: Stopped");
            portField.setDisable(false);
            startButton.setDisable(false);
            stopButton.setDisable(true);
            monitorButton.setDisable(true);
        }
    }

    private void openMonitorScene() {
        if (pokerServer == null) {
            statusLabel.setText("Status: Start server first");
            return;
        }
        try {
            Stage stage = (Stage) startButton.getScene().getWindow();
            ServerApp.showMonitorScene(stage, pokerServer);
            statusLabel.setText("Status: Monitor opened");
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + (e.getMessage() != null ? (": " + e.getMessage()) : "");
            statusLabel.setText("Status: failed to open monitor: " + msg);
            e.printStackTrace();
        }
    }
}
