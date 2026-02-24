package server.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

class PokerServerClientStateTest {

    @Test
    void stopServerFailsWhenClientsConnected() throws Exception {
        PokerServer server = new PokerServer(5555);
        ClientHandler fakeClient = new ClientHandler(null, server);

        CopyOnWriteArrayList<ClientHandler> clients = getClientList(server);
        clients.add(fakeClient);

        assertFalse(server.stopServer(), "server should refuse to stop while clients remain");
        assertTrue(clients.contains(fakeClient), "stop should not remove connected clients");
    }

    private CopyOnWriteArrayList<ClientHandler> getClientList(PokerServer server) throws Exception {
        Field field = PokerServer.class.getDeclaredField("clients");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        CopyOnWriteArrayList<ClientHandler> list =
                (CopyOnWriteArrayList<ClientHandler>) field.get(server);
        return list;
    }
}
