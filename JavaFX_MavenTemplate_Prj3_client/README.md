# JavaFX_MavenTemplate_Prj3_client

Player-facing desktop client for the Networked 3-Card Poker experience. Built on JavaFX with modular scenes, observable game state, and a socket connection to the poker server.

## Features

- **Scene flow** – `ClientApp` loads three FXML scenes:
  - Welcome: enter host/port, connect, and view session tips.
  - Gameplay: manage bets, reveal cards, read round logs, and toggle audio themes.
  - Results: recap the previous round and continue playing.
- **Rich UI controls** – bet inputs with validation, Pair+ summary banner, round information table, dealer/player rank labels, and expandable message area.
- **Audio integration** – `AudioManager` plays background music, click/reveal effects, and exposes separate volume sliders (music vs. SFX) in the **Audio** menu.
- **Expressive UX touches** – dealer chat panel narrates each round with humorous quips, a theme-aware loading overlay bridges scene changes with playful copy, and keyboard shortcuts (`Q`/`D`/`P`/`F`/`C`) trigger the primary actions without reaching for the mouse.
- **Networking** – `PokerClientConnection` serializes `PokerInfo` objects and keeps a listener thread that pushes updates directly into `GameController`.
- **Shared models** – `client.model` mirrors server data (`ClientGameState`, `CardViewModel`, `ThemeVariant`) to keep UI reactive.

## Running the Client

```bash
cd JavaFX_MavenTemplate_Prj3_client
mvn javafx:run
```

1. Launch the server first (see top-level README).  
2. In the welcome screen, enter the server host/port and connect.  
3. Set Ante/Pair+ bets, click **Deal**, and play each round. The message area summarizes Pair+ status, round outcomes, and validation errors.

## Tests

Unit tests live in `src/test/java`.

- `client/net/PokerClientConnectionTest` – verifies that the connection writes/reads `PokerInfo` payloads using mocked streams.
- `client/ui/GameControllerTest` – exercises key UI messaging paths (Pair+ bonus copy). Requires JavaFX to start; on headless environments you may need to supply additional JVM flags or disable the test.

Run tests with:

```bash
cd JavaFX_MavenTemplate_Prj3_client
mvn test
```

## Package Guide

| Package | Description |
| ------- | ----------- |
| `client.ui` | All JavaFX controllers plus `ClientApp`, `AudioManager`, and UI helpers. FXML layouts live under `src/main/resources/client/ui`. |
| `client.model` | Lightweight observable objects backing the UI (game state, card view, theme state). |
| `client.net` | Socket client abstraction that the controllers use to talk to the server. |
| `shared` | Serializable DTOs shared with the server module. Keep this package synchronized between modules. |

Refer to the root README for high-level workflow and to the server README for backend details.
