package client.ui;

import client.net.PokerClientConnection;
import client.ui.AudioManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import shared.PokerInfo;
import shared.PlayerAction;

public class WelcomeController {

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;

    private ClientApp clientApp;
    private final AudioManager audioManager = AudioManager.getInstance();

    void setClientApp(ClientApp app) {
        this.clientApp = app;
    }

    @FXML
    private void initialize() {
        ipField.setText("127.0.0.1");
        portField.setText("5555");
        statusLabel.setText("status: waiting to connect…");
    }

    @FXML
    private void onConnect() {
        audioManager.playClick();
        String host = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty() || portText.isEmpty()) {
            statusLabel.setText("status: please enter host & port");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            statusLabel.setText("status: port must be a number");
            return;
        }

        PokerClientConnection conn = clientApp.getConnection();
        statusLabel.setText("status: connecting…");

        boolean ok = conn.connect(host, port);
        if (!ok) {
            statusLabel.setText("status: could not connect to server.");
            return;
        }

        statusLabel.setText("status: connected.");

        PokerInfo req = new PokerInfo();
        req.setAction(PlayerAction.REQUEST_STATUS);
        conn.send(req);

        clientApp.showScene(ClientScene.GAMEPLAY);
    }
}
