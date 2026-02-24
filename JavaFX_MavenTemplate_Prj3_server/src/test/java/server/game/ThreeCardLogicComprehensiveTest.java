package server.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import shared.Card;
import shared.Rank;
import shared.Suit;

class ThreeCardLogicComprehensiveTest {

    private List<Card> straightFlush;
    private List<Card> trips;
    private List<Card> straight;
    private List<Card> aceLowStraight;
    private List<Card> flush;
    private List<Card> pair;
    private List<Card> highCards;
    private List<Card> dealerNoQualify;
    private List<Card> dealerQualifyingHigh;

    @BeforeEach
    void setUpHands() {
        straightFlush = hand(
                c(Rank.NINE, Suit.HEARTS),
                c(Rank.TEN, Suit.HEARTS),
                c(Rank.JACK, Suit.HEARTS));

        trips = hand(
                c(Rank.SEVEN, Suit.HEARTS),
                c(Rank.SEVEN, Suit.CLUBS),
                c(Rank.SEVEN, Suit.DIAMONDS));

        straight = hand(
                c(Rank.FIVE, Suit.HEARTS),
                c(Rank.SIX, Suit.SPADES),
                c(Rank.SEVEN, Suit.CLUBS));

        aceLowStraight = hand(
                c(Rank.ACE, Suit.SPADES),
                c(Rank.TWO, Suit.DIAMONDS),
                c(Rank.THREE, Suit.CLUBS));

        flush = hand(
                c(Rank.TWO, Suit.SPADES),
                c(Rank.SIX, Suit.SPADES),
                c(Rank.KING, Suit.SPADES));

        pair = hand(
                c(Rank.QUEEN, Suit.HEARTS),
                c(Rank.QUEEN, Suit.SPADES),
                c(Rank.THREE, Suit.CLUBS));

        highCards = hand(
                c(Rank.KING, Suit.CLUBS),
                c(Rank.NINE, Suit.DIAMONDS),
                c(Rank.FOUR, Suit.SPADES));

        dealerNoQualify = hand(
                c(Rank.TWO, Suit.CLUBS),
                c(Rank.FIVE, Suit.DIAMONDS),
                c(Rank.SEVEN, Suit.SPADES));

        dealerQualifyingHigh = hand(
                c(Rank.QUEEN, Suit.CLUBS),
                c(Rank.FOUR, Suit.HEARTS),
                c(Rank.THREE, Suit.SPADES));
    }

    @Test
    @DisplayName("evalHand identifies every ranking including A-2-3 straight and invalid input")
    void evalHandRecognizesAllRanksAndEdges() {
        assertEquals(5, ThreeCardLogic.evalHand(straightFlush), "Straight flush should score 5");
        assertEquals(4, ThreeCardLogic.evalHand(trips), "Three of a kind should score 4");
        assertEquals(3, ThreeCardLogic.evalHand(straight), "Straight should score 3");
        assertEquals(3, ThreeCardLogic.evalHand(aceLowStraight), "A-2-3 should be a straight");
        assertEquals(2, ThreeCardLogic.evalHand(flush), "Flush should score 2");
        assertEquals(1, ThreeCardLogic.evalHand(pair), "Pair should score 1");
        assertEquals(0, ThreeCardLogic.evalHand(highCards), "High card should score 0");

        assertEquals(0, ThreeCardLogic.evalHand(new ArrayList<>()), "Invalid hand size returns 0");
        assertEquals(0, ThreeCardLogic.evalHand(Arrays.asList(c(Rank.ACE, Suit.CLUBS))), "Single card hand returns 0");
    }

    @Test
    @DisplayName("evalHand safely handles null input")
    void evalHandHandlesNull() {
        assertEquals(0, ThreeCardLogic.evalHand(null), "Null hand should return rank 0");
    }

