package server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import shared.Card;
import shared.Rank;

public class ThreeCardLogic {

    public static int evalHand(List<Card> hand) {
        if (hand == null || hand.size() != 3) return 0;

        List<Card> sorted = new ArrayList<>(hand);
        sorted.sort((a, b) -> Integer.compare(getRankValue(a.getRank()), getRankValue(b.getRank())));

        boolean isFlush = sorted.get(0).getSuit() == sorted.get(1).getSuit() &&
                          sorted.get(1).getSuit() == sorted.get(2).getSuit();

        int r0 = getRankValue(sorted.get(0).getRank());
        int r1 = getRankValue(sorted.get(1).getRank());
        int r2 = getRankValue(sorted.get(2).getRank());

        boolean isStraight = false;
        boolean hasAce = sorted.stream().anyMatch(c -> c.getRank() == Rank.ACE);
        boolean hasTwo = sorted.stream().anyMatch(c -> c.getRank() == Rank.TWO);
        boolean hasThree = sorted.stream().anyMatch(c -> c.getRank() == Rank.THREE);

        if (r1 == r0 + 1 && r2 == r1 + 1) {
            isStraight = true;
        } else if (hasAce && hasTwo && hasThree) {
            isStraight = true;
        }

        boolean isThreeOfAKind = r0 == r1 && r1 == r2;
        boolean isPair = (r0 == r1) || (r1 == r2);

        if (isStraight && isFlush) return 5;
        if (isThreeOfAKind) return 4;
        if (isStraight) return 3;
        if (isFlush) return 2;
        if (isPair) return 1;
        return 0;
    }

    public static int evalPPWinnings(List<Card> hand, int bet) {
        if (bet == 0) return 0;

        int handRank = evalHand(hand);
        switch (handRank) {
            case 5: return bet * 40;
            case 4: return bet * 30;
            case 3: return bet * 6;
            case 2: return bet * 3;
            case 1: return bet;
            default: return 0;
        }
    }

    public static int compareHands(List<Card> dealer, List<Card> player) {
        if (!dealerQualifies(dealer)) {
            return 0;
        }

        int dealerRank = evalHand(dealer);
        int playerRank = evalHand(player);

        if (playerRank > dealerRank) return 1;
        if (playerRank < dealerRank) return -1;

        return compareHighCards(player, dealer);
    }

    public static boolean dealerQualifies(List<Card> dealer) {
        if (dealer == null || dealer.size() != 3) return false;

        int rank = evalHand(dealer);
        if (rank >= 1) return true;

        int maxRank = dealer.stream()
            .mapToInt(c -> getRankValue(c.getRank()))
            .max()
            .orElse(0);

        int queenVal = getRankValue(Rank.QUEEN);
        return maxRank >= queenVal;
    }

    private static int compareHighCards(List<Card> hand1, List<Card> hand2) {
        List<Integer> ranks1 = new ArrayList<>();
        List<Integer> ranks2 = new ArrayList<>();

        for (Card c : hand1) {
            int val = getRankValue(c.getRank());
            ranks1.add(c.getRank() == Rank.ACE ? 13 : val);
        }
        for (Card c : hand2) {
            int val = getRankValue(c.getRank());
            ranks2.add(c.getRank() == Rank.ACE ? 13 : val);
        }

        ranks1.sort(Collections.reverseOrder());
        ranks2.sort(Collections.reverseOrder());

        for (int i = 0; i < 3; i++) {
            if (ranks1.get(i) > ranks2.get(i)) return 1;
            if (ranks1.get(i) < ranks2.get(i)) return -1;
        }

        return 0;
    }

    private static int getRankValue(Rank rank) {
        return rank.ordinal();
    }
}
