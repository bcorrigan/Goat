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
    public InvalidCardColourException(String e) {
        super(e);
    }
}
