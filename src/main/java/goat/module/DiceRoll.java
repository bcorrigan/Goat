package goat.module;
/*
 * Dice Module: For all your 5d6 and 1d100 fun.
 */
import java.text.ParseException;

import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;

import goat.dice.*;

/**
 * Dice throwing module.
 * @author bc
 */
public class DiceRoll extends Module {
	
	//kind of crap but at least goat.dice doesn't know about the rest of goat this way
	static {
		Dice.BOLD = Constants.BOLD;
		Dice.UNDERLINE = Constants.UNDERLINE;
		Dice.NORMAL = Constants.NORMAL;
	}
	
	public void processPrivateMessage(Message m) {
		if( m.getModCommand().equals("roll")) {
			String roll = m.getModTrailing();
			try {
				if( Dice.estimateSize(roll)>100) {
					m.pagedReply("I'm not rolling that many dice, I'd be here all day!");
					return;
				}
				Dice dice = new Dice(roll);
				dice.throwDice();
				m.pagedReply(m.getSender() + ": " + dice);
			} catch(ParseException pe) {
				pe.printStackTrace();
				if( pe.getMessage().equals("Throw size too big"))
					m.pagedReply("It is so funny to make me try and throw more dice than exist in the universe.");
				else if ( pe.getMessage().equals("Dice size too big"))
					m.pagedReply("I'm not rolling a sphere, sorry.");
				else if( pe.getMessage().equals("Error parsing roll"))
					m.pagedReply("Sorry, I don't know how to do that.");
				else
					m.pagedReply("An unidentified error occurred with roll: " + pe.getMessage());
			}
		} else if( m.getModCommand().equals("toss") && ( m.getModTrailing().equals("") || m.getModTrailing().contains("coin")  )) {
			Die die = new Die(2);
			die.throwDie();
			if( die.getResult()==2 )
				m.pagedReply("Heads.");
			else
				m.pagedReply("Tails.");
		}
	}
	
    public void processChannelMessage(Message m) {
            processPrivateMessage(m) ;
    }

    public int messageType() {
            return WANT_COMMAND_MESSAGES;
    }

    public String[] getCommands() {
            return new String[]{"roll", "toss"};
    }
}
