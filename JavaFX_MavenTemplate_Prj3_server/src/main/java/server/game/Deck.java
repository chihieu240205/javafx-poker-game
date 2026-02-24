package server.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import shared.Card;
import shared.Rank;
import shared.Suit;

public class Deck {

    private Deque<Card> cards = new ArrayDeque<>();

    public Deck() {
        reshuffle();
    }

    private void reshuffle() {
        cards.clear();
        cards.addAll(buildShuffledDeck());
    }

    private List<Card> buildShuffledDeck() {
        List<Card> all = new ArrayList<>();
        for (Suit s : Suit.values())
            for (Rank r : Rank.values())
                all.add(new Card(r, s));
        Collections.shuffle(all);
        return all;
    }

    public Card deal() {
        if (cards.isEmpty()) {
            reshuffle();
        }
        return cards.removeFirst();
    }

    public List<Card> dealHand(int n) {
        if (cards.size() < n) {
            reshuffle();
        }

        List<Card> h = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            h.add(cards.removeFirst());
        }
        return h;
    }

    public int remainingCards() {
        return cards.size();
    }
}
