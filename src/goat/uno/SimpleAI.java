package goat.uno;

import goat.uno.cards.Card;
import goat.uno.cards.Colour;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>SimpleAI is, uh, a simple AI that simply plays random cards</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Barry Corrigan
 * @version 1.0
 */
public class SimpleAI implements AI {
    /**
         * play
         *
         * @param players LinkedList
         * @param draw    Deck
         *                todo Implement this goat.uno.AI method
         */
    public void play(Player botPlayer, LinkedList<Player> players, Deck draw, Game game) {
        play(botPlayer, game);

        if (game.needColour) {
            int colour = (int) (Math.random() * 4 + 1);
            game.setColour(botPlayer, colour);
            return;
        }

        //no cards are playable, need to draw
        game.draw(botPlayer);

        if (play(botPlayer, game)) {
            if (game.needColour) {
                int colour = (int) (Math.random() * 4 + 1);
                game.setColour(botPlayer, colour);
            }
            return;
        }

        if (game.hasDrawn) {
            game.pass(botPlayer);
            return;
        }

    }

    private boolean play(Player botPlayer, Game game) {
        ArrayList hand = botPlayer.getHand();
        Iterator it = hand.iterator();
        while (it.hasNext()) {
            Card card = (Card) it.next();
            if (card.canPlay(game)) {
                game.play(botPlayer, card);
                return true;
            }
        }
        return false;
    }


}
