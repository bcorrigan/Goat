/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno;

import goat.uno.cards.Card;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * A player
 *
 * @author bc
 */
public class Player {

	private ArrayList hand = new ArrayList(20);
	private int score;
	private String name;
	private boolean hasWon;
	private Game game;

	public Player(Card[] cards, String name, Game game) {
		if(cards.length!=7) {
			System.out.println("hand init fuckup");
			System.exit(0);
		}
		else {
			for(int i=0;i<cards.length; i++) {
				hand.add(cards[i]);
			}
		}
		this.name = name;
		this.game = game;
	}

	public ArrayList getHand() {
		return hand;
	}

	public void setHand(ArrayList hand) {
		this.hand = hand;
	}

	public int getScore() {
		score = 0;
		Iterator it = hand.iterator();
		while(it.hasNext()) {
			Card card = (Card) it.next();
			switch(card.getType()) {
				case Card.NORMALCARD:
					score += card.getNumber();
					continue;
				case Card.REVERSE:
					score += 20;
					continue;
				case Card.SKIP:
					score += 20;
					continue;
				case Card.DRAW:
					score += 20;
					continue;
				case Card.WILD:
					score += 50;
					continue;
				case Card.WDF:
					score += 50;
					continue;
			}
		}
		return score;
	}

	public void addScore(int score) {
		this.score += score;
	}

	public String getName() {
		return name;
	}

	Card[] getValidCards(Game game) {
		Iterator it = hand.iterator();
		ArrayList validCards = new ArrayList(10);
		while(it.hasNext()) {
			Card card = (Card) it.next();
			if(card.canPlay(game))
				validCards.add(card);
		}
		validCards.trimToSize();
		return (Card[]) validCards.toArray();
	}

	Card[] getCards() {
		return (Card[]) hand.toArray();
	}

	boolean hasCard(Card card) {
		return hand.contains(card);
	}

	boolean hasWon() {
		return hasWon;
	}

	public boolean hasUno() {
		if(hand.size()==1)
			return true;
		return false;
	}

	void setWon() {
		hasWon = true;
	}

	public Card drawCard() {
		Card card = game.draw.drawCard();
		hand.add(card);
		game.output.playerDrewCard(this, card);
		//game.output.drawnCards(this, );
		return card;	//return it so we can also notify player!
	}

	public Card drawCardNoOutput() {
		Card card = game.draw.drawCard();
		hand.add(card);
		//game.output.playerDrewCard(this, card);
		//game.output.drawnCards(this, );
		return card;	//return it so we can also notify player!
	}

	public void removeCard(Card card) {
		hand.remove(card);
		if(hand.size()==0)
			setWon();
		//game.draw.discardCard(card);
	}
}
