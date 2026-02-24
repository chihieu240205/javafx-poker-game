# JavaFX_MavenTemplate_Prj3_server

Server-side module for the Networked 3-Card Poker project. It exposes a JavaFX control panel plus a socket-based backend that enforces game rules, tracks player sessions, and pushes updates to connected clients.

## Highlights

- **UI entry point** – `server.ui.ServerApp` launches the intro screen (start/stop controls + port selector) and the monitor window (live client list, round history, leaderboard).
- **Networking stack** – `server.net.PokerServer` accepts TCP clients, spins up `ClientHandler` threads, and synchronizes with the monitor controller for live status updates.
- **Poker logic** – `server.game` contains deterministic deck shuffling, hand evaluation (`ThreeCardLogic`), and per-player state (`PlayerSession`).
- **Shared DTOs** – `shared` package defines serializable classes (`PokerInfo`, `Card`, `RoundOutcome`, etc.) used by both modules.

## Running the Server

```bash
cd JavaFX_MavenTemplate_Prj3_server
mvn javafx:run
```

1. Enter a port (default 5555) and click **Start Server**.  
2. Use **Open Monitor** to view live rounds, player balances, and the leaderboard.  
3. **Stop Server** shuts down only when no clients are connected (mirrors the in-code guard).

## Tests

Unit tests live under `src/test/java`.

- `server/game/ThreeCardLogicTest` – verifies hand ranking and Pair+ payouts.
- `server/net/PokerServerClientStateTest` – ensures `stopServer()` rejects shutdown while clients remain.
- `server/game/DeckTest` – covers deck initialization and shuffle guarantees (provided starter test).

Run tests via:

```bash
cd JavaFX_MavenTemplate_Prj3_server
mvn test
```

## Architecture Notes

| Component | Responsibility |
| --------- | -------------- |
| `server.net.PokerServer` | Accepts sockets, manages thread pool, routes monitor updates, enforces stop/start policy. |
| `server.net.ClientHandler` | Handles a single client’s lifecycle (bets, DEAL/PLAY/FOLD requests) and serializes `PokerInfo` responses. |
| `server.game.PlayerSession` | Stores per-client balance, bets, and hand state between actions. |
| `server.ui.ServerMonitorController` | JavaFX controller for the monitor scene—renders client statuses, round logs, and leaderboard. |

For additional context see inline comments within each package. This module is self-contained: build/run/test commands do not touch the client module.
