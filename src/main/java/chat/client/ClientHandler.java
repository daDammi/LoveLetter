package chat.client;

import chat.server.Server;
import game.Card;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * ClientHandler object is needed for every client connecting to the server (see Server class).
 * Constructor needs socket of the client.
 * Contains the complete chat logic.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private String name;
    private String message;
    private PrintWriter output;
    private BufferedReader input;
    private Player player;

    // constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void run() {

        try {
            // reads input from the client
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // sends output to the server
            output = new PrintWriter(socket.getOutputStream(), true);

            // once the client is connected to the server ask the player for his/her name
            sendMessage("Hello there! What's your name?");
            name = input.readLine();
            boolean isValid = isValidName(name);

            // is the first check was not passed, ask for a new name until a valid name is given
            while (!isValid) {
                name = input.readLine();
                isValid = isValidName(name);
            }

            // create a player with the name
            player = new Player(this, name);
            Server.playerNames.add(name);

            // if the name passed all checks, the player is greeted and the rest of the chat is informed
            sendMessage("Welcome " + name + "!");
            getDate();
            sendMessage("Thanks for this personal information and welcome to Love Letter. Type '/help' to show all possible commands.");
            Server.sendToAllClientsExceptSender(name + " has joined.", this);

            // this is the normal chat logic after the reception with alert checks for either empty strings or the client shutdown message "bye"
            while ((message = input.readLine()) != null) {
                if (message.equals("bye"))
                    if (Server.gameRunning) {
                        sendMessage("You can't leave while the game is running. Please finnish the game first.");
                    } else {
                        Server.sendToAllClientsExceptSender(name + " has left the chat!", this);
                        System.out.println("Player disconnected: " + name);
                        Server.clients.remove(this);
                        Server.playerNames.remove(name);
                        socket.close();
                        input.close();
                        output.close();
                        break;
                    }
                else {
                    chatLogic(message);
                }
            }

        } catch (IOException e) {
            // handle client termination without closing it properly (without writing "bye")
            System.err.println("Client disconnected abruptly: " + name);
            Server.sendToAllClientsExceptSender(name + " has left the chat!", this);
        } finally {
            try {
                Server.clients.remove(this);
                Server.playerNames.remove(name);
                socket.close();
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends message via PrintWriter.
     * Used in the sendToAllClientsExceptSender() method.
     *
     * @param message
     */
    public void sendMessage(String message) {
        output.println(message);
        output.flush();
    }

    Player seekPlayer;

    /**
     * @param name of the sought-after player
     * @return Player object with the given name
     */
    public Player getPlayerByName(String name) {
        for (ClientHandler client : Server.clients) {
            if (client.getName().equals(name)) {
                seekPlayer = client.getPlayer();
            }
        }
        return seekPlayer;
    }


    /**
     * Check for a valid name and return false if any naming rules are violated
     *
     * @param name to check
     * @return true only if all the checks pass
     */
    private boolean isValidName(String name) {
        if (name.isEmpty()) {
            sendMessage("Name cannot be empty!");
            return false;
        }
        if (name.contains(" ")) {
            sendMessage("Your name should only consist of one word!");
            return false;
        }
        // check if the name is already taken by another player
        if (Server.playerNames.contains(name)) {
            sendMessage("This name is already taken. Please choose another name!");
            return false;
        }

        // check if the name contains any characters not from the English alphabet; spaces are allowed
        if (!name.matches("[a-zA-Z ]+")) {
            sendMessage("Only characters from the English alphabet are allowed!");
            return false;
        }
        if (name.length() > 25) {
            sendMessage("No more than 25 characters are allowed!");
            return false;
        }
        // if all the checks pass, the name is considered valid
        return true;
    }


    public boolean isGameCommand(String command) {
        if (Server.gameCommands.contains(command)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isCardCommand(String command) {
        if (Server.allCards.contains(command)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isWhisperCommand(String command) {
        if (!command.startsWith(" ") && command.contains(" ")) {
            return true;
        }
        return false;
    }

    boolean allInvalid = false;

    /**
     * Some cards require the selection of a specific player as target.
     * Make sure the targeted player 1. exists and 2. is not protected by the handmaid.
     * @param target as String
     * @return
     */
    public boolean isValidTarget(String target) {
        for (ClientHandler client : Server.clients) {
            if ((client.getPlayer().getName().equals(target)) && client.getPlayer().isProtected()) {
                sendMessage("The player is protected by the handmaid!");
                return false;
            }
            if (client.getPlayer().getName().equals(target)) {
                return true;
            } else {
                allInvalid = true;
            }
        }
        if (allInvalid) {
            sendMessage("Unknown player name.");
            return false;
        }
        return false;
    }

    /**
     * Validity check for the cards selection when playing the Guard.
     * @param targetCard
     * @return boolean
     */
    public boolean isValidCard(String targetCard) {
        if (Server.allCards.contains(targetCard)) {
            return true;
        } else {
            sendMessage("Please choose an existing card from the game!");
            return false;
        }
    }

    LocalDate date;
    long differenceInDays;

    public boolean isValidDate(String dateString) {
        LocalDate today = LocalDate.now();
        try {
            date = LocalDate.parse(dateString);
        } catch (DateTimeParseException e) {
            sendMessage("Please use the format YYYY-MM-DD!");
            return false;
        }
        differenceInDays = ChronoUnit.DAYS.between(date, today);
        if (differenceInDays > 37000 || differenceInDays < 0) {
            sendMessage("Are you a time traveller? Please choose a more realistic date.");
            return false;
        }
        return true;
    }

    /**
     * Several conditions must be met before a game can be started.
     * @return boolean
     */
    public boolean legalGameStart() {
        if (Server.gameRunning) {
            sendMessage("The game is already running.");
            return false;
        }
        if ((Server.clients.size() < 2)) {
            sendMessage("There are not enough player to start the game!");
            return false;
        }
        if (Server.clients.size() != Server.playerNames.size()) {
            sendMessage("Please wait until everyone has picked a name!");
            return false;
        }
        for (ClientHandler client : Server.clients) {
            if (client.getPlayer().getDaysFromLastDate() == null) {
                sendMessage("Please wait until everyone is ready for the chat.");
                return false;
            }
        }
        return true;
    }

    String command;
    String whisperName;

    /**
     * Common chat logic used for the client.
     * Runs through all checks, then either sends message in the according mode, or executes command
     *
     * @param message
     */
    private void chatLogic(String message) {
        if (message.isEmpty()) {
            sendMessage("You can't send an empty message!");
        }
        //check for commands
        else if (message.startsWith("/")) {
            command = message.replace("/", "");

            if (isWhisperCommand(command)) {
                whisperChat(message);
            } else {
                commandLogic(command);
            }
        }

        // if the message is neither empty nor a special command nor equals "bye", the message gets send to all players the usual way
        else {
            Server.sendToAllClientsExceptSender(name + ": " + message, this);
        }
    }

    /**
     * Chat logic for using the whisper chat.
     * Has to check for the target player and make sure that player actually exists.
     *
     * @param message
     */
    public void whisperChat(String message) {
        // substring to get the name of the target player: starts at 1 to exclude the '/' and stops at the first space
        whisperName = message.substring(1, message.indexOf(" "));
        // check if message was a whisper command for a specific player
        if (Server.playerNames.contains(whisperName)) {
            for (ClientHandler client : Server.clients) {
                if (message.startsWith("/" + client.getPlayer().getName() + " ")) {
                    Server.whisperToPlayer(this, message.replace("/" + client.getPlayer().getName() + " ", ""), client.getPlayer().getName());
                }
            }
        } else {
            sendMessage("There is no player with that name.");
        }
    }

    /**
     *  Checks for the kind of command in combination with different game states and displays messages accordingly.
     * @param command: message without the '/'
     */
    public void commandLogic(String command) {
        //try to play a card while it is not the players turn
        if (Server.gameRunning) {
            if (isCardCommand(command) &&
                    !player.isActive()) {
                sendMessage("It's not your turn!");
                //try to play a card, that is not in the players hand
            } else if (isCardCommand(command) &&
                    !player.playerHandContains(command)) {
                sendMessage("That card is not in your hand! Please choose another one. Your cards are:");
                sendMessage(player.getPlayerHand().get(0).getName());
                sendMessage(player.getPlayerHand().get(1).getName());
            } else {
                getCommandMethod(command);
            }
        } else {
            if (isGameCommand(command) || isCardCommand(command)) {
                sendMessage("You can't use a game command right now because the game is not running at the moment!");

                // if the command is neither game-command nor card-command, run it through the regular command logic
            } else {
                getCommandMethod(command);
            }
        }
    }

    private String dateString;
    private boolean isValidDate;

    /**
     * Asks the player for his/her last date. Checks for the right input format,
     * then calculates the difference to the current date and sets the daysFromLastDate variable in Player to the result.
     * When the game starts this can be used to determine the starting player.
     */
    public void getDate() {
        LocalDate today = LocalDate.now();
        sendMessage("To determine the starting player for the game 'Love Letter' please tell us the date of your last romantic meeting. Please use the format YYYY-MM-DD!");
        try {
            dateString = input.readLine();
            isValidDate = isValidDate(dateString);

            while (!isValidDate) {
                dateString = input.readLine();
                isValidDate = isValidDate(dateString);
            }

            date = LocalDate.parse(dateString);
            differenceInDays = ChronoUnit.DAYS.between(date, today);
            this.player.setDaysFromLastDate(differenceInDays);
        } catch (IOException e) {
            System.err.println("Client disconnected while choosing a date.");
        }
    }

    /**
     * For console command: prints the name of the player whose turn it is.
     */
    public void getActivePlayer() {
        for (ClientHandler client : Server.clients) {
            if (client.getPlayer().isActive()) {
                sendMessage("It's " + client.getPlayer().getName() + "'s turn.");
            }
        }
    }

    /**
     * For console command: prints the names of all players (all clients in Server.clients) on the console.
     */
    public void getPlayerNames() {
        for (ClientHandler client : Server.clients) {
            sendMessage(client.getPlayer().getName());
        }
    }

    /**
     * For console command: prints all player commands on the console.
     */
    public void help() {
        sendMessage("Here are all the commands you can use:");
        sendMessage("/help: show all commands including their description.");
        sendMessage("/cards: show all cards with their respective values and effects.");
        sendMessage("/players: show the name of all players in the chat/game.");
        sendMessage("/start OR /play: start the game 'Love Letter'.");
        sendMessage("The following commands can only be used while the game is running:");
        sendMessage("/endGame: stop the game 'Love Letter' while playing. But you eventually have to explain yourself to your friends :)");
        sendMessage("/points: show the number of your points.");
        sendMessage("/hand OR /showHand: show the card(s) in your hand.");
        sendMessage("/allCards: show all cards, that have been played util now. Use this information wisely ;)");
        sendMessage("/active: show the active player.");
    }

    /**
     * For console commands: prints information about every cards on the console.
     */
    public void cards() {
        sendMessage("Here are all the cards with their respective values and effects:");
        sendMessage("8 - Princess: If you discard the Princess for any reason, you are knocked out of the round. Playing the Princess counts as discarding.");
        sendMessage("7 - Countess: If you have either King or Prince in your hand additionally to the Countess, you have to play the Countess.");
        sendMessage("6 - King: Exchange your card with the card of another target player.");
        sendMessage("5 - Prince: Choose a player. That player has to discard his card and draw a new one.");
        sendMessage("4 - Handmaid: Until your next turn you are protected. Other players cannot target you.");
        sendMessage("3 - Baron: Compare your card with the card of another target player. The player with the lower value card is knocked out of the round.");
        sendMessage("2 - Priest: Look at the card of another target player.");
        sendMessage("1 - Guard: Guess the card of another target player. If you guessed right, that player is knocked out of the round.");
        sendMessage("Essential for all targeting cards: if every other player is protected, you have to choose yourself.");
    }


    //here are all the card effects
    public void playPrincess() {
        this.player.setInRound(false);
        sendMessage("You're out of the round :(");
        Server.sendToAllClientsExceptSender(this.player.getName() + " played the Princess. Oops!", this);
        this.player.setInactive();
        this.player.removeCardFromHand("Princess");
        Server.allPlayedCards.add(new Card("Princess", 8, 1));
    }

    public void playCountess() {
        Server.sendToAllClientsExceptSender(this.player.getName() + " played the Countess. What could this mean?", this);
        this.player.setInactive();
        this.player.removeCardFromHand("Countess");
        Server.allPlayedCards.add(new Card("Countess", 7, 1));
    }

    String target;
    Player targetPlayer;
    boolean isValidTarget;

    public void playKing() {
        try {
            sendMessage("Choose a player to trade your card with. If every other player is protected by the Handmaid, there is no effect.");
            target = input.readLine();
            isValidTarget = isValidTarget(target);

            while (!isValidTarget) {
                target = input.readLine();
                isValidTarget = isValidTarget(target);
            }
            targetPlayer = getPlayerByName(target);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //remove the King from the hand before the effect, otherwise the King could be traded before it gets discarded
        this.player.removeCardFromHand("King");
        // if the player has to choose him/her-self, the card effect gets skipped
        if (targetPlayer != this.getPlayer()) {
            Server.sendToAllClientsExceptSender(this.player.getName() + " played the King and trades cards with " + targetPlayer.getName() + ".", this);

            Card[] cardBuffer = new Card[2];

            cardBuffer[0] = this.player.getCard();
            cardBuffer[1] = targetPlayer.getCard();

            this.player.getPlayerHand().set(0, cardBuffer[1]);
            sendMessage("You got the " + cardBuffer[1].getName() + " from " + targetPlayer.getName());
            targetPlayer.getPlayerHand().set(0, cardBuffer[0]);
            for (ClientHandler client : Server.clients) {
                if (client.getPlayer() == targetPlayer) {
                    client.sendMessage("You and " + this.player.getName() + " exchanged cards. You got the " + cardBuffer[0].getName() + " from " + this.player.getName() + ".");
                }
            }
        }

        this.player.setInactive();
        Server.allPlayedCards.add(new Card("King", 6, 1));
    }

    public void playPrince() {
        try {
            sendMessage("Choose a player who has to discard his card and draw a new one. If there is no legal target, you have to choose yourself.");
            target = input.readLine();
            isValidTarget = isValidTarget(target);

            while (!isValidTarget) {
                target = input.readLine();
                isValidTarget = isValidTarget(target);
            }
            targetPlayer = getPlayerByName(target);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (targetPlayer != this.getPlayer()) {
            Server.sendToAllClientsExceptSender(this.player.getName() + " played the Prince targeting " + targetPlayer.getName() + ". Say goodbye to your card.", this);
            targetPlayer.discardCard();
            targetPlayer.drawCard(Server.deck);

        } else {
            sendMessage("You choose yourself, so the card has no effect.");
            Server.sendToAllClientsExceptSender(this.player.getName() + " played the Prince, but there was no target.", this);
        }
        this.player.setInactive();
        this.player.removeCardFromHand("Prince");
        Server.allPlayedCards.add(new Card("Prince", 5, 2));
    }

    public void playHandmaid() {
        this.player.protect();
        Server.sendToAllClientsExceptSender(this.player.getName() + " played the Handmaid. Keep your hands away!", this);
        sendMessage("You are protected until your next turn.");
        this.player.setInactive();
        this.player.removeCardFromHand("Handmaid");
        Server.allPlayedCards.add(new Card("Handmaid", 4, 2));
    }

    public void playBaron() {
        try {
            sendMessage("Choose a player to compare your hand with.");
            target = input.readLine();
            isValidTarget = isValidTarget(target);

            while (!isValidTarget) {
                target = input.readLine();
                isValidTarget = isValidTarget(target);
            }
            targetPlayer = getPlayerByName(target);

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.player.getPlayerHand().get(0).getValue() < targetPlayer.getPlayerHand().get(0).getValue()) {
            this.player.setInRound(false);
            sendMessage(targetPlayer.getName() + " has the higher value card. You're out of the round :(");
            Server.sendToAllClientsExceptSender(this.player.getName() + " played the Baron, but choose the wrong target and is kicked out of the round.", this);
        }
        if (this.player.getPlayerHand().get(0).getValue() > targetPlayer.getPlayerHand().get(0).getValue()) {
            targetPlayer.setInRound(false);
            targetPlayer.sendMessage(this.player.getName() + " has the higher value card. You're out of the round :(");
        }

        this.player.setInactive();
        this.player.removeCardFromHand("Baron");
        Server.allPlayedCards.add(new Card("Baron", 3, 2));
    }

    public void playPriest() {
        try {
            sendMessage("Choose a player to spy on. If there is no legal target, you have to choose yourself.");
            target = input.readLine();
            isValidTarget = isValidTarget(target);

            while (!isValidTarget) {
                target = input.readLine();
                isValidTarget = isValidTarget(target);
            }
            targetPlayer = getPlayerByName(target);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Server.sendToAllClientsExceptSender(this.player.getName() + " played the Priest. Somebody's nosy.", this);

        this.player.showCardToPlayer(targetPlayer.getName());

        this.player.setInactive();
        this.player.removeCardFromHand("Priest");
        Server.allPlayedCards.add(new Card("Priest", 2, 2));
    }

    String targetCard;
    boolean isValidCard;

    public void playGuard() {
        try {
            sendMessage("Choose a player. If there is no legal target, the card has no effect.");
            target = input.readLine();
            isValidTarget = isValidTarget(target);

            while (!isValidTarget) {
                target = input.readLine();
                isValidTarget = isValidTarget(target);
            }
            targetPlayer = getPlayerByName(target);

            sendMessage("Choose a card other than the Guard.");
            targetCard = input.readLine();
            isValidCard = isValidCard(targetCard);
            if (targetCard.equals("Guard")) {
                isValidCard = false;
                sendMessage("You cannot choose the Guard. Please choose another card.");
            }

            while (!isValidCard) {
                targetCard = input.readLine();
                isValidCard = isValidCard(targetCard);
                if (targetCard.equals("Guard")) {
                    isValidCard = false;
                    sendMessage("You cannot choose the Guard. Please choose another card.");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (targetPlayer != this.getPlayer()) {
            if (targetPlayer.getCard().getName().equals(targetCard)) {
                targetPlayer.setInRound(false);
                sendMessage("You guessed right! Sorry " + targetPlayer.getName() + "!");
                Server.sendToAllClientsExceptSender(this.player.getName() + " played the Guard targeting " + targetPlayer.getName() + " and guessed right. Sorry " + targetPlayer.getName() + "!", this);
            } else {
                Server.sendToAllClientsExceptSender(this.player.getName() + " played the Guard targeting " + targetPlayer.getName() + " and guessed wrong. Lucky you, " + targetPlayer.getName() + "!", this);
                sendMessage("That was wrong.");
            }
        }

        this.player.setInactive();
        this.player.removeCardFromHand("Guard");
        Server.allPlayedCards.add(new Card("Guard", 1, 5));
    }

    /**
     * Detects a String and runs the corresponding method. If the String is none of the commands, an error message will be displayed.
     *
     * @param command String with the name of the command
     */
    public void getCommandMethod(String command) {
        switch (command) {
            case "help":
                this.help();
                break;
            case "cards":
                this.cards();
                break;
            case "players":
                this.getPlayerNames();
                break;
            case "start":
            case "play":
                if (legalGameStart()) {
                    Server.acceptingClients = false;
                    Server.gameRunning = true;
                }
                break;
            case "endGame":
                Server.gameRunning = false;
                Server.acceptingClients = true;
                for (ClientHandler client : Server.clients) {
                    client.getPlayer().setInactive();
                    client.getPlayer().getPlayerHand().clear();
                    client.getPlayer().removeProtection();
                }
                sendMessage("You ended the game before a winner could be decided!");
                Server.sendToAllClientsExceptSender(this.getName() + " stopped the game. No winner was decided.", this);
            case "points":
                sendMessage("You have " + player.getPoints() + " points.");
                break;
            case "hand":
            case "showHand":
                player.showHand();
                break;
            case "allCards":
                sendMessage("Here are all cards that got played in this round:");
                for (Card card : Server.allPlayedCards) {
                    sendMessage(card.getName());
                }
                break;
            case "active":
                if (this.getPlayer().isActive()) {
                    sendMessage("It's your turn!");
                } else {
                    this.getActivePlayer();
                }
                break;
            case "Princess":
                playPrincess();
                break;
            case "Countess":
                playCountess();
                break;
            case "King":
                if (player.mustPlayCountess()) {
                    player.sendMessage("You have the Countess and either King or Prince: the countess must be played!");
                    break;
                }
                playKing();
                break;
            case "Prince":
                if (player.mustPlayCountess()) {
                    player.sendMessage("You have the Countess and either King or Prince: the countess must be played!");
                    break;
                }
                playPrince();
                break;
            case "Handmaid":
                playHandmaid();
                break;
            case "Baron":
                playBaron();
                break;
            case "Priest":
                playPriest();
                break;
            case "Guard":
                playGuard();
                break;
            default:
                sendMessage("NOT A LEGAL COMMAND!");
        }
    }
}



