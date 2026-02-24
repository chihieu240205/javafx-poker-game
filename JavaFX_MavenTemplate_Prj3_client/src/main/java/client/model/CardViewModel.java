package client.model;

import javafx.scene.image.Image;
import shared.Card;

public class CardViewModel {

    private final Image image;
    private static boolean useNewBack = false;

    public static void setUseNewBack(boolean useNewBackValue) {
        useNewBack = useNewBackValue;
    }

    public CardViewModel(Card card, boolean faceDown) {

        if (faceDown || card == null) {
            String backPath = useNewBack
                    ? "/cards/BACK_NEW.png"
                    : "/cards/BACK.png";
            image = safeLoad(backPath);
            return;
        }

        String filename = getFileName(card);
        image = safeLoad("/cards/" + filename);
    }

    public Image getImage() {
        return image;
    }

    private Image safeLoad(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            System.out.println("MISSING IMAGE: " + path);
            return null;
        }
    }

    private String getFileName(Card c) {
        String r;
        switch (c.getRank()) {
            case ACE:   r = "A"; break;
            case KING:  r = "K"; break;
            case QUEEN: r = "Q"; break;
            case JACK:  r = "J"; break;
            case TEN:   r = "10"; break;
            case NINE:  r = "9"; break;
            case EIGHT: r = "8"; break;
            case SEVEN: r = "7"; break;
            case SIX:   r = "6"; break;
            case FIVE:  r = "5"; break;
            case FOUR:  r = "4"; break;
            case THREE: r = "3"; break;
            case TWO:   r = "2"; break;
            default:    r = "";
        }

        String s;
        switch (c.getSuit()) {
            case CLUBS:    s = "C"; break;
            case DIAMONDS: s = "D"; break;
            case HEARTS:   s = "H"; break;
            case SPADES:   s = "S"; break;
            default:       s = "";
        }

        return r + s + ".png";
    }
}
