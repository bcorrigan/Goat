/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;
import goat.uno.Player;

import java.util.Collections;

/**
 * A coloured card. When played, the order of play reverses
 *
 * @author bc
 */
public class Reverse implements Card {

	private int colour;

	public Reverse(int colour) throws InvalidCardColourException {
		if(colour>=1&&colour<=4)
			this.colour=colour;
		else
			throw new InvalidCardColourException("The colour specified must be a value 0-4, or, Card.ANYCOLOUR, Card.RED, Card.GREEN, Card.BLUE, or Card.YELLOW");
	}

	public int getColour() {
		return colour;
	}

	public int getNumber() {
		return REVERSE;
	}

	public int getType() {
		return REVERSE;
	}

	public boolean canPlay(Game game) {
		Card card = game.draw.peekDiscardCard();
		if(colour==card.getColour())
			return true;
		if(REVERSE==card.getNumber())
			return true;
		return false;
	}

	public void play(Game game) {
		game.draw.discardCard(this);
		game.output.normalPlay(game.currentPlayer, this);
		Collections.reverse(game.players);
		game.output.playerReversed(game.currentPlayer);
		if(game.players.size()==1) { //if there are two players (there is always one player not in players)
			game.players.addLast(game.currentPlayer);
			Player player = (Player) game.players.removeFirst();
			game.currentPlayer = player;
			game.output.playerSkipped(player);
		}
	}
}
