package server.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import shared.Card;

import java.util.List;

public class DeckTest {

    @Test
    public void testDealHandAlwaysHasEnoughCards() {
        Deck deck = new Deck();

        List<Card> hand = deck.dealHand(3);

        assertNotNull(hand);
        assertEquals(3, hand.size());
    }

    @Test
    public void testDeckReshufflesWhenLow() {
        Deck deck = new Deck();

        for (int i = 0; i < 50; i++) {
            deck.deal();
        }

        List<Card> hand = deck.dealHand(3);

        assertEquals(3, hand.size());
    }

    @Test
    public void testRemainingCardsDecreases() {
        Deck deck = new Deck();
        int before = deck.remainingCards();

        deck.deal();

        int after = deck.remainingCards();
        assertEquals(before - 1, after);
    }
}
