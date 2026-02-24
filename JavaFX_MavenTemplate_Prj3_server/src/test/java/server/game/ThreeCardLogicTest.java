package server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import shared.Card;
import shared.Rank;
import shared.Suit;

class ThreeCardLogicTest {

    private List<Card> straightFlush() {
        return Arrays.asList(
                new Card(Rank.NINE, Suit.HEARTS),
                new Card(Rank.TEN, Suit.HEARTS),
                new Card(Rank.JACK, Suit.HEARTS));
    }

    private List<Card> dealerNoQualify() {
        return Arrays.asList(
                new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.FIVE, Suit.DIAMONDS),
                new Card(Rank.SEVEN, Suit.SPADES));
    }

    private List<Card> playerHighCards() {
        return Arrays.asList(
                new Card(Rank.KING, Suit.CLUBS),
                new Card(Rank.NINE, Suit.DIAMONDS),
                new Card(Rank.FOUR, Suit.SPADES));
    }

    @Test
    void evalHandRecognizesEachRank() {
        assertEquals(5, ThreeCardLogic.evalHand(straightFlush()), "straight flush should score 5");

        List<Card> trips = Arrays.asList(
                new Card(Rank.SEVEN, Suit.HEARTS),
                new Card(Rank.SEVEN, Suit.CLUBS),
                new Card(Rank.SEVEN, Suit.DIAMONDS));
        assertEquals(4, ThreeCardLogic.evalHand(trips), "three of a kind should score 4");

        List<Card> straight = Arrays.asList(
                new Card(Rank.FIVE, Suit.HEARTS),
                new Card(Rank.SIX, Suit.SPADES),
                new Card(Rank.SEVEN, Suit.CLUBS));
        assertEquals(3, ThreeCardLogic.evalHand(straight), "straight should score 3");

        List<Card> flush = Arrays.asList(
                new Card(Rank.TWO, Suit.SPADES),
                new Card(Rank.SIX, Suit.SPADES),
                new Card(Rank.KING, Suit.SPADES));
        assertEquals(2, ThreeCardLogic.evalHand(flush), "flush should score 2");

        List<Card> pair = Arrays.asList(
                new Card(Rank.QUEEN, Suit.HEARTS),
                new Card(Rank.QUEEN, Suit.SPADES),
                new Card(Rank.THREE, Suit.CLUBS));
        assertEquals(1, ThreeCardLogic.evalHand(pair), "pair should score 1");

        assertEquals(0, ThreeCardLogic.evalHand(playerHighCards()), "high card should score 0");
    }

    @Test
    void evalPPWinningsPaysCorrectMultiplier() {
        int bet = 10;
        assertEquals(400, ThreeCardLogic.evalPPWinnings(straightFlush(), bet), "straight flush pays 40x");
        assertEquals(0, ThreeCardLogic.evalPPWinnings(playerHighCards(), bet), "high card pays nothing");
    }

    @Test
    void compareHandsAccountsForDealerQualification() {
        assertEquals(false, ThreeCardLogic.dealerQualifies(dealerNoQualify()));
        assertEquals(0, ThreeCardLogic.compareHands(dealerNoQualify(), playerHighCards()));

        List<Card> dealerTrips = Arrays.asList(
                new Card(Rank.THREE, Suit.CLUBS),
                new Card(Rank.THREE, Suit.HEARTS),
                new Card(Rank.THREE, Suit.DIAMONDS));
        assertEquals(-1, ThreeCardLogic.compareHands(dealerTrips, playerHighCards()));

        assertEquals(0, ThreeCardLogic.compareHands(playerHighCards(), playerHighCards()));
    }
}