    @Test
    @DisplayName("Pair Plus pays correct multipliers for all ranks")
    void evalPPWinningsPaysMultipliers() {
        assertEquals(400, ThreeCardLogic.evalPPWinnings(straightFlush, 10), "Straight flush pays 40x");
        assertEquals(150, ThreeCardLogic.evalPPWinnings(trips, 5), "Trips pay 30x");
        assertEquals(30, ThreeCardLogic.evalPPWinnings(straight, 5), "Straight pays 6x");
        assertEquals(15, ThreeCardLogic.evalPPWinnings(flush, 5), "Flush pays 3x");
        assertEquals(5, ThreeCardLogic.evalPPWinnings(pair, 5), "Pair pays 1x");
        assertEquals(0, ThreeCardLogic.evalPPWinnings(highCards, 25), "High card loses Pair Plus");
    }

    @Test
    @DisplayName("Pair Plus returns 0 when bet is zero even for strong hands")
    void evalPPWinningsReturnsZeroWhenBetIsZero() {
        assertEquals(0, ThreeCardLogic.evalPPWinnings(straightFlush, 0), "Zero bet should pay zero");
        assertEquals(0, ThreeCardLogic.evalPPWinnings(pair, 0), "Zero bet should pay zero on pair");
    }

    @Test
    @DisplayName("Pair Plus handles max bet amount and never returns negative")
    void evalPPWinningsHandlesMaxBetAndNonNegative() {
        int payout = ThreeCardLogic.evalPPWinnings(flush, 25);
        assertEquals(75, payout, "Flush should pay 3x on max bet 25");
        assertTrue(payout >= 0, "Payouts should never be negative");
    }

    @Test
    @DisplayName("Pair Plus scales linearly with bet size")
    void evalPPWinningsScalesWithBet() {
        assertEquals(1000, ThreeCardLogic.evalPPWinnings(straightFlush, 25), "Straight flush should pay 40x on bet 25");
        assertEquals(12, ThreeCardLogic.evalPPWinnings(straight, 2), "Straight pays 6x on small bet");
    }

    @Test
    @DisplayName("Higher ranking player hand beats lower dealer hand")
    void playerHigherRankBeatsDealer() {
        assertEquals(1, ThreeCardLogic.compareHands(flush, straightFlush), "Player straight flush should beat dealer flush");
        assertEquals(1, ThreeCardLogic.compareHands(pair, straight), "Player straight should beat dealer pair");
    }

    @Test
    @DisplayName("Higher ranking dealer hand beats lower player hand")
    void dealerHigherRankBeatsPlayer() {
        assertEquals(-1, ThreeCardLogic.compareHands(straightFlush, flush), "Dealer straight flush should beat player flush");
        assertEquals(-1, ThreeCardLogic.compareHands(trips, straight), "Dealer trips should beat player straight");
    }

