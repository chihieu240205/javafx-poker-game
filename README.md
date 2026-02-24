# CS342Pr3 – Networked 3-Card Poker

This repository contains a complete client/server implementation of a networked 3-card poker game built with Java 11, Maven, and JavaFX. The project is split into two Maven modules:

- `JavaFX_MavenTemplate_Prj3_server` – hosts the authoritative game server, poker logic, and a JavaFX monitor UI.
- `JavaFX_MavenTemplate_Prj3_client` – provides the desktop client, including gameplay UI, audio, and networking.

Both modules share the same `shared` package so that message classes (`PokerInfo`, `Card`, etc.) stay in sync.

## Repository Layout

| Path | Description |
| ---- | ----------- |
| `JavaFX_MavenTemplate_Prj3_server/` | Server application, network stack, dealer monitor UI, and server-side tests. |
| `JavaFX_MavenTemplate_Prj3_client/` | Player-facing client, UI scenes (welcome, gameplay, results), audio assets, and client-side tests. |
| `README.md` | This overview—start here for prerequisites and high-level workflow. |
| `<module>/README.md` | Module-specific guides with build/run/test instructions and architectural notes. |

## Prerequisites

- Java 11 (or newer compatible with JavaFX 19)
- Maven 3.6+
- macOS/Windows/Linux capable of running JavaFX desktop apps

## Quick Start

1. **Clone & install dependencies** – Maven downloads JavaFX on the first build, so ensure you have a stable internet connection the first time you run `mvn`.
2. **Start the server**
   ```bash
   cd JavaFX_MavenTemplate_Prj3_server
   mvn javafx:run
   ```
   Use the intro UI to choose a port (default 5555) and launch the monitor window.
3. **Start the client**
   ```bash
   cd JavaFX_MavenTemplate_Prj3_client
   mvn javafx:run
   ```
   Connect to the server’s host/port from the welcome screen, place bets, and play rounds.  
   - Use **keyboard shortcuts** for instant actions: `Q` (Quick Bet), `D` (Deal), `P` (Play), `F` (Fold), `C` (Continue).  
   - Scene changes now flow through a **theme-aware loading overlay** with lighthearted dealer quips, so switching between gameplay/result screens feels smooth without harsh flashes.

## Testing

Each Maven module owns its tests. Run them independently so UI/network dependencies stay isolated.

```bash
# Server module tests (game logic, server state guards)
cd JavaFX_MavenTemplate_Prj3_server
mvn test

# Client module tests (networking and controller behaviors)
cd JavaFX_MavenTemplate_Prj3_client
mvn test
```

> **Heads up:** Client-side UI tests require JavaFX to initialize; on headless environments you may need to configure additional JVM flags or skip those tests.

## Next Steps

- See `JavaFX_MavenTemplate_Prj3_server/README.md` for details about the server UI, networking pipeline, and monitoring features.
- See `JavaFX_MavenTemplate_Prj3_client/README.md` for UI flow descriptions, audio controls, and client-side architecture.

Happy coding and good luck at the tables!
