/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;
import goat.uno.Player;

/**
 * Wild Draw Four: Upstream player draws four cards, player chooses a new colour, upstream player is skipped
 *
 * @author bc
 */
public class WDF implements Card {
    public int getColour() {
        return ANYCOLOUR;
    }

    public void setColour(int colour) throws InvalidCardColourException {

    }

    public int getNumber() {
        return ANYNUMBER;
    }

    public void setNumber(int number) throws InvalidCardNumberException {

    }

    public int getType() {
        return WDF;
    }

    public boolean canPlay(Game game) {
        if (game.currentPlayer.hasUno())
            return false;
        return true;
    }

    public void play(Game game) {
        game.draw.discardCard(this);
        game.output.normalPlay(game.currentPlayer, this);
        game.needColour = true;
        game.output.chooseColour(game.currentPlayer);
        Player player = (Player) game.players.getFirst();
        Card card1 = player.drawCardNoOutput();
        Card card2 = player.drawCardNoOutput();
        Card card3 = player.drawCardNoOutput();
        Card card4 = player.drawCardNoOutput();
        Card[] cards = {card1, card2, card3, card4};

        game.output.drawnCards(player, cards);

        game.players.addLast(game.currentPlayer);
        player = (Player) game.players.removeFirst();
        game.currentPlayer = player;
        game.output.playerSkipped(player);
    }
}
