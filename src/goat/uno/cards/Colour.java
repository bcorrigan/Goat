/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;

/**
 * Not actually a card, just a fake card that is a colour only: "Blue", "Red", etc
 *
 * @author bc
 */
public class Colour implements Card {
    int colour;

    public Colour(int colour) {
        this.colour = colour;
    }

    public int getColour() {
        return colour;
    }

    public int getNumber() {
        return ANYNUMBER;
    }

    public int getType() {
        return FAKE;
    }

    public boolean canPlay(Game game) {
        return false;
    }

    public void play(Game game) {

    }
}
