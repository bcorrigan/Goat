/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;
import goat.uno.Player;

/**
 * A skip card. Can be any colour. When played, it skips the next person in the order of play.
 *
 * @author bc
 */
public class Skip implements Card {

	private int colour;

	public Skip(int colour) throws InvalidCardColourException {
		if(colour>=1&&colour<=4)
			this.colour=colour;
		else
			throw new InvalidCardColourException("The colour specified must be a value 0-4, or, Card.ANYCOLOUR, Card.RED, Card.GREEN, Card.BLUE, or Card.YELLOW");
	}

	public int getColour() {
		return colour;
	}

	public int getNumber() {
		return SKIP;
	}

	public int getType() {
		return SKIP;
	}

	public boolean canPlay(Game game) {
		Card card = game.draw.peekDiscardCard();
		if(colour==card.getColour())
			return true;
		if(SKIP==card.getNumber())
			return true;
		return false;
	}

	public void play(Game game) {
    	game.draw.discardCard(this);
		game.output.normalPlay(game.currentPlayer, this);
		Player player = (Player) game.players.getFirst();
		game.players.addFirst(player) ;
		game.output.playerSkipped(player);
		game.players.addLast(game.currentPlayer);
		game.currentPlayer = (Player) game.players.removeFirst();
	}
}
