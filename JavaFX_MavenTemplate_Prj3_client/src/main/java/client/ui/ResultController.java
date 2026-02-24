package client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import client.ui.AudioManager;
import shared.GamePhase;
import shared.PokerInfo;
import shared.RoundOutcome;

public class ResultController {

    @FXML private Label resultLabel;
    @FXML private Label winningsLabel;
    @FXML private Label balanceLabelResult;
    @FXML private Button playAgainButton;
    @FXML private Button exitButton;

    private ClientApp clientApp;
    private final AudioManager audioManager = AudioManager.getInstance();

    void setClientApp(ClientApp app) {
        this.clientApp = app;
    }

    @FXML
    private void handlePlayAgain() {
        audioManager.playClick();
        clientApp.resetGameplayUI();

        PokerInfo resetInfo = new PokerInfo();
        resetInfo.setGamePhase(GamePhase.WAITING_BETS);
        clientApp.getGameState().setLastInfo(resetInfo);

        clientApp.showScene(ClientScene.GAMEPLAY);
    }

    @FXML
    private void handleExit() {
        audioManager.playClick();
        clientApp.shutdown();
    }

    public void showResult(PokerInfo info) {
        int net = info.getNetWinnings();
        RoundOutcome outcome = info.getRoundResult();

        String resultText;
        if (net > 0) {
            resultText = "You won!";
        } else if (net < 0) {
            resultText = "You lost";
        } else {
            if (outcome == RoundOutcome.FOLD) {
                resultText = "You folded";
            } else {
                resultText = "Push";
            }
        }
        if (resultLabel != null) {
            resultLabel.setText(resultText);
        }

        StringBuilder winSb = new StringBuilder();
        winSb.append("Winnings: ").append(info.getNetWinnings());

        if (info.getPairPlusBet() > 0) {
            winSb.append("  (Pair+ bet: $").append(info.getPairPlusBet());
            if (info.getNetWinnings() > 0) {
                winSb.append(", includes Pair+ bonus");
            }
            winSb.append(")");
        }
        if (winningsLabel != null) {
            winningsLabel.setText(winSb.toString());
        }

        if (balanceLabelResult != null) {
            balanceLabelResult.setText("Balance: $" + info.getTotalBalance());
        }
    }
}
