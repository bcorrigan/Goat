package goat.dice;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Collection;

/**
 * You can add dice to this class, then call methods to throw them, 
 * retrieve them, and so on and so forth. 
 * @author Barry Corrigan
 */
public class Dice  {
	//Populate these to mark up output
	public static String BOLD = "";
	public static String UNDERLINE = "";
	public static String NORMAL="";
	
	private HashMap<Integer,DiceGroup> dice = new HashMap<Integer,DiceGroup>();
	
	public void addDie(Die die) {
		//if group already exists, add to it, else make new group and add to it
		DiceGroup diceGroup = dice.get(die.getSize());
		if(diceGroup!=null)
			diceGroup.addDie(die);
		else {
			diceGroup = new DiceGroup(die.getSize());
			diceGroup.addDie(die);
			dice.put( die.getSize(), diceGroup );
		}
	}
	
	public void addDice(Die[] dice) {
		for( Die die:dice ) {
			addDie(die);
		}
	}
	
	public void addDice( Collection<Die> diceCollection ) {
		addDice(diceCollection.toArray(new Die[0]));
	}
	
	/**
	 * Rolls all the dice! ie populates the result parameter of every Die object.
	 */
	public void throwDice() {
		for( DiceGroup diceGroup:dice.values() ) {
			diceGroup.throwDice();   //.setResult( (int) Math.random()*die.getSize() + 1);
		}
	}
	
	//Nice and easy constructor
	public Dice( String roll ) throws ParseException {
		parseRoll( roll );
	}
	
	public int getTotalScore() {
		int totalScore=0;
		for( DiceGroup diceGroup : dice.values() )
			totalScore+=diceGroup.getTotal();
		return totalScore;
	}
	
	/**
	 * Parses strings like "2d20 + 7d6" into the resulting Die objects and adds them to 
	 * ths dice object
	 * @param roll The roll to be parsed.
	 * @throws ParseException when roll is not in correct format.
	 */
	public void parseRoll( String roll ) throws ParseException {
		String terms[];
        if( roll.contains("+"))
            terms = roll.split("\\+");
        else
            terms = new String[]{roll};
        //for each term, check it matches regex and if so
        //push into roll values array. 
        for( int i=0; i<terms.length; i++ ) {
            if( terms[i].matches("\\s*?\\d*?d\\d+\\s*?")) { //match whitespace digit-'d'-digit
                //zap any whitespace
                terms[i] = terms[i].trim();
                int throwSize;
                if( terms[i].split("d")[0].equals("") )
                	throwSize=1;		//if no size is specified, use 1. eg a term like "d6"
                else
                	try {
                		throwSize = Integer.parseInt( terms[i].split("d")[0] );
                	} catch( NumberFormatException nfe ) {
                    	throw new ParseException( "Throw size too big", -1 );
                    }
                //now get diceSize
                int diceSize;
                try {
                	diceSize = Integer.parseInt( terms[i].split("d")[1]);
                } catch( NumberFormatException nfe ) {
                	throw new ParseException( "Dice size too big", -1 );
                }
                //create a Die and add it
                for( int j=0; j<throwSize; j++ )
                	addDie( new Die(diceSize) );
            } else {
                throw new ParseException("Error parsing roll", -1);                
            }
        }
	}
	
	/**
	 * Estimates the size of the roll before performing it. Useful for preventing abusive 
	 * "qpt" style users trying to roll a trillion dice or similar.
	 * @param roll The roll string to estimate, as used in parseRoll
	 * @return The number of dice that would have to be instantiated and thrown
	 */
	static public int estimateSize( String roll ) throws ParseException {
		//TODO this is almost same as parseRoll method, if only java had closures.. Could make it use anonymous class tho. Investigate.
		String terms[];
        if( roll.contains("+"))
            terms = roll.split("\\+");
        else
            terms = new String[]{roll};
        int throwSizeTotal=0;
        for( int i=0; i<terms.length; i++ ) {
            if( terms[i].matches("\\s*?\\d*?d\\d+\\s*?")) { //match whitespace digit-'d'-digit
                terms[i] = terms[i].trim();
                if( terms[i].split("d")[0].equals("") )
                	throwSizeTotal+=1;		//if no size is specified, use 1. eg a term like "d6"
                else
                	try {
                		throwSizeTotal += Integer.parseInt( terms[i].split("d")[0] );
                	} catch( NumberFormatException nfe ) {
                    	throw new ParseException( "Throw size too big", -1 );
                    }
            } else {
                throw new ParseException("Error parsing roll", -1);                
            } 
        }
        return throwSizeTotal;
	}
	
	/**
	 * Returns a formatted result string.
	 * @return
	 */
	public String toString() {
		//sort dice
		//now assemble a string, use StringBuilder for speed (it really can get slow)
		StringBuilder result = new StringBuilder();
		for( DiceGroup diceGroup:dice.values() ) {
			result.append( diceGroup + " ");
		}
		result.append(BOLD + " Total:" + NORMAL + getTotalScore());
		System.out.println("RESULT:" + result);
		return result.toString();
	}	
}
