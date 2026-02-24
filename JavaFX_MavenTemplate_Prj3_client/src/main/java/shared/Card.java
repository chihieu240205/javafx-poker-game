package shared;

import java.io.Serializable;

public class Card implements Serializable {
    private Rank rank;
    private Suit suit;

    public Card(Rank r, Suit s) {
        this.rank = r;
        this.suit = s;
    }

    public Rank getRank() { return rank; }
    public Suit getSuit() { return suit; }
}
