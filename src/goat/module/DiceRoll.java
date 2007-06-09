package goat.module;
/*
 * Dice Module: For all your 5d6 and 1d100 fun.
 */
import java.text.ParseException;

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
		Dice.BOLD = Message.BOLD;
		Dice.UNDERLINE = Message.UNDERLINE;
		Dice.NORMAL = Message.NORMAL;
	}
	
	public void processPrivateMessage(Message m) {
		if( m.modCommand.equals("roll")) {
			String roll = m.modTrailing;
			try {
				if( Dice.estimateSize(roll)>100) {
					m.createPagedReply("I'm not rolling that many dice, I'd be here all day!").send();
					return;
				}
				Dice dice = new Dice(roll);
				dice.throwDice();
				m.createPagedReply(m.sender + ": " + dice).send();
			} catch(ParseException pe) {
				pe.printStackTrace();
				if( pe.getMessage().equals("Throw size too big"))
					m.createPagedReply("It is so funny to make me try and throw more dice than exist in the universe.").send();
				else if ( pe.getMessage().equals("Dice size too big"))
					m.createPagedReply("I'm not rolling a sphere, sorry.").send();
				else if( pe.getMessage().equals("Error parsing roll"))
					m.createPagedReply("Sorry, I don't know how to do that.").send();
				else
					m.createPagedReply("An unidentified error occurred with roll: " + pe.getMessage()).send();
			}
		} else if( m.modCommand.equals("toss") && ( m.modTrailing.equals("") || m.modTrailing.contains("coin")  )) {
			Die die = new Die(2);
			die.throwDie();
			if( die.getResult()==2 )
				m.createPagedReply("Heads.").send();
			else
				m.createPagedReply("Tails.").send();
		}
	}
	
    public void processChannelMessage(Message m) {
            processPrivateMessage(m) ;
    }

    public int messageType() {
            return WANT_COMMAND_MESSAGES;
    }

    public static String[] getCommands() {
            return new String[]{"roll", "toss"};
    }
}
