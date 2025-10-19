package chat.server;

import chat.client.ClientHandler;
import game.Card;
import game.Deck;
import game.Player;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server class for creating a new server object.
 * Run the server on a server object with the .runServer method.
 */
public class Server {
    //define PORT only once on server side, so all clients can grab this final variable
    public static final int PORT = 8000;
    private static final int maxClients = 4;
    public static boolean acceptingClients = true;
    public static Deck deck = new Deck();
    private ServerSocket serverSocket;
    public static List<ClientHandler> clients = new ArrayList<>();
    public static List<String> playerNames = new ArrayList<>();
    private ExecutorService pool;
    public static volatile boolean gameRunning = false;
    public static final List<String> allCards = new ArrayList<>(Arrays.asList("Princess", "Countess", "King", "Prince", "Handmaid", "Baron", "Priest", "Guard"));
    public static final List<String> gameCommands = new ArrayList<>(Arrays.asList("endGame", "points", "hand", "showHand", "allCards", "active"));

    /**
     * Starts the server:
     * Creates a server object to then wait for incoming clients.
     * Creates a ClientHandler for the incoming client to add to an ArrayList of clients with a variable maximum of clients.
     * Uses the ExecuterService to manage a pool of async client threads.
     */
    public void runServer() {

        Thread serverThread = new Thread(() -> {
            try {
                // start the server and listen for new clients on the defined PORT
                serverSocket = new ServerSocket(PORT);
                pool = Executors.newCachedThreadPool();
                System.out.println("Server started. Waiting for players...");

                while (acceptingClients && clients.size() < maxClients) {

                    // the serverSocket waits for incoming clients and accepts once a client connects to the PORT
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Player joined");

                    // create new client handler object and add it to the array list of clients
                    ClientHandler client = new ClientHandler(clientSocket);
                    clients.add(client);
                    pool.execute(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread gameThread = new Thread(() -> {
            while (true) {
                if (gameRunning) {
                    runGame();
                }
            }
        });
        serverThread.start();
        gameThread.start();
    }

    /**
     * Sends a message to all clients except the one who has written the message (sender).
     *
     * @param message to be sent
     * @param sender  that will NOT get the message
     */
    public static void sendToAllClientsExceptSender(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Use in the server: send a message to all clients
     *
     * @param message to be sent
     */
    public static void sendToAllClients(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Sends a message from a player to another specific player
     *
     * @param sender   who is whispering
     * @param message  to be sent
     * @param receiver who is receiving the whispered message
     */
    public static void whisperToPlayer(ClientHandler sender, String message, String receiver) {
        for (ClientHandler client : clients) {
            if (client.getName().equals(receiver)) {
                client.sendMessage(sender.getName() + " whispers: " + message);
            }
        }
    }

    /**
     * Excludes the players that got kicked out of a round.
     * Uses the number to check, if the round ends before the deck is empty
     * @return number of players still playing the round
     */
    private int countPlayersInRound() {
        int count = 0;
        for (ClientHandler client : clients) {
            if (client.getPlayer().isInRound()) {
                count++;
            }
        }
        return count;
    }

    Player seekPlayer;

    /**
     * @param index to find a player with
     * @return player with the index
     */
    public Player getPlayerByIndex(int index) {
        for (ClientHandler client : clients) {
            if (client.getPlayer().getIndex() == index) {
                seekPlayer = client.getPlayer();
            }
        }
        return seekPlayer;
    }

    int numberOfPlayers;
    int winningPoints;
    int index;
    int turnCount = 1;
    public static Player activePlayer;
    public static List<Card> allPlayedCards = new ArrayList<>();
    long daysFromLastDate = 1000000;

    /**
     * Sets up the game before running the game loops:
     * sends "Game started" message; counts and initializes number of players;
     * determines points needed for a win; determines the starting player etc.
     */
    public void initializeGame() {
        System.out.println("Game started!");
        sendToAllClients("Game started. Welcome to Love Letter!");

        numberOfPlayers = clients.size();
        index = 0;

        //determine the playing order by giving every player a fixed index for the game
        for (ClientHandler client : clients) {
            client.getPlayer().setIndex(index);
            index++;
        }

        switch (numberOfPlayers) {
            case 2:
                winningPoints = 5;
                break;
            case 3:
                winningPoints = 4;
                break;
            case 4:
                winningPoints = 3;
                break;
        }

        for (ClientHandler client : clients) {
            if (client.getPlayer().getDaysFromLastDate() < daysFromLastDate) {
                daysFromLastDate = client.getPlayer().getDaysFromLastDate();
                activePlayer = client.getPlayer();
            }
        }

        index = activePlayer.getIndex();
        activePlayer.setActive();
    }

    int playerInRoundCount;
    int roundCount = 1;

    /**
     * Sets up a new game round: first, make sure that every players hand is empty.
     * Reset the turn count, prepare the deck (clear, build, shuffle) set 3 cards aside, if needed and draw the reserve card etc.
     */
    private void initializeRound() {
        //clear the list of all cards
        allPlayedCards.clear();
        turnCount = 1;

        //build a new deck to play with
        deck.buildDeck();
        deck.shuffleDeck();

        if (numberOfPlayers == 2) {
            deck.putThreeCardsAside();
        }
        deck.setReserveCard();

        //empty all player hands, give every player his/her starting card, put every player back into the round
        for (ClientHandler client : clients) {
            client.getPlayer().getPlayerHand().clear();
            client.getPlayer().drawCard(deck);
            client.getPlayer().setInRound(true);
        }
        //set the count back to all players
        playerInRoundCount = countPlayersInRound();

    }

    /**
     * Determines logic for one round: iterate through all players and check if player is still in the round.
     * If yes, the round is played and ends only when the active player plays a card.
     * Afterward increment turn count and player index.
     * Before the next round: increment player index to determine the next player;
     * increment the round count; set next player active.
     */
    public void playRound() {
        //iterate through every client
        for (ClientHandler client : clients) {
            //only go into the play loop, if the client player is active
            if (client.getPlayer().isActive() && playerInRoundCount > 1) {
                sendToAllClients("Round " + roundCount + ", turn " + turnCount + ": " + client.getPlayer().getName());
                client.getPlayer().sendMessage("It's your turn! Play a card by writing '/' and the name of the card.");
                client.getPlayer().drawCard(deck);
                //
                if (client.getPlayer().isProtected()) {
                    client.getPlayer().removeProtection();
                }
                //stay in this loop until the player plays a card
                while (client.getPlayer().isActive()) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                turnCount++;
                //increase the index for the next player
                index++;
            }
            //set the next player active, but only if he wasn't kicked out for the round, otherwise increase the index and check the next player
            while (!getPlayerByIndex(index % numberOfPlayers).isInRound()) {
                index++;
            }
            //if the deck is empty, the round ends here
            if (!deck.isEmpty()) {
                activePlayer = getPlayerByIndex(index % numberOfPlayers);
                activePlayer.setActive();
            }
            playerInRoundCount = countPlayersInRound();
        }
    }

    int maxPoints = 0;
    int maxCardValue = 0;
    Player maxCardPLayer;
    Player winningPlayer;

    /**
     * Complete game: start with initializeGame().
     * Play rounds until one player has the points needed to win (determined in initializeGame()).
     * Play one round until the deck is empty. After every round assign one point.
     * After the game set gameRunning = false.
     */
    public void runGame() {
        initializeGame();
        // looping the rounds
        while (maxPoints != winningPoints) {
            initializeRound();
            //looping the turn
            while (!deck.isEmpty() && playerInRoundCount > 1) {
                playRound();
            }
            // THE FOLLOWING LOGIC EXECUTES AFTER EVERY ROUND:
            // Get the player with the highest value card. If there is only one player left in the round, he/she has automatically the highest value card.
            maxCardValue = 0;
            maxCardPLayer = null;
            for (ClientHandler client : clients) {
                if (client.getPlayer().isInRound()) {
                    if (client.getPlayer().getCard().getValue() > maxCardValue) {
                        maxCardValue = client.getPlayer().getCard().getValue();
                        maxCardPLayer = client.getPlayer();
                    }
                }
            }
            // Reward the point to the player who won the round. That player is also the starting player for the next round.
            sendToAllClients("End of round " + roundCount + ". " + maxCardPLayer.getName() + " gets one point.");
            maxCardPLayer.getOnePoint();
            activePlayer = maxCardPLayer;
            index = maxCardPLayer.getIndex();
            activePlayer.setActive();

            // Check all players for the most points to redetermine the player with the most points and the number of the points.
            // If the points are enough for a win, the while loop exits here and the game ends.
            for (ClientHandler client : clients) {
                client.sendMessage("You have " + client.getPlayer().getPoints() + " points.");
                if (client.getPlayer().getPoints() > maxPoints) {
                    maxPoints = client.getPlayer().getPoints();
                    winningPlayer = client.getPlayer();
                }
            }
            roundCount++;
        }

        // After the game, declare the winner and reset all player and game variables.
        sendToAllClients("The game is over. The winner is: " + winningPlayer.getName());
        for (ClientHandler client : clients) {
            client.getPlayer().setInactive();
            client.getPlayer().getPlayerHand().clear();
            client.getPlayer().removeProtection();
        }

        deck.getDeck().clear();
        allPlayedCards.clear();
        gameRunning = false;
        acceptingClients = true;
        maxPoints = 0;
    }
}


