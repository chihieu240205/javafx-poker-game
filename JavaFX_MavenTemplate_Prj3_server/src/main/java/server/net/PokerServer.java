package server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import server.game.PlayerSession;
import shared.RoundOutcome;

public class PokerServer {

    private int port;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor clientExecutor;
    private boolean running = false;
    private Thread acceptThread;

    private server.ui.ServerMonitorController monitor;

    private CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public PokerServer(int port) {
        this.port = port;
    }

    public void setMonitor(server.ui.ServerMonitorController monitor) {
        this.monitor = monitor;
        if (monitor != null) {
            monitor.setPort(port);
            monitor.setRunningState(running);
            refreshMonitorClientState();
            monitor.log("Monitor attached.");
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }


    private void log(String msg) {
        if (monitor != null) {
            monitor.log(msg);
        }
        System.out.println("[SERVER] " + msg);
    }

    private void gameEvent(String msg) {
        if (monitor != null) {
            monitor.addGameEvent(msg);
        }
        System.out.println("[GAME] " + msg);
    }

    public void updateClientPhase(long clientId, String status) {
        if (monitor != null) {
            monitor.upsertClientStatus(clientId, status);
        }
    }

    private void refreshMonitorClientState() {
        if (monitor != null) {
            monitor.updateClientCount(clients.size());
            for (ClientHandler c : clients) {
                monitor.upsertClientStatus(c.getClientId(), c.getStatusText());
            }
        }
    }


    public List<PlayerSession> getTop10Players() {
        return clients.stream()
                .map(ClientHandler::getSession)
                .filter(s -> s != null)
                .sorted((a, b) -> Integer.compare(
                        b.getTotalMoneyWon(),
                        a.getTotalMoneyWon()))
                .limit(10)
                .collect(Collectors.toList());
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            clientExecutor = new ThreadPoolExecutor(
                    4,
                    32,
                    60L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(50),
                    new ThreadPoolExecutor.CallerRunsPolicy());

            log("Server started on port " + port);

            acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        log("Client connected: " + socket.getInetAddress());

                        ClientHandler handler = new ClientHandler(socket, this);

                        handleClientConnected(handler, socket);

                        clientExecutor.execute(handler);

                    } catch (IOException ignored) {
                        if (!running) break;
                    }
                }
            }, "poker-accept-thread");

            acceptThread.setDaemon(true);
            acceptThread.start();

            if (monitor != null) {
                monitor.setRunningState(true);
            }

        } catch (IOException e) {
            log("Failed to start server: " + e.getMessage());
        }
    }

    public boolean stopServer() {
        running = false;

        log("Stopping server...");

        if (!clients.isEmpty()) {
            log("Notifying " + clients.size() + " client(s) of shutdown.");
            for (ClientHandler c : clients) {
                c.requestShutdown("Server is shutting down, disconnecting all players.");
            }
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        for (ClientHandler c : clients) {
            c.close();
        }

        clients.clear();
        refreshMonitorClientState();

        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        if (clientExecutor != null) {
            clientExecutor.shutdownNow();
        }

        log("Server stopped.");
        gameEvent("Server stopped.");

        if (monitor != null) {
            monitor.setRunningState(false);
        }

        return true;
    }

    public void removeClient(ClientHandler c) {
        clients.remove(c);
        log("Client disconnected.");
        gameEvent("Client " + c.toString() + " dropped.");
        if (monitor != null) {
            monitor.removeClientStatus(c.getClientId());
        }
        refreshMonitorClientState();
    }

    private void handleClientConnected(ClientHandler handler, Socket socket) {
        clients.add(handler);
        gameEvent("Client " + handler.toString() + " joined from " + socket.getInetAddress().getHostAddress());
        if (monitor != null) {
            monitor.upsertClientStatus(handler.getClientId(), "Connected · waiting to bet");
        }
        refreshMonitorClientState();
    }

    public void recordHandStart(long clientId, int handNumber, int anteBet, int pairPlusBet, int balanceAfterBet) {
        gameEvent(String.format(
                "Client %d starting hand #%d | Ante: $%d, Pair+: $%d | Balance after bet: $%d",
                clientId, handNumber, anteBet, pairPlusBet, balanceAfterBet));
        if (monitor != null) {
            monitor.upsertClientStatus(clientId, "Playing hand #" + handNumber);
        }
    }

    public void recordRoundOutcome(long clientId,
                                int handNumber,
                                int anteBet,
                                int pairPlusBet,
                                int playBet,
                                int netWinnings,
                                RoundOutcome outcome,
                                int balanceAfterRound) {
        gameEvent(String.format(
                "Hand #%d (Client %d): Ante $%d, Pair+ $%d, Play $%d -> %s, Net %s$%d, Balance $%d",
                handNumber,
                clientId,
                anteBet,
                pairPlusBet,
                playBet,
                outcome,
                netWinnings >= 0 ? "+" : "-",
                Math.abs(netWinnings),
                balanceAfterRound));

        if (monitor != null) {
            monitor.addGameRecord(new server.ui.ServerMonitorController.GameRecord(
                    clientId,
                    handNumber,
                    anteBet,
                    pairPlusBet,
                    playBet,
                    outcome.toString(),
                    netWinnings,
                    balanceAfterRound
            ));
            monitor.upsertClientStatus(clientId, "Waiting for next hand");
        }

        PlayerSession session = null;
        for (ClientHandler c : clients) {
            if (c.getClientId() == clientId) {
                session = c.getSession();
                break;
            }
        }

        if (session != null) {
            session.addMoneyWon(netWinnings);
        }

        if (monitor != null) {
            monitor.updateLeaderboard(getTop10Players());
        }
    }

}
