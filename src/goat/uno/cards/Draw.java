/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;
import goat.uno.Player;

/**
 * Draw card. Can be any colour. When played, next player draws two cards and is skipped.
 *
 * @author bc
 */
public class Draw implements Card {

	private int colour;

	public Draw(int colour) throws InvalidCardColourException {
		if(colour>=1&&colour<=4)
			this.colour=colour;
		else
			throw new InvalidCardColourException("The colour specified must be a value 0-4, or, Card.ANYCOLOUR, Card.RED, Card.GREEN, Card.BLUE, or Card.YELLOW");
	}

	public int getColour() {
		return colour;
	}

	public int getNumber() {
		return DRAW;
	}

	public int getType() {
		return DRAW;
	}

	public boolean canPlay(Game game) {
		Card card = game.draw.peekDiscardCard();
		if(this.colour==card.getColour())
			return true;
		if(DRAW==card.getNumber())
			return true;
		return false;
	}

	public void play(Game game) {
    	game.draw.discardCard(this);
		game.output.normalPlay(game.currentPlayer, this);
		Player player = (Player) game.players.peek();

		Card card1=player.drawCardNoOutput();
		Card card2=player.drawCardNoOutput();
		Card[] cards = {card1, card2};
		game.output.drawnCards(player, cards);
		game.players.addLast(game.currentPlayer);
		player = (Player) game.players.removeFirst();
		game.currentPlayer = player;
		game.output.playerSkipped(player);
	}
}
