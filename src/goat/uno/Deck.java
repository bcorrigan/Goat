/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno;

import goat.uno.cards.*;

import java.util.*;

/**
 * A deck of cards. Usually either the full deck, the discard deck, or the draw deck.
 *
 * @author bc
 */
public class Deck {
	private LinkedList draw = new LinkedList();
	private LinkedList discard = new LinkedList();
	private Output uno;
	Colour colourCard;
	private boolean fake=false;

	public Deck(Output uno) {
		try {
			//add Green 0, Blue 0, Red 0, Yellow 0
			for (int i = 1; i < 5; i++) {
				NormalCard card = new NormalCard(i, 0);
				draw.add(card);
			}
			//add RGBY 1-9 x2
			for (int i = 1; i < 5; i++) {
				for (int j = 1; j < 10; j++)
					for(int k=0; k<2; k++) {
						NormalCard card = new NormalCard(i, j);
						draw.add(card);
					}
			}
			//add 8 Skip cards, RGBY
			for(int i=0;i<2;i++) {
				for(int j=1;j<5;j++) {
					Skip skip = new Skip(j);
					draw.add(skip);
				}
			}
			//add 8 Reverse card, RGBY
			for(int i=0;i<2;i++) {
				for(int j=1;j<5;j++) {
					Reverse reverse = new Reverse(j);
					draw.add(reverse);
				}
			}
			//add 8 draw two cards
            for(int i=0;i<2;i++) {
				for(int j=1;j<5;j++) {
					Draw draw = new Draw(j);
					this.draw.add(draw);
				}
			}
			//add 4 Wild cards & four Wild Draw Four cards
			for(int i=0;i<4;i++) {
				Wild wild = new Wild();
				draw.add(wild);
				WDF wdf = new WDF();
				draw.add(wdf);
			}
			shuffle();
			this.uno = uno;
			while(true) {
				Card card = (Card) draw.removeFirst();
				if(card.getType()==Card.WILD||card.getType()==Card.WDF)
					continue;
				discard.add(card);
				break;
			}
		} catch (InvalidCardColourException e) {
			e.printStackTrace();
		} catch (InvalidCardNumberException e) {
			e.printStackTrace();
		}
	}

	void addCard(Card card) {
		draw.add(card);
	}

	//Card[] drawCards() {
	//	return (Card[]) draw.toArray();
	//}

	Card drawCard() {
		if(draw.size()==0) {       //alter this
			Card topDiscard = (Card) discard.removeFirst();
			LinkedList temp = draw;
			draw = discard;
			discard = temp;
			discard.addFirst(topDiscard);
			uno.swapDrawAndDiscard();
		}
		return (Card) draw.removeFirst();
	}

	public Card peekDiscardCard() {
		if(fake) {
			return colourCard;
		}
		return (Card) discard.peek();
	}

	public void discardCard(Card card) {
		discard.addFirst(card);
		fake=false;
	}

	void shuffle() {
		Collections.shuffle(draw);
	}

	public void setFakePeek(Colour colourCard) {
		this.colourCard = colourCard;
		fake=true;
	}
}
