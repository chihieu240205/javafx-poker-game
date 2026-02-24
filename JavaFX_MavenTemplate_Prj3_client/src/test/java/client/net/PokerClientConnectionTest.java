package client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import shared.PokerInfo;

class PokerClientConnectionTest {

    @Test
    void sendAndListenLoopWorkWithPreconfiguredStreams() throws Exception {
        PokerClientConnection connection = new PokerClientConnection();
        DummySocket dummySocket = new DummySocket();

        PipedOutputStream clientPipe = new PipedOutputStream();
        PipedInputStream serverPipe = new PipedInputStream(clientPipe);
        ObjectOutputStream clientOut = new ObjectOutputStream(clientPipe);
        ObjectInputStream serverIn = new ObjectInputStream(serverPipe);

        PokerInfo inboundInfo = new PokerInfo();
        inboundInfo.setTotalBalance(750);
        ObjectInputStream clientIn = objectInputStreamFor(inboundInfo);

        setField(connection, "socket", dummySocket);
        setField(connection, "out", clientOut);
        setField(connection, "in", clientIn);
        setField(connection, "listening", true);
        setField(connection, "listenerThread", Thread.currentThread());

        AtomicReference<PokerInfo> infoFromServer = new AtomicReference<>();
        AtomicReference<String> disconnectReason = new AtomicReference<>();
        CountDownLatch infoLatch = new CountDownLatch(1);
        CountDownLatch disconnectLatch = new CountDownLatch(1);

        connection.setListener(new PokerClientConnection.Listener() {
            @Override
            public void onInfo(PokerInfo info) {
                infoFromServer.set(info);
                infoLatch.countDown();
            }

            @Override
            public void onDisconnect(String reason) {
                disconnectReason.set(reason);
                disconnectLatch.countDown();
            }
        });

        PokerInfo outbound = new PokerInfo();
        outbound.setAnteBet(15);
        connection.send(outbound);
        PokerInfo received = (PokerInfo) serverIn.readObject();
        assertEquals(15, received.getAnteBet());

        Method loop = PokerClientConnection.class.getDeclaredMethod("listenLoop");
        loop.setAccessible(true);
        loop.invoke(connection);

        assertTrue(infoLatch.await(2, TimeUnit.SECONDS), "listener should receive inbound info");
        assertEquals(750, infoFromServer.get().getTotalBalance());
        assertTrue(disconnectLatch.await(2, TimeUnit.SECONDS), "disconnect callback should be fired");
        assertEquals("Connection closed by server", disconnectReason.get());
    }

    private ObjectInputStream objectInputStreamFor(PokerInfo info) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(buffer)) {
            oos.writeObject(info);
            oos.writeObject(null);
        }
        return new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = PokerClientConnection.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class DummySocket extends java.net.Socket {
        private boolean closed;

        @Override
        public boolean isConnected() {
            return !closed;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public synchronized void close() {
            closed = true;
        }
    }
}
