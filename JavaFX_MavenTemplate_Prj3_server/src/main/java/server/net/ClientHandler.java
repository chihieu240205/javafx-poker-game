package server.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import server.game.Deck;
import server.game.PlayerSession;
import server.game.ThreeCardLogic;
import shared.Card;
import shared.GamePhase;
import shared.PlayerAction;
import shared.PokerInfo;
import shared.RoundOutcome;

public class ClientHandler implements Runnable {

    private Socket socket;
    private PokerServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean active = true;
    private PlayerSession session;
    private Deck deck;
    private long clientId;
    private int currentHandNumber = 0;

    private PokerInfo handleStatusRequest() {
        PokerInfo res = new PokerInfo();
        res.setClientId(clientId);

        res.setTotalBalance(session.getBalance());

        res.setGamePhase(GamePhase.WAITING_BETS);

        res.addMessage("Connected. Current balance: $" + session.getBalance());

        return res;
    }


    public ClientHandler(Socket socket, PokerServer server) {
        this.socket = socket;
        this.server = server;
        this.clientId = System.currentTimeMillis();
        this.session = new PlayerSession(clientId);
        this.deck = new Deck();
    }

    public PlayerSession getSession() {
        return session;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(15000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            log("Client handler started for client " + clientId);

            while (active) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof PokerInfo) {
                        PokerInfo request = (PokerInfo) obj;
                        PokerInfo response = processRequest(request);

                        out.writeObject(response);
                        out.flush();
                    }
                } catch (SocketTimeoutException e) {
                    continue;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            log("Client disconnected or error: " + e.getMessage());
        } finally {
            server.removeClient(this);
            close();
        }
    }

    private PokerInfo processRequest(PokerInfo request) {

        PokerInfo response = new PokerInfo();
        response.setClientId(clientId);
        response.getMessages().clear();

        PlayerAction action = request.getAction();

        switch (action) {

            case REQUEST_STATUS:
                PokerInfo status = handleStatusRequest();
                copyStatusIntoResponse(status, response);
                break;

            case REQUEST_DEAL:
                handleDeal(request, response);
                break;

            case PLAY:
                handlePlay(request, response);
                break;

            case FOLD:
                handleFold(request, response);
                break;

            default:
                response.addMessage("Invalid action received");
                response.setGamePhase(GamePhase.WAITING_BETS);
        }

        response.setTotalBalance(session.getBalance());

        return response;
    }

    private void copyStatusIntoResponse(PokerInfo from, PokerInfo to) {
        to.setGamePhase(from.getGamePhase());
        to.setPlayerHand(from.getPlayerHand());
        to.setDealerHand(from.getDealerHand());
        to.setAnteBet(from.getAnteBet());
        to.setPairPlusBet(from.getPairPlusBet());
        to.setPlayBet(from.getPlayBet());
        to.getMessages().addAll(from.getMessages());
    }



    private void handleDeal(PokerInfo request, PokerInfo response) {
        int anteBet = request.getAnteBet();
        int pairPlusBet = request.getPairPlusBet();

        if (anteBet < 5 || anteBet > 25) {
            response.addMessage("Ante must be between 5 and 25.");
            response.setGamePhase(GamePhase.WAITING_BETS);
            return;
        }

        if (pairPlusBet != 0) {
            if (pairPlusBet < 5 || pairPlusBet > 25) {
                response.addMessage("Pair Plus must be 0 or between 5 and 25.");
                response.setGamePhase(GamePhase.WAITING_BETS);
                return;
            }
        }

        int totalBet = anteBet + pairPlusBet;
        if (totalBet > session.getBalance()) {
            response.addMessage("Insufficient balance. You have: " + session.getBalance());
            response.setGamePhase(GamePhase.WAITING_BETS);
            return;
        }

        if (deck.remainingCards() < 10) {
            deck = new Deck();
            response.addMessage("New deck shuffled");
        }

        session.resetRound();

        session.setAnteBet(anteBet);
        session.setPairPlusBet(pairPlusBet);
        session.updateBalance(-totalBet);

        session.setPlayerHand(deck.dealHand(3));
        session.setDealerHand(deck.dealHand(3));

        response.setPlayerHand(session.getPlayerHand());
        response.setDealerHand(session.getDealerHand());
        response.setAnteBet(anteBet);
        response.setPairPlusBet(pairPlusBet);
        response.setGamePhase(GamePhase.AWAITING_ACTION);
        response.addMessage("Cards dealt. Choose Play or Fold.");

        session.setCurrentPhase(GamePhase.AWAITING_ACTION);
        currentHandNumber = session.getHandsPlayed() + 1;
        server.updateClientPhase(clientId, "Awaiting action (hand #" + currentHandNumber + ")");

        server.recordHandStart(clientId, currentHandNumber, anteBet, pairPlusBet, session.getBalance());
    }

    private void handlePlay(PokerInfo request, PokerInfo response) {

        if (session.getCurrentPhase() != GamePhase.AWAITING_ACTION) {
            response.addMessage("Invalid action: not awaiting play/fold. Round has been reset.");

            session.resetRound();

            response.setPlayerHand(new ArrayList<>());
            response.setDealerHand(new ArrayList<>());
            response.setAnteBet(0);
            response.setPairPlusBet(0);
            response.setPlayBet(0);
            response.setRoundResult(null);
            response.setNetWinnings(0);
            response.setGamePhase(GamePhase.WAITING_BETS);

            return;
        }

        session.setPlayBet(session.getAnteBet());

        if (session.getPlayBet() > session.getBalance()) {
            response.addMessage("Insufficient balance for play bet.");
            session.resetRound();
            response.setGamePhase(GamePhase.WAITING_BETS);
            return;
        }

        session.updateBalance(-session.getPlayBet());

        int anteBet = session.getAnteBet();
        int playBet = session.getPlayBet();
        int pairBet = session.getPairPlusBet();
        int handNumber = session.getHandsPlayed() + 1;

        boolean dealerQualifies = ThreeCardLogic.dealerQualifies(session.getDealerHand());
        int pairPlusWinnings = ThreeCardLogic.evalPPWinnings(session.getPlayerHand(), session.getPairPlusBet());

        int pairPlusPayout = 0;
        if (pairBet > 0 && pairPlusWinnings > 0) {
            pairPlusPayout = pairBet + pairPlusWinnings;
        }

        int antePlayWinnings = 0;
        RoundOutcome outcome;

        if (!dealerQualifies) {
            outcome = RoundOutcome.PUSH;
            antePlayWinnings = session.getAnteBet() + session.getPlayBet();
            response.addMessage("Dealer does not have at least Queen high; ante is pushed and play wager returned.");
        } else {
            int comparison = ThreeCardLogic.compareHands(session.getDealerHand(), session.getPlayerHand());
            if (comparison > 0) {
                outcome = RoundOutcome.WIN;
                antePlayWinnings = (session.getAnteBet() + session.getPlayBet()) * 2;
                response.addMessage("You win! Dealer hand: " + formatHand(session.getDealerHand()));
            } else if (comparison < 0) {
                outcome = RoundOutcome.LOSS;
                antePlayWinnings = 0;
                response.addMessage("Dealer wins. Dealer hand: " + formatHand(session.getDealerHand()));
            } else {
                outcome = RoundOutcome.PUSH;
                antePlayWinnings = session.getAnteBet() + session.getPlayBet();
                response.addMessage("Push! Dealer hand: " + formatHand(session.getDealerHand()));
            }
        }

        int totalPayout = antePlayWinnings + pairPlusPayout;
        int totalBet = anteBet + playBet + pairBet;

        int netWinnings = totalPayout - totalBet;

        session.updateBalance(totalPayout);

        if (pairPlusWinnings > 0) {
            response.addMessage("Pair-Plus bonus: " + pairPlusWinnings);
        }

        response.setPlayerHand(session.getPlayerHand());
        response.setDealerHand(session.getDealerHand());
        response.setAnteBet(session.getAnteBet());
        response.setPairPlusBet(session.getPairPlusBet());
        response.setPlayBet(session.getPlayBet());
        response.setRoundResult(outcome);
        response.setNetWinnings(netWinnings);
        response.setGamePhase(GamePhase.RESOLVED);

        session.setCurrentPhase(GamePhase.RESOLVED);
        session.incrementHandsPlayed();

        server.recordRoundOutcome(
                clientId,
                handNumber,
                anteBet,
                pairBet,
                playBet,
                netWinnings,
                outcome,
                session.getBalance());
    }


    private void handleFold(PokerInfo request, PokerInfo response) {

        if (session.getCurrentPhase() != GamePhase.AWAITING_ACTION) {
            response.addMessage("Invalid action: not awaiting play/fold. Round has been reset.");

            session.resetRound();

            response.setPlayerHand(new ArrayList<>());
            response.setDealerHand(new ArrayList<>());
            response.setAnteBet(0);
            response.setPairPlusBet(0);
            response.setPlayBet(0);
            response.setRoundResult(null);
            response.setNetWinnings(0);
            response.setGamePhase(GamePhase.WAITING_BETS);

            return;
        }

        int netWinnings = -(session.getAnteBet() + session.getPairPlusBet());

        response.setPlayerHand(session.getPlayerHand());
        response.setDealerHand(session.getDealerHand());
        response.setAnteBet(session.getAnteBet());
        response.setPairPlusBet(session.getPairPlusBet());
        response.setPlayBet(0);
        response.setRoundResult(RoundOutcome.FOLD);
        response.setNetWinnings(netWinnings);
        response.setGamePhase(GamePhase.RESOLVED);
        response.addMessage("You folded. Ante and Pair Plus wagers are forfeited.");

        session.setCurrentPhase(GamePhase.RESOLVED);
        session.incrementHandsPlayed();

        server.recordRoundOutcome(
                clientId,
                session.getHandsPlayed(),
                session.getAnteBet(),
                session.getPairPlusBet(),
                0,
                netWinnings,
                RoundOutcome.FOLD,
                session.getBalance());

        server.updateClientPhase(clientId, "Waiting for next hand");
    }

    private String formatHand(List<Card> hand) {
        if (hand == null || hand.isEmpty()) return "No cards";
        StringBuilder sb = new StringBuilder();
        for (Card c : hand) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c.getRank()).append(" of ").append(c.getSuit());
        }
        return sb.toString();
    }

    private void log(String msg) {
        System.out.println("[ClientHandler " + clientId + "] " + msg);
    }

    public void requestShutdown(String reason) {
        try {
            if (out != null) {
                PokerInfo info = new PokerInfo();
                info.setClientId(clientId);
                info.addMessage(reason != null ? reason : "Server shutting down.");
                info.setGamePhase(GamePhase.WAITING_BETS);
                out.writeObject(info);
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    public void close() {
        active = false;
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (Exception ignored) {}
    }

    @Override
    public String toString() {
        return "Client " + clientId + " (Balance: " + session.getBalance() + ")";
    }

    public long getClientId() {
        return clientId;
    }

    public String getStatusText() {
        GamePhase phase = session.getCurrentPhase();
        switch (phase) {
            case AWAITING_ACTION:
                return "Playing hand #" + (session.getHandsPlayed() + 1) + " (awaiting action)";
            case DEALT:
                return "Cards dealt";
            case RESOLVED:
                return "Round resolved";
            case WAITING_BETS:
            default:
                return "Waiting to bet";
        }
    }
}
