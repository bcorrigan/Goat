package goat.dice;

import java.util.ArrayList;

/**
 * A group of dice of same size. This is useful cos we have various operations we want to perform on same-sized dice.
 * 
 * @author E182518
 */
public class DiceGroup {
	private ArrayList<Die> dice = new ArrayList<Die>();
	
	private int size;
	
	public DiceGroup( int size ) {
		this.size=size;
	}
	
	int getSize() {
		return size;
	}
	
	void setDice(ArrayList<Die> dice) {
		this.dice = dice;
	}
	
	ArrayList<Die> getDice() {
		return dice;
	}
	
	//TODO handle error differently - exception really appropriate?
	void addDie(Die die) {
		if( die.getSize()==size )
			dice.add( die );
		else throw new IllegalArgumentException("Tried to add die of size " + die.getSize() + " to group of size " + size);
	}
	
	public int getTotal() {
		int total=0;
		for( Die die:dice )
			total+=die.getResult();
		return total;
	}
	
	public String toString() {
		StringBuilder term = new StringBuilder( dice.size() + "d" + size + ":" );
		for( int i=0; i<dice.size(); i++ ) {
			term.append(dice.get(i).getResult());
			if(i<(dice.size()-1))
				term.append(",");
		}
		term.append(":" + getTotal());
		return term.toString();
	}
	
	public void throwDice() {
		for( Die die:dice )
			die.throwDie();
	}
}
