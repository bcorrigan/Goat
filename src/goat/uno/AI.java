package goat.uno;

import java.util.LinkedList;

/**
 * <p>Description: Represents a pluggable AI for Uno</p>
 * <p/>
 * <p>Copyright: Copyright (c) Barry Corrigan 2005</p>
 *
 * @author Barry Corrigan
 * @version 1.0
 */
public interface AI {
    void play(Player botPlayer, LinkedList players, Deck draw, Game game);
}