    @Test
    @DisplayName("Tie and high-card comparisons are handled correctly")
    void tiesAndHighCardComparisons() {
        assertEquals(0, ThreeCardLogic.compareHands(highCards, highCards), "Identical high-card hands push");

        List<Card> lowerStraight = hand(
                c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.HEARTS),
                c(Rank.SIX, Suit.SPADES));
        assertEquals(1, ThreeCardLogic.compareHands(lowerStraight, straight), "Higher straight should win when ranks equal");
    }

    @Test
    @DisplayName("High-card tiebreakers favor the higher single card when ranks match")
    void highCardTieBreakerApplied() {
        List<Card> dealerHigh = hand(
                c(Rank.QUEEN, Suit.DIAMONDS),
                c(Rank.JACK, Suit.SPADES),
                c(Rank.SEVEN, Suit.CLUBS));

        List<Card> playerHigherHigh = hand(
                c(Rank.KING, Suit.HEARTS),
                c(Rank.TEN, Suit.CLUBS),
                c(Rank.SIX, Suit.SPADES));

        assertEquals(1, ThreeCardLogic.compareHands(dealerHigh, playerHigherHigh), "Higher high card should win when both are high-card hands");
    }

    @Test
    @DisplayName("Pair vs. pair uses kicker/highest ranks to break ties")
    void pairVsPairKickerDecidesWinner() {
        List<Card> dealerPairKings = hand(
                c(Rank.KING, Suit.CLUBS),
                c(Rank.KING, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS));
        List<Card> playerPairQueens = hand(
                c(Rank.QUEEN, Suit.SPADES),
                c(Rank.QUEEN, Suit.DIAMONDS),
                c(Rank.TWO, Suit.HEARTS));

        assertEquals(-1, ThreeCardLogic.compareHands(dealerPairKings, playerPairQueens), "Higher pair (Kings) should beat lower pair (Queens)");
    }

    @Test
    @DisplayName("Ace-high beats King-high when both are high-card hands")
    void aceHighBeatsKingHigh() {
        List<Card> dealerKingHigh = hand(
                c(Rank.KING, Suit.CLUBS),
                c(Rank.NINE, Suit.SPADES),
                c(Rank.SEVEN, Suit.HEARTS));
        List<Card> playerAceHigh = hand(
                c(Rank.ACE, Suit.DIAMONDS),
                c(Rank.NINE, Suit.HEARTS),
                c(Rank.SIX, Suit.CLUBS));

        assertEquals(1, ThreeCardLogic.compareHands(dealerKingHigh, playerAceHigh), "Ace-high should beat King-high");
    }

    @Test
    @DisplayName("Dealer non-qualification results in push")
    void dealerNonQualificationPushes() {
        assertTrue(!ThreeCardLogic.dealerQualifies(dealerNoQualify), "Dealer should not qualify with below Queen high");
        assertEquals(0, ThreeCardLogic.compareHands(dealerNoQualify, highCards), "Non-qualifying dealer should push");
    }

    @Test
    @DisplayName("Dealer qualifies with Queen-high and comparisons still apply")
    void dealerQualificationByQueenHigh() {
        assertTrue(ThreeCardLogic.dealerQualifies(dealerQualifyingHigh), "Dealer qualifies with Queen-high");
        assertEquals(1, ThreeCardLogic.compareHands(dealerQualifyingHigh, straight), "Player straight beats qualifying dealer high-card");
        assertEquals(1, ThreeCardLogic.compareHands(dealerQualifyingHigh, pair), "Player pair should beat qualifying dealer high-card");
    }

    @Test
    @DisplayName("Pair beats high-card, flush beats pair, straight beats flush (3-card ranking order)")
    void rankingOrderSamples() {
        assertEquals(1, ThreeCardLogic.compareHands(dealerQualifyingHigh, pair), "Pair beats high-card");
        assertEquals(1, ThreeCardLogic.compareHands(pair, flush), "Flush beats pair when dealer has pair");
        assertEquals(1, ThreeCardLogic.compareHands(flush, straight), "Straight beats flush in 3-card poker");
    }

    @Test
    @DisplayName("Each rank beats every lower rank when dealer qualifies")
    void rankingHierarchyEnforced() {
        List<Card> straightFlushDealer = straightFlush;
        List<Card> tripsDealer = trips;
        List<Card> straightDealer = straight;
        List<Card> flushDealer = flush;
        List<Card> pairDealer = pair;
        List<Card> highDealer = dealerQualifyingHigh;

        assertEquals(1, ThreeCardLogic.compareHands(tripsDealer, straightFlushDealer));
        assertEquals(1, ThreeCardLogic.compareHands(straightDealer, straightFlushDealer));
        assertEquals(1, ThreeCardLogic.compareHands(flushDealer, straightFlushDealer));
        assertEquals(1, ThreeCardLogic.compareHands(pairDealer, straightFlushDealer));
        assertEquals(1, ThreeCardLogic.compareHands(highDealer, straightFlushDealer));

        assertEquals(-1, ThreeCardLogic.compareHands(tripsDealer, straightDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(tripsDealer, flushDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(tripsDealer, pairDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(tripsDealer, highDealer));

        assertEquals(-1, ThreeCardLogic.compareHands(straightDealer, flushDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(straightDealer, pairDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(straightDealer, highDealer));

        assertEquals(-1, ThreeCardLogic.compareHands(flushDealer, pairDealer));
        assertEquals(-1, ThreeCardLogic.compareHands(flushDealer, highDealer));

        assertEquals(-1, ThreeCardLogic.compareHands(pairDealer, highDealer));
    }

    private Card c(Rank r, Suit s) {
        return new Card(r, s);
    }

    private List<Card> hand(Card... cards) {
        return new ArrayList<>(Arrays.asList(cards));
    }
}
