package goat.uno;

import goat.uno.cards.Card;
import goat.uno.cards.Colour;

/**
 * User: bc
 * public interface to uno. Client programmers implement this to handle output from the game.
 */
public interface Output {
	/**
	 * Called when the game is first initialised for any given round. EG, "You have started uno! blah blah blah."
 	 */
	public void gameInit();

	/**
	 * Called when a player joins the game. "SomePlayer is ready to play uno!"
	 * @param player the player who is ready to play
	 */
	public void playerStart(Player player);

	/**
	 * When called, should show a player his hand
	 * @param player
	 */
	public void hand(Player player);

	/**
	 * Called when a player has no such card
	 * @param player
	 */
	public void noSuchCard(Player player);

	/**
	 * Called when the player can't play the card he has tried to
	 * @param player
	 */
	public void playerCantPlay(Player player);

	/**
	 * Called when the player has won the match!
	 * @param player
	 */
	public void playerWon(Player player, int score, Player[] players);

	/**
	 * Called when the draw and the discard decks are swapped
	 */
	public void swapDrawAndDiscard();

	/**
	 * Called when the player has drawn a card.
	 */
	public void playerDrewCard(Player player, Card card);

	/**
	 * Called when the player has passed
	 */
	public void playerPassed(Player player);

	/**
	 * Called when the passes to another player
	 * @param player The player it has passed to
	 */
	public void nextPlayer(Player player);

	/**
	 * Called to show the player the top card to be played on top of.
	 * @param card The card to be played on top of
	 * @param player The player it should be shown to
	 */
	public void showTopDiscardCard(Player player, Card card);

	/**
	 * As above, but shows it to everybody
	 * @param card
	 */
	public void showTopDiscardCardEverybody(Card card);

	/**
	 * Called when a player plays a normal card - "James plays a green 7"
	 * @param player
	 * @param card
	 */
	public void normalPlay(Player player, Card card);

	/**
	 * Called when a player is skipped (james is skipped!)
	 * @param player The player who is skipped
	 */
	public void playerSkipped(Player player);

	/**
	 * Called when a player reverses the game by playing a reverse card
	 */
	public void playerReversed(Player player);

	/**
	 * Called when a player draws some cards. - "You drew: Green 1, WDF"
	 */
	public void drawnCards(Player player, Card[] cards);

	/**
	 * Called when a player plays a wildcard, and must be notified to choose a colour
	 * @param player
	 */
	public void chooseColour(Player player);

	/**
	 * Called when a player has chosen a colour and the players must be shown what it is - "james has chosen Blue"
	 * @param colour
	 */
	public void chosenColour(Colour colour);

	/**
	 * Called when a player has Uno
	 * @param player the player who has uno
	 */
	public void playerHasUno(Player player);

	/**
	 * Called to show the order of play
	 */
	public void order(Player[] allPlayers, int[] noCards);
}
