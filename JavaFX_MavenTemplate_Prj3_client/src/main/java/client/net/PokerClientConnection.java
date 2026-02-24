package client.net;

import shared.PokerInfo;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class PokerClientConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public interface Listener {
        void onInfo(PokerInfo info);

        default void onError(Exception e) {}

        default void onDisconnect(String reason) {}
    }

    private Listener listener;
    private Thread listenerThread;
    private volatile boolean listening;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean connect(String host, int port) {
        try {
            if (isConnected()) {
                return true;
            }

            socket = new Socket(host, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            listening = true;
            listenerThread = new Thread(this::listenLoop, "poker-client-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            return true;

        } catch (IOException e) {
            notifyError(e);
            return false;
        }
    }

    public void send(PokerInfo info) {
        if (!isConnected()) return;

        try {
            out.writeObject(info);
            out.flush();
        } catch (IOException e) {
            notifyError(e);
            disconnect("Send failed: " + e.getMessage());
        }
    }

    private PokerInfo receive() throws IOException, ClassNotFoundException {
        if (!isConnected()) return null;

        Object obj = in.readObject();
        if (obj instanceof PokerInfo) {
            return (PokerInfo) obj;
        }
        return null;
    }

    private void listenLoop() {
        while (listening && isConnected()) {
            try {
                PokerInfo info = receive();
                if (info != null && listener != null) {
                    listener.onInfo(info);
                } else if (info == null) {
                    break;
                }
            } catch (Exception e) {
                notifyError(e);
                break;
            }
        }

        disconnect("Connection closed by server");
    }

    public void disconnect() {
        disconnect("Disconnected");
    }

    private void disconnect(String reason) {
        listening = false;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}

        notifyDisconnect(reason);

        if (listenerThread != null && listenerThread.isAlive() && !Objects.equals(Thread.currentThread(), listenerThread)) {
            try { listenerThread.join(200); } catch (InterruptedException ignored) {}
        }
    }

    public void sendSafe(PokerInfo info) {
        try {
            send(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyError(Exception e) {
        if (listener != null) {
            listener.onError(e);
        }
    }

    private void notifyDisconnect(String reason) {
        if (listener != null) {
            listener.onDisconnect(reason);
        }
    }
}
