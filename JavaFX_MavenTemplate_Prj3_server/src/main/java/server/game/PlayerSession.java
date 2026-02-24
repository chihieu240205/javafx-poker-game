package server.game;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import shared.Card;
import shared.GamePhase;

public class PlayerSession {
    private final long sessionId;
    private int handsPlayed;
    private int totalWinnings;
    private LocalDateTime lastActive = LocalDateTime.now();

    private int balance = 1000;
    private int anteBet = 0;
    private int pairPlusBet = 0;
    private int playBet = 0;
    private List<Card> playerHand = new ArrayList<>();
    private List<Card> dealerHand = new ArrayList<>();
    private GamePhase currentPhase = GamePhase.WAITING_BETS;
    private int totalMoneyWon = 0;

    public int getTotalMoneyWon() {
        return totalMoneyWon;
    }

    public void addMoneyWon(int amount) {
        totalMoneyWon += amount;
    }

    public PlayerSession(long id) {
        this.sessionId = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getHandsPlayed() {
        return handsPlayed;
    }

    public void incrementHandsPlayed() {
        this.handsPlayed++;
    }

    public int getTotalWinnings() {
        return totalWinnings;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void touch() {
        this.lastActive = LocalDateTime.now();
    }

    public int getBalance() {
        return balance;
    }

    public int getAnteBet() {
        return anteBet;
    }

    public void setAnteBet(int anteBet) {
        validateNonNegative(anteBet, "ante bet");
        this.anteBet = anteBet;
    }

    public int getPairPlusBet() {
        return pairPlusBet;
    }

    public void setPairPlusBet(int pairPlusBet) {
        validateNonNegative(pairPlusBet, "pair-plus bet");
        this.pairPlusBet = pairPlusBet;
    }

    public int getPlayBet() {
        return playBet;
    }

    public void setPlayBet(int playBet) {
        validateNonNegative(playBet, "play bet");
        this.playBet = playBet;
    }

    public List<Card> getPlayerHand() {
        return new ArrayList<>(playerHand);
    }

    public void setPlayerHand(List<Card> playerHand) {
        this.playerHand = new ArrayList<>(playerHand);
    }

    public List<Card> getDealerHand() {
        return new ArrayList<>(dealerHand);
    }

    public void setDealerHand(List<Card> dealerHand) {
        this.dealerHand = new ArrayList<>(dealerHand);
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(GamePhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public void resetRound() {
        anteBet = 0;
        pairPlusBet = 0;
        playBet = 0;
        playerHand.clear();
        dealerHand.clear();
        currentPhase = GamePhase.WAITING_BETS;
    }

    public void updateBalance(int change) {
        balance += change;
        totalWinnings += change;
    }

    private void validateNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException("" + field + " cannot be negative");
        }
    }
}
