package client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import client.model.CardViewModel;
import client.model.ClientGameState;
import client.net.PokerClientConnection;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import shared.Card;
import shared.GamePhase;
import shared.PlayerAction;
import shared.PokerInfo;

public class GameController {

    @FXML private Label balanceLabel;
    @FXML private Label totalWinningsLabel;
    @FXML private TextField anteField;
    @FXML private TextField pairPlusField;
    @FXML private Label playLabel;
    @FXML private TableView<InfoRow> roundInfoTable;
    @FXML private TableColumn<InfoRow, String> infoLabelColumn;
    @FXML private TableColumn<InfoRow, String> infoValueColumn;
    @FXML private Slider musicVolumeSlider;
    @FXML private Slider sfxVolumeSlider;
    @FXML private Label dealerRankLabel;
    @FXML private Label playerRankLabel;

    @FXML private ImageView dealerCard1;
    @FXML private ImageView dealerCard2;
    @FXML private ImageView dealerCard3;

    @FXML private ImageView playerCard1;
    @FXML private ImageView playerCard2;
    @FXML private ImageView playerCard3;

    @FXML private TextArea messageArea;

    @FXML private Button dealButton;
    @FXML private Button playButton;
    @FXML private Button foldButton;
    @FXML private Button continueButton;
    @FXML private Button quickBetButton;

    @FXML private Button setAnteButton;
    @FXML private Button setPairPlusButton;

    private ClientApp clientApp;
    private ClientGameState gameState;
    private PokerClientConnection connection;

    private boolean anteSet = false;
    private boolean pairPlusSet = false;
    private int anteValue = 0;
    private int pairPlusValue = 0;
    private int totalWinnings = 0;
    private boolean awaitingServer = false;
    private final ObservableList<InfoRow> roundInfoRows = FXCollections.observableArrayList();
    private PokerInfo lastResolvedInfo;
    private boolean dealerFaceDown = true;
    private boolean playerJustDealt = false;
    private static final double VOLUME_SCALE = 100.0;
    private final AudioManager audioManager = AudioManager.getInstance();
    private final Map<Integer, Integer> anteFrequencies = new HashMap<>();
    private final Map<Integer, Integer> pairPlusFrequencies = new HashMap<>();

    private final List<String> messageHistory = new ArrayList<>();
    private final Random dealerHumor = new Random();
    private final List<String> dealerQuipBag = new ArrayList<>(DEALER_QUIPS);
    private static final List<String> DEALER_QUIPS = List.of(
            "Shuffle faster, feelings later.",
            "Remember: the house plants always win.",
            "My poker face is 90% eyebrows.",
            "I tip myself in sarcasm.",
            "Cards up, spirits up!",
            "Want tips? I only deal sarcasm.",
            "These cards smell like victory… or snacks.",
            "Winner buys the next deck, right?",
            "If luck knocks, tell it I'm busy shuffling.",
            "Don't blame me, I just narrate the chaos.",
            "Bet responsibly, celebrate loudly.",
            "House rules: laugh at bad beats.",
            "Card counting? I struggle with card pronouncing.",
            "Every deck has drama. I'm just the host.",
            "May your straights be royal and your excuses creative."
    );
    private void playClick() { audioManager.playClick(); }
    private void showHandRanks(GamePhase phase, List<Card> ph, List<Card> dh) {
        if (playerRankLabel != null) {
            if (ph != null && ph.size() >= 3 && phase != GamePhase.WAITING_BETS) {
                playerRankLabel.setText(describeHand(ph));
            } else {
                playerRankLabel.setText("");
            }
        }

        if (dealerRankLabel != null) {
            if (dh != null && dh.size() >= 3 && phase == GamePhase.RESOLVED) {
                dealerRankLabel.setText(describeHand(dh));
            } else {
                dealerRankLabel.setText("");
            }
        }
    }

