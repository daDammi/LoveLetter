package game;

import chat.server.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private List<Card> deck = new ArrayList<>();
    // if there are two players, three cards get set aside
    private List<Card> sideCards = new ArrayList<>();
    private Card resevereCard;
    private boolean isEmpty = false;

    public List<Card> getDeck() {
        return this.deck;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public void setEmpty() {
        this.isEmpty = true;
    }

    public void setReserveCard() {
        this.resevereCard = this.getTopCard();
        this.removeTopCard();
    }

    public Card getResevereCard() {
        return this.resevereCard;
    }

    /**
     * Construct an array list containing the deck
     */
    public void buildDeck() {
        deck.clear();
        this.deck.add(new Card("Princess", 8, 1));

        this.deck.add(new Card("Countess", 7, 1));

        this.deck.add(new Card("King", 6, 1));

        this.deck.add(new Card("Prince", 5, 2));
        this.deck.add(new Card("Prince", 5, 2));

        this.deck.add(new Card("Handmaid", 4, 2));
        this.deck.add(new Card("Handmaid", 4, 2));

        this.deck.add(new Card("Baron", 3, 2));
        this.deck.add(new Card("Baron", 3, 2));

        this.deck.add(new Card("Priest", 2, 2));
        this.deck.add(new Card("Priest", 2, 2));

        this.deck.add(new Card("Guard", 1, 5));
        this.deck.add(new Card("Guard", 1, 5));
        this.deck.add(new Card("Guard", 1, 5));
        this.deck.add(new Card("Guard", 1, 5));
        this.deck.add(new Card("Guard", 1, 5));
    }
    
    public void shuffleDeck() {
        Collections.shuffle(this.deck);
    }

    public int getLength() {
        return this.deck.size();
    }

    /**
     * Returns the last card of the deck array.
     * @return Card
     */
    public Card getTopCard() {
        return this.deck.get(this.deck.size() - 1);
    }

    /**
     * Removes the last card of the deck array.
     */
    public void removeTopCard() {
        this.deck.remove(this.deck.size() - 1);
    }

    /**
     * If there are only two players, the top three cards of the deck are put aside.
     * The cards are added to the allPlayedCards list, visible to all players.
     */
    public void putThreeCardsAside() {
        sideCards.clear();
        for (int i = 0; i < 3; i++) {
            this.sideCards.add(this.getTopCard());
            Server.allPlayedCards.add(this.getTopCard());
            this.removeTopCard();
        }
    }


}
