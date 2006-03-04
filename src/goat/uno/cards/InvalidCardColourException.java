/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

/**
 * Thrown when an invalid colour is specified.
 *
 * @author bc
 */
public class InvalidCardColourException extends Exception {

	/**
	 * Eclipse moans if we don't have this.
	 */
	private static final long serialVersionUID = 1L;

	public InvalidCardColourException(String e) {
        super(e);
    }
}
