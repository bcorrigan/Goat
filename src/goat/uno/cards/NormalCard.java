/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;

/**
 * A normal Card, of the type RGBY 0-9
 *
 * @author bc
 */
public class NormalCard implements Card {

	public NormalCard(int colour, int number) throws InvalidCardColourException, InvalidCardNumberException {
		if(colour>=1&&colour<=4)
			this.colour=colour;
		else
			throw new InvalidCardColourException("The colour specified must be a value 0-4, or, Card.ANYCOLOUR, Card.RED, Card.GREEN, Card.BLUE, or Card.YELLOW");
		if(number>=0&&number<=9)
			this.number=number;
		else
			throw new InvalidCardNumberException("Invalid number: must be from 0-9");
	}

	/**
	 * the colour of this card - RGBY or ANYCOLOUR
	 */
	private int colour;

	/**
	 * The number of this card -  0-9 or <code>ANYNUMBER</code>
	 */
	private int number;

	/**
	 * Get the colour of this card.
	 * @return <code>RED</code>, <code>GREEN</code>, <code>BLUE</code>, <code>YELLOW</code>, or <code>ANYCOLOUR</code>.
	 */
	public int getColour() {
		return colour;
	}

	/**
	 * Get the number of this card
	 * @return
	 */
	public int getNumber() {
		return number;
	}

	public int getType() {
		return NORMALCARD;
	}

	public boolean canPlay(Game game) {
		Card card = game.draw.peekDiscardCard();
		if(colour==card.getColour())
			return true;
		if(number==card.getNumber())
			return true;
		return false;
	}

	public void play(Game game) {
		game.draw.discardCard(this);	//that's it!
		game.output.normalPlay(game.currentPlayer, this);
	}
}
