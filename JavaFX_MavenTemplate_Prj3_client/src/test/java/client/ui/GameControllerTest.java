package client.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import client.model.ClientGameState;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import shared.Card;
import shared.GamePhase;
import shared.PokerInfo;
import shared.Rank;
import shared.RoundOutcome;
import shared.Suit;

class GameControllerTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void initFx() throws Exception {
        if (FX_STARTED.compareAndSet(false, true)) {
            Path cacheDir = Path.of("target", "javafx-cache");
            Files.createDirectories(cacheDir);
            System.setProperty("javafx.cachedir", cacheDir.toAbsolutePath().toString());
            System.setProperty("prism.order", "sw");

            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX platform failed to start");
            }
        }
    }

    @Test
    void updateViewAppendsPairPlusBonusMessages() throws Exception {
        GameController controller = loadController();

        PokerInfo info = resolvedInfo(true, 10);

        AtomicReference<String> messageText = new AtomicReference<>();
        runOnFx(() -> {
            controller.updateView(info);
            messageText.set(getMessageArea(controller).getText());
        });

        assertTrue(messageText.get().contains("Pair+ bonus included in winnings."),
                "message area should confirm Pair+ bonus");
    }

    @Test
    void updateViewNotesMissingPairPlusBet() throws Exception {
        GameController controller = loadController();

        PokerInfo info = resolvedInfo(false, 0);

        AtomicReference<String> messageText = new AtomicReference<>();
        runOnFx(() -> {
            controller.updateView(info);
            messageText.set(getMessageArea(controller).getText());
        });

        assertTrue(messageText.get().contains("Pair+ bonus not included (no Pair+ bet)."),
                "message area should explain lack of Pair+ bet");
    }

    @Test
    void quickBetShortcutFiresWhenMessageLogFocused() throws Exception {
        GameController controller = loadController();

        runOnFx(() -> {
            TextArea messageArea = getMessageArea(controller);
            TextField anteField = getAnteField(controller);

            KeyEvent baseEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "q", "q", KeyCode.Q,
                    false, false, false, false);
            KeyEvent targetedEvent = (KeyEvent) baseEvent.copyFor(messageArea, messageArea);

            invokeHandleShortcut(controller, targetedEvent);

            assertTrue(anteField.isDisabled(),
                    "Quick Bet shortcut should still fire when message log (non-editable) has focus");
        });
    }

    @Test
    void shortcutsStillFireWhenBetFieldFocused() throws Exception {
        GameController controller = loadController();

        AtomicBoolean dealTriggered = new AtomicBoolean(false);

        runOnFx(() -> {
            TextField anteField = getAnteField(controller);

            KeyEvent quickEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "q", "q", KeyCode.Q,
                    false, false, false, false);
            KeyEvent quickForField = (KeyEvent) quickEvent.copyFor(anteField, anteField);

            invokeHandleShortcut(controller, quickForField);
            assertTrue(anteField.isDisabled(), "Quick Bet shortcut should disable ante field values");

            Button dealButton = getDealButton(controller);
            dealButton.setOnAction(e -> dealTriggered.set(true));

            KeyEvent dealEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "d", "d", KeyCode.D,
                    false, false, false, false);
            KeyEvent dealForField = (KeyEvent) dealEvent.copyFor(anteField, anteField);

            invokeHandleShortcut(controller, dealForField);
        });

        assertTrue(dealTriggered.get(),
                "Deal shortcut should fire even if the ante TextField currently has focus");
    }

    private GameController loadController() throws Exception {
        FXMLLoader loader = new FXMLLoader(GameController.class.getResource("/client/ui/gameplay.fxml"));
        Parent root = loader.load();
        root.getStylesheets().clear();

        GameController controller = loader.getController();
        setPrivateField(controller, "gameState", new ClientGameState());
        return controller;
    }

    private PokerInfo resolvedInfo(boolean includeBonus, int pairPlusBet) {
        PokerInfo info = new PokerInfo();
        info.setGamePhase(GamePhase.RESOLVED);
        info.setPairPlusBet(pairPlusBet);
        info.setAnteBet(10);
        info.setPlayBet(10);
        info.setNetWinnings(40);
        info.setTotalBalance(900);
        info.setRoundResult(RoundOutcome.WIN);
        info.setPlayerHand(sampleHand());
        info.setDealerHand(sampleHand());

        List<String> messages = new ArrayList<>();
        messages.add("Round complete");
        if (includeBonus) {
            messages.add("Pair-Plus bonus: 30");
        }
        info.setMessages(messages);
        return info;
    }

    private List<Card> sampleHand() {
        return Arrays.asList(
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.KING, Suit.HEARTS),
                new Card(Rank.QUEEN, Suit.CLUBS));
    }

    private TextArea getMessageArea(GameController controller) {
        return (TextArea) getPrivateField(controller, "messageArea");
    }

    private TextField getAnteField(GameController controller) {
        return (TextField) getPrivateField(controller, "anteField");
    }

    private Button getDealButton(GameController controller) {
        return (Button) getPrivateField(controller, "dealButton");
    }

    private Object getPrivateField(GameController controller, String fieldName) {
        try {
            var field = GameController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(controller);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to access " + fieldName, e);
        }
    }

    private void setPrivateField(GameController controller, String fieldName, Object value) {
        try {
            var field = GameController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(controller, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set " + fieldName, e);
        }
    }

    private void invokeHandleShortcut(GameController controller, KeyEvent event) {
        try {
            var method = GameController.class.getDeclaredMethod("handleShortcut", KeyEvent.class);
            method.setAccessible(true);
            method.invoke(controller, event);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to invoke handleShortcut", e);
        }
    }

    private void runOnFx(Runnable task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Timed out waiting for FX operation");
        }
        if (error.get() != null) {
            fail(error.get().getMessage(), error.get());
        }
    }
}
