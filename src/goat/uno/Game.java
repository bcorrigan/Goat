/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno;

import goat.uno.cards.Card;
import goat.uno.cards.Colour;

import java.util.*;

/**
 * @author bc
 */
public class Game {
	public LinkedList players = new LinkedList();
	public Player currentPlayer;
	public boolean needColour;	//true when the game is looking for a colour
	public Colour colourCard;	//when above is true, this is the colour needed
	public Output output;
	public Deck draw; //= new Deck();
	boolean hasDrawn = false;
	Player wildPlayer;
	private Scores scores = new Scores();


	/**
	 * Initialises this game.
	 *
	 * @param uno The output class. This game will call the methods of this class for its output.
	 */
	public Game(Output uno, String name) {  //@TODO thread this for 30 seconds, while players join, turn over top card add to discard. show it
		this.output = uno;
		draw = new Deck(uno);
		uno.gameInit();
		Card[] cards = new Card[7];
		for (int i = 0; i < 7; i++) {
			cards[i] = draw.drawCard();
		}
		Player player = new Player(cards, name, this);
		//players.add(player);
		currentPlayer = player;
		//players.add(player);
		uno.hand(player);
		uno.playerStart(player);
		uno.showTopDiscardCardEverybody(draw.peekDiscardCard());
	}

	public void play(Player player, Card card) {
		if (player == currentPlayer) {
			if (player.hasCard(card)) {
				if (card.canPlay(this)) {
					card.play(this);
					player.removeCard(card);
					if (player.hasWon()) {
						playerWon(player);     //@TODO scores tallying
						return;
					}
					if (card.getType() == Card.WDF)
						wildPlayer = (Player) players.getLast();     //maybe make WILD skip nextPlayer() here too, but without this bit
					else if ((card.getType() == Card.WILD))
						wildPlayer = currentPlayer;                  //this is why I was having problems with syncing!
					else
						nextPlayer();

					if (player.hasUno())
						output.playerHasUno(player);
				} else {
					output.playerCantPlay(player);
				}
			} else {
				output.noSuchCard(player);
			}
		}
	}

	public void hand(Player player) {
		output.hand(player);
	}

	public void join(String name) {
		Card[] cards = new Card[7];
		for (int i = 0; i < 7; i++) {
			cards[i] = draw.drawCard();
		}
		Player player = new Player(cards, name, this);
		players.add(player);
		output.hand(player);
		output.playerStart(player);
	}

	public void draw(Player player) {
		if(!hasDrawn)
			if (player == currentPlayer) {
				player.drawCard();
				hasDrawn = true;
			}
	}

	public void pass(Player player) {
		if (player == currentPlayer) {
			if (hasDrawn) {
				output.playerPassed(player);
				//change current player again
				nextPlayer();
				hasDrawn = false;
			}
		}
	}

	public void setColour(Player player, int colour) {
		if (player == wildPlayer) {
			needColour = false;
			colourCard = new Colour(colour);
			draw.setFakePeek(colourCard);
			output.chosenColour(colourCard);
			nextPlayer();
		}
	}

	public void nextPlayer() {
		players.addLast(currentPlayer);
		currentPlayer = (Player) players.removeFirst();
		output.nextPlayer(currentPlayer);
		output.showTopDiscardCard(currentPlayer, draw.peekDiscardCard());
		hasDrawn = false;
	}

	public void show() {
		output.showTopDiscardCardEverybody(draw.peekDiscardCard());
	}

	private void playerWon(Player player) {
		int score = 0;
		Iterator it = players.iterator();
		Player[] playersArray = new Player[players.size()];
		int i = 0;
		while (it.hasNext()) {
			Player aPlayer = (Player) it.next();
			if(!(player.getName().equals(aPlayer.getName()))) {
				playersArray[i] = aPlayer;
				score += aPlayer.getScore();
				i++;
			}
		}
		if(i!=players.size()) {
			playersArray[i] = currentPlayer;
			score+=currentPlayer.getScore();
		}
		output.playerWon(player, score, playersArray);
		scores.commit(player, score, playersArray);
	}

	public void order() {
		int totalNoPlayers = players.size() + 1;
		Player[] allPlayers = new Player[totalNoPlayers];
		int[] noCards = new int[totalNoPlayers];
		for(int i=0;i<players.size();i++) {
			Player player = (Player) players.get(i);;
			allPlayers[i+1] = player;
			noCards[i+1] = player.getHand().size();
		}
		allPlayers[0] = currentPlayer;
		noCards[0] = currentPlayer.getHand().size();
		output.order(allPlayers, noCards);
	}
}
