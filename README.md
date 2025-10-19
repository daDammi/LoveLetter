# Vorprojekt Stefan Damböck

This was a pre-project for a university group project.
This assignment was supposed to verify the student's programming skills before moving on to the main group project.

Students were tasked with recreating the card game Love Letter without graphical elements and a rudimentary server-client environment.

The time frame was about two weeks.

## About the game

_Love Letter_ is a card game by Kanai Seiji and was first published in 2012. It can be played by up to four players. It is a deduction card game of risk, bluffing, and luck. Players compete to deliver their love letter to the Princess while deflecting the efforts of others.

In this project the game was recreated to be fully playable in a console, using text only.
Every player uses a different console.
Player actions are takeen via text commands.

Players start in a lobby. Any player can start a game once there are at least two players. Ending a game returns to the lobby, where another game can be started.

## Set up project locally and create jar file

- clone repository to local folder
- run

```
mvn clean package
```

to build the _target_ folder.

## Run the game

To run the game, first start the server and then add up to four clients (players).

- open a new terminal from the just created _target_ folder (or open the terminal from anywhere else and navigate to the _target_ folder)
- run

```
java -jar vp-damboeck-1.0-SNAPSHOT.jar server
```

to start the server

- for every player, open a separate terminal and navigate to the _target_ folder
- run

```
java -jar vp-damboeck-1.0-SNAPSHOT.jar client
```

- Enter a name and a date to enter the lobby
- In the lobby, players can chat, start a game or leave (close the client) by simply writing _bye_.
- Type /start or /play to start a game, once there are at least two players connected.

## Commands

These are all commands the players can use:

- /help: show all commands including their description.
- /cards: show all cards with their respective values and effects.
- /players: show the name of all players in the chat/game.
- /start OR /play: start the game 'Love Letter'.

The following commands can only be used while the game is running:

- /endGame: stop the game while playing.
- /points: show the number of your points.
- /hand OR /showHand: show the card(s) in your hand.
- /allCards: show all cards, that have been played util now
- /active: show the active player.

# How to play

## Cards

The game consist of 8 different card and 16 cards in total. Each card has a value and an effect when played.

These are the cards with their respective values:

- 8 - Princess: If you discard the Princess for any reason, you are knocked out of the round. Playing the Princess counts as discarding.
- 7 - Countess: If you have either King or Prince in your hand additionally to the Countess, you have to play the Countess.
- 6 - King: Exchange your card with the card of another target player.
- 5 - Prince: Choose a player. That player has to discard his card and draw a new one.
- 4 - Handmaid: Until your next turn you are protected. Other players cannot target you.
- 3 - Baron: Compare your card with the card of another target player. The player with the lower value card is knocked out of the round.
- 2 - Priest: Look at the card of another target player.
- 1 - Guard: Guess the card of another target player. If you guessed right, that player is knocked out of the round.

## Gameplay

Starting the game will automatically provide the game setup:

- Shuffle the cards.
- Remove the top card face down from the deck (hidden information). In a 2-player game, remove three cards face up instead.
- Deal one card to each player.
- The remaining cards form the deck.
- Choose a starting player: the player with the last romantic encounter (hence the question when connecting).

Each turn consists of two steps:

- The active player draws a card and has now to cards. All non active players always have one card.
- Choose one card to play, resolving its effect immediately. Discard the card face up. Effects target other players unless otherwise stated.

Then the turn passes clockwise.

A player is out of the round if they:

- Discard the _Princess_, or
- Are eliminated by another player’s effect (e.g., Guard, Baron).

The round ends when:

- Only one player remains, **or**
- The deck runs out of cards.

The remaining players reveal their hands — the **highest card value** wins the round and earns _one token of affection_.
The first player to earn a set number of affection tokens, depending on the number of players, wins.

### Disclaimer

This is an unofficial, non-commercial, text-based implementation inspired by the card game _Love Letter_ by Seiji Kanai.
All names and thematic elements remain the property of their respective owners.
This project was for educational purposes only and is not affiliated with or endorsed by Alderac Entertainment Group (AEG) or Z-Man Games.
