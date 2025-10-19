package game;

import chat.client.ClientHandler;
import chat.server.Server;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for creating a player instance with a name and number of points.
 *
 * @author Stefan
 */
public class Player {
    private String name;
    private ClientHandler client;
    private int points = 0;
    private int index;
    private List<Card> playerHand = new ArrayList();
    private boolean isActive = false; // is it the players turn?
    private boolean isInRound = true; // is the player still in the round?
    private boolean isProtected = false; // is the player protected by the handmaid?
    private Long daysFromLastDate; // use wrapper class to check for empty variable

    // constructor
    public Player(ClientHandler client, String name) {
        this.client = client;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getPoints() {
        return this.points;
    }

    /**
     * NOT a getter, but increases the points of the player by 1.
     */
    public void getOnePoint() {
        this.points = this.points + 1;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    /**
     * Returns the card in the players hand.
     * Is only used, if the player has exactly one card in hand.
     * @return
     */
    public Card getCard() {
        if (this.playerHand.get(0) != null) {
            return this.playerHand.get(0);
        }
        return this.playerHand.get(1);
    }

    public List<Card> getPlayerHand() {
        return this.playerHand;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive() {
        this.isActive = true;
    }

    public void setInactive() {
        this.isActive = false;
    }

    public boolean isInRound() {
        return this.isInRound;
    }

    /**
     * Set true or false
     * @param inRound
     */
    public void setInRound(boolean inRound) {
        this.isInRound = inRound;
    }

    public boolean isProtected() {
        return this.isProtected;
    }

    public void protect() {
        this.isProtected = true;
    }

    public void removeProtection() {
        this.isProtected = false;
    }

    public Long getDaysFromLastDate() {
        return this.daysFromLastDate;
    }

    public void setDaysFromLastDate(long days) {
        this.daysFromLastDate = days;
    }

    public boolean playerHandContains(String cardName) {
        for (Card card : playerHand) {
            if (card.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    Card seekCard;

    public Card getCardByName(String cardName) {
        for (Card card : playerHand) {
            if (card.getName().equals(cardName)) {
                seekCard = card;
            }
        }
        return seekCard;
    }

    /**
     * Check if the player has both either King or Prince and the Countess in Hand
     * @return boolean
     */
    public boolean mustPlayCountess() {
        if ((this.playerHandContains("King") || this.playerHandContains("Prince")) && this.playerHandContains("Countess")) {
            return true;
        }
        return false;
    }

    /**
     * Standard draw card method:
     * First, check if the deck is empty.
     * If no, continue to draw a card by adding the top deck card to the player hand
     * If the deck is empty, take the reserve card.
     *
     * @param deck
     */
    public void drawCard(Deck deck) {
        if (!deck.isEmpty()) {
            this.getPlayerHand().add(deck.getTopCard());
            sendMessage("You drew the " + deck.getTopCard().getName());
            deck.removeTopCard();
            if (deck.getLength() == 0) {
                deck.setEmpty();
            }
        } else {
            this.getPlayerHand().add(deck.getResevereCard());
            sendMessage("You drew the " + deck.getResevereCard().getName());
        }
    }

    /**
     * Can only occur when the prince is played.
     * Takes the card in the players hand and adds it to the pile that
     */
    public void discardCard() {
        Server.allPlayedCards.add(this.getCard());
        this.playerHand.remove(this.getCard());
    }

    /**
     * Removes a specific card from the player's hand.
     * Is used after every card play method, so the card is removed after it is played.
     * @param cardName
     */
    public void removeCardFromHand(String cardName) {
        this.playerHand.remove(getCardByName(cardName));
    }

    /**
     * Gets all cards in the player's hand and prints the names on the console.
     */
    public void showHand() {
        for (Card card : playerHand) {
            sendMessage(card.getName());
        }
    }

    /**
     * Method for the Priest: shows the card of a specific target player.
     *
     * @param targetPlayer
     */
    public void showCardToPlayer(String targetPlayer) {
        for (ClientHandler client : Server.clients) {
            if (client.getName().equals(targetPlayer)) {
                this.sendMessage(targetPlayer + "'s card is: " + client.getPlayer().getCard().getName());
            }
        }
    }

    public void sendMessage(String message) {
        client.sendMessage(message);
    }
}