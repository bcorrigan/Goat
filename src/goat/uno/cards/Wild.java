/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno.cards;

import goat.uno.Game;
import goat.uno.Player;

/**
 * Can be played on any card. Player can set a new colour when it is played.
 *
 * @author bc
 */
public class Wild implements Card {
	public int getColour() {
		return ANYCOLOUR;
	}

	public int getNumber() {
		return ANYNUMBER;
	}

	public int getType() {
		return WILD;
	}

	public boolean canPlay(Game game) {
		if(game.currentPlayer.hasUno())
			return false;
		return true;
	}

	public void play(Game game) {
    	game.draw.discardCard(this);
		game.output.normalPlay(game.currentPlayer, this);
		game.needColour=true;
		//game.draw.setFakePeek();
		game.output.chooseColour(game.currentPlayer);
		//game.players.addLast(game.currentPlayer);
		//Player player = (Player) game.players.removeFirst();
		//game.currentPlayer = player;
		//game.output.playerSkipped(player);
	}
}
