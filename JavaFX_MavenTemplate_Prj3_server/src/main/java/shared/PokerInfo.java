package shared;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PokerInfo implements Serializable {

    private List<Card> playerHand = new ArrayList<>();
    private List<Card> dealerHand = new ArrayList<>();
    private int anteBet;
    private int pairPlusBet;
    private int playBet;
    private RoundOutcome roundResult;
    private int netWinnings;
    private int totalBalance;
    private List<String> messages = new ArrayList<>();
    private PlayerAction action = PlayerAction.NONE;
    private boolean foldRequested;
    private long clientId;
    private GamePhase gamePhase = GamePhase.WAITING_BETS;
    private Instant timestamp = Instant.now();

    public PokerInfo() {}

    public List<Card> getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(List<Card> playerHand) {
        this.playerHand = playerHand;
    }

    public List<Card> getDealerHand() {
        return dealerHand;
    }

    public void setDealerHand(List<Card> dealerHand) {
        this.dealerHand = dealerHand;
    }

    public int getAnteBet() {
        return anteBet;
    }

    public void setAnteBet(int anteBet) {
        this.anteBet = anteBet;
    }

    public int getPairPlusBet() {
        return pairPlusBet;
    }

    public void setPairPlusBet(int pairPlusBet) {
        this.pairPlusBet = pairPlusBet;
    }

    public int getPlayBet() {
        return playBet;
    }

    public void setPlayBet(int playBet) {
        this.playBet = playBet;
    }

    public RoundOutcome getRoundResult() {
        return roundResult;
    }

    public void setRoundResult(RoundOutcome roundResult) {
        this.roundResult = roundResult;
    }

    public int getNetWinnings() {
        return netWinnings;
    }

    public void setNetWinnings(int netWinnings) {
        this.netWinnings = netWinnings;
    }

    public int getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(int totalBalance) {
        this.totalBalance = totalBalance;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String msg) {
        this.messages.add(msg);
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void setAction(PlayerAction action) {
        this.action = action;
    }

    public PlayerAction getAction() {
        return action;
    }

    public boolean isFoldRequested() {
        return foldRequested;
    }

    public void setFoldRequested(boolean foldRequested) {
        this.foldRequested = foldRequested;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isActionRequired() {
        return gamePhase == GamePhase.AWAITING_ACTION;
    }
}
