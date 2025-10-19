package game;

public class Card {
    String name;
    int value;
    int amount;

    //constructor
    public Card(String name, int value, int amount) {
        this.name = name;
        this.value = value;
        this.amount = amount;
    }

    public String getName() {
        return this.name;
    }

    public int getValue() {
        return this.value;
    }

}