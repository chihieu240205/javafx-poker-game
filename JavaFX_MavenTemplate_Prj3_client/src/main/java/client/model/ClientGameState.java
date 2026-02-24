package client.model;

import shared.PokerInfo;

public class ClientGameState {

    private long clientId;
    private int balance = 100;
    private PokerInfo lastInfo;
    private ThemeVariant theme = ThemeVariant.CLASSIC;

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public PokerInfo getLastInfo() {
        return lastInfo;
    }

    public void setLastInfo(PokerInfo lastInfo) {
        this.lastInfo = lastInfo;
    }

    public ThemeVariant getTheme() {
        return theme;
    }

    public void toggleTheme() {
        if (theme == ThemeVariant.CLASSIC) {
            theme = ThemeVariant.MODERN;
        } else {
            theme = ThemeVariant.CLASSIC;
        }
    }

    public void reset() {
        lastInfo = null;
    }
}