    void setClientApp(ClientApp app, ClientGameState state) {
        this.clientApp = app;
        this.gameState = state;
        this.connection = app.getConnection();
        if (balanceLabel != null) {
            balanceLabel.setText("$" + gameState.getBalance());
        }

        updateBetSummaryLabel(0, 0, 0);
        updateTotalWinningsLabel();
    }

    @FXML
    private void initialize() {
        playButton.setDisable(true);
        foldButton.setDisable(true);
        messageArea.setEditable(false);

        dealButton.setDisable(true);
        if (continueButton != null) {
            continueButton.setVisible(false);
            continueButton.setManaged(false);
        }

        if (roundInfoTable != null) {
            roundInfoTable.setItems(roundInfoRows);
        }
        if (infoLabelColumn != null) {
            infoLabelColumn.setCellValueFactory(data -> data.getValue().labelProperty());
        }
        if (infoValueColumn != null) {
            infoValueColumn.setCellValueFactory(data -> data.getValue().valueProperty());
        }
        if (musicVolumeSlider != null) {
            musicVolumeSlider.setMin(0);
            musicVolumeSlider.setMax(VOLUME_SCALE);
            musicVolumeSlider.setValue(audioManager.getMusicVolume() * VOLUME_SCALE);
            musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double level = newVal.doubleValue() / VOLUME_SCALE;
                audioManager.setMusicVolume(level);
            });
        }
        if (sfxVolumeSlider != null) {
            sfxVolumeSlider.setMin(0);
            sfxVolumeSlider.setMax(VOLUME_SCALE);
            sfxVolumeSlider.setValue(audioManager.getSfxVolume() * VOLUME_SCALE);
            sfxVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double level = newVal.doubleValue() / VOLUME_SCALE;
                audioManager.setSfxVolume(level);
            });
        }

        setupKeyboardShortcuts();
    }

    @FXML
    private void onSetAnte() {
        playClick();
        String text = anteField.getText().trim();

        if (!text.matches("\\d+")) {
            messageArea.appendText("Ante must be a number.\n");
            return;
        }

        int val = Integer.parseInt(text);
        if (val < 5 || val > 25) {
            messageArea.appendText("Ante must be between 5–25.\n");
            return;
        }

        anteValue = val;
        anteSet = true;
        anteField.setDisable(true);
        if (setAnteButton != null) {
            setAnteButton.setDisable(true);
        }

        messageArea.appendText("Ante set to $" + val + "\n");
        recordFrequency(anteFrequencies, val);

        updateBetSummaryLabel(anteValue, pairPlusValue, 0);
        updateDealButtonState();
        requestGlobalFocus();
    }

    @FXML
    private void onSetPairPlus() {
        playClick();
        String text = pairPlusField.getText().trim();

        if (text.isEmpty()) {
            pairPlusValue = 0;
            pairPlusSet = true;
            pairPlusField.setDisable(true);
            if (setPairPlusButton != null) {
                setPairPlusButton.setDisable(true);
            }
            messageArea.appendText("Pair Plus left at $0.\n");
            updateBetSummaryLabel(anteValue, pairPlusValue, 0);
            updateDealButtonState();
            recordFrequency(pairPlusFrequencies, pairPlusValue);
            requestGlobalFocus();
            return;
        }

        if (!text.matches("\\d+")) {
            messageArea.appendText("Pair Plus must be a number.\n");
            return;
        }

        int val = Integer.parseInt(text);
        if (val != 0 && (val < 5 || val > 25)) {
            messageArea.appendText("Pair Plus must be 5–25 or 0.\n");
            return;
        }

        pairPlusValue = val;
        pairPlusSet = true;
        pairPlusField.setDisable(true);

        if (setPairPlusButton != null) {
            setPairPlusButton.setDisable(true);
        }

        messageArea.appendText("Pair Plus set to $" + val + "\n");
        updateBetSummaryLabel(anteValue, pairPlusValue, 0);
        updateDealButtonState();
        recordFrequency(pairPlusFrequencies, val);
        requestGlobalFocus();
    }

    private void updateDealButtonState() {
        dealButton.setDisable(!anteSet);
        if (quickBetButton != null) {
            quickBetButton.setDisable(anteSet);
        }
    }

    private void updateBetSummaryLabel(int ante, int pairPlus, int play) {
        if (playLabel != null) {
            playLabel.setText(
                    "Ante: $" + ante +
                            "   Pair+: $" + pairPlus +
                            "   Play: $" + play
            );
        }
    }

    private void updateTotalWinningsLabel() {
        if (totalWinningsLabel != null) {
            totalWinningsLabel.setText("$" + totalWinnings);
        }
    }


    private void setupKeyboardShortcuts() {
        if (dealButton == null) return;
        dealButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, this::handleShortcut);
                oldScene.removeEventFilter(KeyEvent.KEY_TYPED, this::consumeShortcutCharacter);
            }
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleShortcut);
                newScene.addEventFilter(KeyEvent.KEY_TYPED, this::consumeShortcutCharacter);
                requestGlobalFocus();
            }
        });
    }

    private void requestGlobalFocus() {
        if (dealButton == null) return;
        Platform.runLater(() -> {
            if (dealButton.getScene() != null) {
                dealButton.getScene().getRoot().requestFocus();
            }
        });
    }

    private void handleShortcut(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean shortcutKey = isShortcutKey(code);
        if (!shortcutKey && event.getTarget() instanceof TextInputControl) {
            TextInputControl input = (TextInputControl) event.getTarget();
            if (input.isEditable() && !input.isDisabled()) {
                return;
            }
        }

        switch (code) {
            case Q:
                triggerButton(quickBetButton, event);
                break;
            case D:
                triggerButton(dealButton, event);
                break;
            case P:
                triggerButton(playButton, event);
                break;
            case F:
                triggerButton(foldButton, event);
                break;
            case C:
                triggerButton(continueButton, event);
                break;
            default:
                break;
        }
    }

    private void consumeShortcutCharacter(KeyEvent event) {
        if (event.getEventType() != KeyEvent.KEY_TYPED) {
            return;
        }
        if (!(event.getTarget() instanceof TextInputControl)) {
            return;
        }

        TextInputControl input = (TextInputControl) event.getTarget();
        if (!input.isEditable() || input.isDisabled()) {
            return;
        }

        if (isShortcutCharacter(event.getCharacter())) {
            event.consume();
        }
    }

    private boolean isShortcutKey(KeyCode code) {
        return code == KeyCode.Q
                || code == KeyCode.D
                || code == KeyCode.P
                || code == KeyCode.F
                || code == KeyCode.C;
    }

    private boolean isShortcutCharacter(String character) {
        if (character == null || character.isBlank()) {
            return false;
        }
        char c = Character.toUpperCase(character.charAt(0));
        return c == 'Q' || c == 'D' || c == 'P' || c == 'F' || c == 'C';
    }

    private void triggerButton(Button button, KeyEvent event) {
        if (button != null && !button.isDisabled() && button.isVisible()) {
            button.fire();
            event.consume();
        }
    }

    private void appendMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String flavored = addDealerFlavor(message);
        messageHistory.add(flavored);
        if (messageArea != null) {
            String finalText = flavored.endsWith("\n") ? flavored : flavored + "\n";
            messageArea.setText(finalText);
        }
    }

    private void appendMessages(List<String> messages) {
        if (messages == null) {
            return;
        }
        for (String m : messages) {
            appendMessage(m);
        }
    }

    private String addDealerFlavor(String message) {
        if (DEALER_QUIPS.isEmpty() || message == null || message.isBlank()) {
            return message;
        }
        if (dealerQuipBag.isEmpty()) {
            dealerQuipBag.addAll(DEALER_QUIPS);
        }
        String quip = dealerQuipBag.remove(dealerHumor.nextInt(dealerQuipBag.size()));
        dealerQuipBag.add(quip);
        return message + " " + quip;
    }

    private void clearMessageHistory() {
        messageHistory.clear();
        if (messageArea != null) {
            messageArea.clear();
        }
    }

    private boolean hasSufficientBalanceForDeal() {
        if (gameState == null) {
            return true;
        }

        int totalBet = anteValue + pairPlusValue;
        int balance = gameState.getBalance();

        if (totalBet > balance) {
            appendMessage(
                    "Insufficient balance for $" + totalBet +
                            " bet. Current balance: $" + balance + ".");
            return false;
        }

        return true;
    }

    @FXML
    private void onQuickBet() {
        playClick();
        int ante = mostFrequentOrDefault(anteFrequencies, 25);
        int pairPlus = mostFrequentOrDefault(pairPlusFrequencies, 0);

        anteField.setText(String.valueOf(ante));
        pairPlusField.setText(String.valueOf(pairPlus));

        anteValue = ante;
        pairPlusValue = pairPlus;
        anteSet = true;
        pairPlusSet = true;

        anteField.setDisable(true);
        pairPlusField.setDisable(true);
        if (setAnteButton != null) setAnteButton.setDisable(true);
        if (setPairPlusButton != null) setPairPlusButton.setDisable(true);

        recordFrequency(anteFrequencies, ante);
        recordFrequency(pairPlusFrequencies, pairPlus);

        messageArea.appendText("Quick Bet: Ante $" + ante + ", Pair+ $" + pairPlus + "\n");
        updateBetSummaryLabel(anteValue, pairPlusValue, 0);
        updateDealButtonState();
        requestGlobalFocus();
    }

    @FXML
    private void onDeal() {
        playClick();
        if (!anteSet) {
            messageArea.appendText("Cannot deal now.\n");
            return;
        }

        if (awaitingServer) {
            messageArea.appendText("Please wait for server.\n");
            return;
        }

        awaitingServer = true;

        dealButton.setDisable(true);
        playButton.setDisable(true);
        foldButton.setDisable(true);

        anteField.setDisable(true);
        pairPlusField.setDisable(true);
        if (setAnteButton != null)  setAnteButton.setDisable(true);
        if (setPairPlusButton != null) setPairPlusButton.setDisable(true);

        playerJustDealt = false;
        dealerFaceDown = true;

        PokerInfo info = new PokerInfo();
        info.setAction(PlayerAction.REQUEST_DEAL);
        info.setAnteBet(anteValue);
        info.setPairPlusBet(pairPlusValue);

        clientApp.getConnection().send(info);

        messageArea.appendText("Dealing…\n");
    }


    @FXML
    private void onPlay() {
        playClick();
        if (awaitingServer) {
            return;
        }
        awaitingServer = true;

        playButton.setDisable(true);
        foldButton.setDisable(true);
        dealButton.setDisable(true);

        PokerInfo info = new PokerInfo();
        info.setAction(PlayerAction.PLAY);
        clientApp.getConnection().send(info);
    }


    @FXML
    private void onFold() {
        playClick();
        if (awaitingServer) {
            return;
        }
        awaitingServer = true;

        playButton.setDisable(true);
        foldButton.setDisable(true);
        dealButton.setDisable(true);

        PokerInfo info = new PokerInfo();
        info.setAction(PlayerAction.FOLD);
        clientApp.getConnection().send(info);
    }


    @FXML
    private void onFreshStart() {
        playClick();
        gameState.reset();
        resetUI();

        PokerInfo req = new PokerInfo();
        req.setAction(PlayerAction.REQUEST_STATUS);
        clientApp.getConnection().send(req);
    }

    @FXML
    private void onExit() {
        playClick();
        clientApp.shutdown();
    }

    @FXML
    private void onNewLook() {
        playClick();
        if (clientApp != null) {
            clientApp.toggleNewLook();
        }
    }


    private void flipPlayerCards(List<Card> ph) {
        if (ph == null || ph.size() < 3) return;
        flipCard(playerCard1, new CardViewModel(ph.get(0), false).getImage());
        flipCard(playerCard2, new CardViewModel(ph.get(1), false).getImage());
        flipCard(playerCard3, new CardViewModel(ph.get(2), false).getImage());
    }

    private void flipDealerCards(List<Card> dh) {
        if (dh == null || dh.size() < 3) return;
        flipCard(dealerCard1, new CardViewModel(dh.get(0), false).getImage());
        flipCard(dealerCard2, new CardViewModel(dh.get(1), false).getImage());
        flipCard(dealerCard3, new CardViewModel(dh.get(2), false).getImage());
    }

    private void flipCard(ImageView view, Image newImage) {
        if (newImage == null) return;
        audioManager.playReveal();
        ScaleTransition hide = new ScaleTransition(Duration.millis(150), view);
        hide.setFromX(1);
        hide.setToX(0);

        ScaleTransition show = new ScaleTransition(Duration.millis(150), view);
        show.setFromX(0);
        show.setToX(1);

        hide.setOnFinished(e -> {
            view.setImage(newImage);
            show.play();
        });

        hide.play();
    }

    public void updateView(PokerInfo info) {

        awaitingServer = false;

        if (info.getTotalBalance() > 0) {
            balanceLabel.setText("$" + info.getTotalBalance());
        }

        updateBetSummaryLabel(
                info.getAnteBet(),
                info.getPairPlusBet(),
                info.getPlayBet()
        );

        messageArea.clear();
        if (info.getMessages() != null) {
            for (String m : info.getMessages()) {
                messageArea.appendText(m + "\n");
            }
        }

        GamePhase phase = info.getGamePhase();
        if (phase == null) phase = GamePhase.WAITING_BETS;


        List<Card> ph = info.getPlayerHand();
        List<Card> dh = info.getDealerHand();

        switch (phase) {
            case WAITING_BETS:
                dealButton.setDisable(!anteSet);
                playButton.setDisable(true);
                foldButton.setDisable(true);
                roundInfoRows.clear();
                showContinueButton(false);
                playerJustDealt = false;
                dealerFaceDown = true;
                break;

            case DEALT:
            case AWAITING_ACTION:
                dealButton.setDisable(true);
                playButton.setDisable(false);
                foldButton.setDisable(false);
                showContinueButton(false);
                if (!playerJustDealt && ph != null && ph.size() >= 3) {
                    flipPlayerCards(ph);
                    playerJustDealt = true;
                }
                dealerFaceDown = true;
                break;

            case RESOLVED:
                dealButton.setDisable(true);
                playButton.setDisable(true);
                foldButton.setDisable(true);
                totalWinnings += info.getNetWinnings();
                updateTotalWinningsLabel();
                populateRoundInfoTable(info);
                lastResolvedInfo = info;
                showContinueButton(true);
                if (dealerFaceDown && dh != null && dh.size() >= 3) {
                    flipDealerCards(dh);
                    dealerFaceDown = false;
                }

                boolean hasPairPlusBonus =
                        info.getMessages() != null &&
                        info.getMessages().stream()
                                .anyMatch(m -> m.contains("Pair-Plus bonus"));

                if (info.getPairPlusBet() > 0 && hasPairPlusBonus) {
                    messageArea.appendText("Pair+ bonus included in winnings.\n");
                } else if (info.getPairPlusBet() > 0) {
                    messageArea.appendText("Pair+ bonus not included.\n");
                } else {
                    messageArea.appendText("Pair+ bonus not included (no Pair+ bet).\n");
                }
                break;
        }

        updateCardImages(playerCard1, ph, 0, false);
        updateCardImages(playerCard2, ph, 1, false);
        updateCardImages(playerCard3, ph, 2, false);

        boolean dealerFaceDown = (phase != GamePhase.RESOLVED);
        updateCardImages(dealerCard1, dh, 0, dealerFaceDown);
        updateCardImages(dealerCard2, dh, 1, dealerFaceDown);
        updateCardImages(dealerCard3, dh, 2, dealerFaceDown);

        showHandRanks(phase, ph, dh);
    }


    public void resetUI() {
        balanceLabel.setText("$" + gameState.getBalance());
        anteField.clear();
        pairPlusField.clear();
        playLabel.setText("$0");

        playerCard1.setImage(null);
        playerCard2.setImage(null);
        playerCard3.setImage(null);

        dealerCard1.setImage(null);
        dealerCard2.setImage(null);
        dealerCard3.setImage(null);

        messageArea.clear();
        if (dealerRankLabel != null) dealerRankLabel.setText("");
        if (playerRankLabel != null) playerRankLabel.setText("");


        anteSet = false;
        pairPlusSet = false;
        anteValue = 0;
        pairPlusValue = 0;
        totalWinnings = 0;
        roundInfoRows.clear();
        lastResolvedInfo = null;

        anteField.setDisable(false);
        pairPlusField.setDisable(false);
        if (setAnteButton != null)  setAnteButton.setDisable(false);
        if (setPairPlusButton != null) setPairPlusButton.setDisable(false);

        dealButton.setDisable(true);
        playButton.setDisable(true);
        foldButton.setDisable(true);

        awaitingServer = false;
        dealerFaceDown = true;
        playerJustDealt = false;

        updateBetSummaryLabel(0, 0, 0);
        updateTotalWinningsLabel();
        updateDealButtonState();
    }

    public void showEndOfRoundPopup(PokerInfo info) {
        populateRoundInfoTable(info);

        lastResolvedInfo = info;
        showContinueButton(true);
    }

    private void updateCardImages(ImageView imageView, List<Card> hand, int index, boolean faceDown) {
        if (hand != null && index < hand.size()) {
            Card card = hand.get(index);
            CardViewModel vm = new CardViewModel(card, faceDown);
            imageView.setImage(vm.getImage());
        } else {
            imageView.setImage(null);
        }
    }

    @FXML
    private void onContinue() {
        playClick();
        if (lastResolvedInfo != null) {
            clientApp.showResult(lastResolvedInfo);
            showContinueButton(false);
        }
    }

    private void showContinueButton(boolean show) {
        if (continueButton != null) {
            continueButton.setVisible(show);
            continueButton.setManaged(show);
            continueButton.setDisable(!show);
        }
    }

    private void populateRoundInfoTable(PokerInfo info) {
        roundInfoRows.clear();

        String dealerHand = formatHand(info.getDealerHand());
        String playerHand = formatHand(info.getPlayerHand());
        String messages = formatMessages(info);

        roundInfoRows.add(new InfoRow("Outcome", String.valueOf(info.getRoundResult())));
        roundInfoRows.add(new InfoRow("Ante Bet", "$" + info.getAnteBet()));
        roundInfoRows.add(new InfoRow("Pair Plus Bet", "$" + info.getPairPlusBet()));
        roundInfoRows.add(new InfoRow("Play Bet", "$" + info.getPlayBet()));
        roundInfoRows.add(new InfoRow("Net Winnings", "$" + info.getNetWinnings()));
        roundInfoRows.add(new InfoRow("Dealer Hand", dealerHand));
        roundInfoRows.add(new InfoRow("Player Hand", playerHand));
        if (!messages.isEmpty()) {
            roundInfoRows.add(new InfoRow("Messages", messages));
        }
    }

    private String formatHand(List<Card> hand) {
        if (hand == null || hand.isEmpty()) {
            return "-";
        }
        return hand.stream()
                .map(c -> c.getRank() + " of " + c.getSuit())
                .collect(Collectors.joining(", "));
    }

    private String formatMessages(PokerInfo info) {
        if (info.getMessages() == null || info.getMessages().isEmpty()) {
            return "";
        }
        return info.getMessages().stream().collect(Collectors.joining(" | "));
    }

    private void recordFrequency(Map<Integer, Integer> freqMap, int value) {
        freqMap.put(value, freqMap.getOrDefault(value, 0) + 1);
    }

    private int mostFrequentOrDefault(Map<Integer, Integer> freqMap, int defaultVal) {
        if (freqMap.isEmpty()) return defaultVal;
        return freqMap.entrySet().stream()
                .max((a, b) -> {
                    int cmp = Integer.compare(a.getValue(), b.getValue());
                    if (cmp != 0) return cmp;
                    return Integer.compare(a.getKey(), b.getKey());
                })
                .map(Map.Entry::getKey)
                .orElse(defaultVal);
    }

    private String describeHand(List<Card> hand) {
        if (hand == null || hand.size() < 3) return "";

        List<shared.Rank> ranks = hand.stream().map(Card::getRank).collect(Collectors.toList());
        boolean flush = isFlush(hand);
        boolean straight = isStraight(ranks);

        Map<shared.Rank, Integer> counts = new HashMap<>();
        for (shared.Rank r : ranks) {
            counts.put(r, counts.getOrDefault(r, 0) + 1);
        }
        boolean trips = counts.containsValue(3);
        boolean pair = counts.containsValue(2);

        if (flush && straight) return "Straight Flush";
        if (trips) return "Three of a Kind";
        if (straight) return "Straight";
        if (flush) return "Flush";
        if (pair) return "Pair";

        shared.Rank high = highestRank(ranks);
        return "High Card " + formatRank(high);
    }

    private boolean isFlush(List<Card> hand) {
        Set<shared.Suit> suits = new HashSet<>();
        for (Card c : hand) {
            suits.add(c.getSuit());
        }
        return suits.size() == 1;
    }

    private boolean isStraight(List<shared.Rank> ranks) {
        List<Integer> values = ranks.stream()
                .map(shared.Rank::ordinal)
                .sorted()
                .collect(Collectors.toList());

        if (values.get(0).equals(values.get(1)) || values.get(1).equals(values.get(2))) {
            return false;
        }

        boolean regular = values.get(1) == values.get(0) + 1 && values.get(2) == values.get(1) + 1;
        boolean aceLow = values.get(0) == shared.Rank.TWO.ordinal()
                && values.get(1) == shared.Rank.THREE.ordinal()
                && values.get(2) == shared.Rank.ACE.ordinal();

        return regular || aceLow;
    }

    private shared.Rank highestRank(List<shared.Rank> ranks) {
        shared.Rank high = ranks.get(0);
        for (shared.Rank r : ranks) {
            if (r.ordinal() > high.ordinal()) {
                high = r;
            }
        }
        return high;
    }

    private String formatRank(shared.Rank r) {
        switch (r) {
            case ACE: return "Ace";
            case KING: return "King";
            case QUEEN: return "Queen";
            case JACK: return "Jack";
            case TEN: return "Ten";
            case NINE: return "Nine";
            case EIGHT: return "Eight";
            case SEVEN: return "Seven";
            case SIX: return "Six";
            case FIVE: return "Five";
            case FOUR: return "Four";
            case THREE: return "Three";
            case TWO: return "Two";
            default: return "";
        }
    }

    public static class InfoRow {
        private final SimpleStringProperty label = new SimpleStringProperty();
        private final SimpleStringProperty value = new SimpleStringProperty();

        public InfoRow(String label, String value) {
            this.label.set(label);
            this.value.set(value);
        }

        public SimpleStringProperty labelProperty() { return label; }
        public SimpleStringProperty valueProperty() { return value; }
    }
}
