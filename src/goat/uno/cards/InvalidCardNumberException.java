/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

/**
 * Thrown when an invalid card number is specified.
 *
 * @author bc
 */
public class InvalidCardNumberException extends Exception {
    public InvalidCardNumberException(String e) {
        super(e);
    }
}
