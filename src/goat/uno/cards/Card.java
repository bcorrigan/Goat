/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;

/**
 * <p>Represents a card in the game.</p>
 * <ul>
 * <li>There are 108 cards as follows:
 * <li>19 Red cards - 1 to 9 x2 + one 0 card
 * <li>19 Green cards - 1 to 9 x2 + one 0 card
 * <li>19 Blue cards - 1 to 9 x2 + one 0 card
 * <li>19 Yellow cards - 1 to 9 x2 + one 0 card
 * <li>8 Skip cards - 2 each in RGBY
 * <li>8 Reverse cards - 2 each in RGBY
 * <li>8 Draw 2 cards - 2 each in RGBY
 * <li>4 Wild cards
 * <li>4 Wild Draw 4 cards
 * </ul>
 * <p>Implementors handle each of these special cases.</p>
 * @see <code>Skip</code>, <code>Reverse</code>, <code>DrawTwo</code>, <code>Wild</code>, <code>WDF</code>.</p>
 * @author bc
 */
public interface Card {

	/**
	 * This card can be played on any other colour of card.
	 */
	public static int ANYCOLOUR = 0;    //TODO seems ugly, consider removing ANYCOLOUR
	public static int RED = 1;
	public static int GREEN = 2;
	public static int BLUE = 3;
	public static int YELLOW = 4;

	public static int NORMALCARD=0;
	public static int DRAW=10;
	public static int SKIP=11;
	public static int REVERSE=12;
	public static int WILD=13;
	public static int WDF=14;
	public static int FAKE=15;

	/**
	 * This card can be played on any other numbered card.
	 */
	public static int ANYNUMBER=10; //TODO seems ugly, consider removing ANYNUMBER

	/**
	 * Get the colour of this card.
	 * @return <code>RED</code>, <code>GREEN</code>, <code>BLUE</code>, <code>YELLOW</code>, or <code>ANYCOLOUR</code>.
	 */
	public int getColour();

	/**
	 * Set the colour of this card.
	 */
	//public void setColour(int colour) throws InvalidCardColourException;

	/**
	 * Get the number of this card.
	 * @return
	 */
	public int getNumber();

	/**
	 * Set the number of this card.
	 */
	//public void setNumber(int number) throws InvalidCardNumberException;

	/**
	 * Get the type of this card
	 *
	 * @return The type of card this is
	 */
	public int getType();

	/**
	 * Checks if this card can play in this game
	 * @param game The game to be tested against
	 * @return True if it can, false if it can't.
	 */
	boolean canPlay(Game game);

	/**
	 * Plays this card in this game
	 * @param game The game the card should play in
	 */
	void play(Game game);
}
