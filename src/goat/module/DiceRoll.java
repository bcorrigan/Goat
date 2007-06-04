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
	
	public void processPrivateMessage(Message m) {
		if( m.modCommand.equals("roll")) {
			String roll = m.modTrailing;
			try {
				if( Dice.estimateSize(roll)>100) {
					m.createPagedReply("Don't be a qpt, please.").send();
					return;
				}
				Dice dice = new Dice(roll);
				dice.throwDice();
				m.createPagedReply(dice.toString()).send();
			} catch(ParseException pe) {
				pe.printStackTrace();
				m.createPagedReply("That doesn't seem right somehow. Sorry.").send();
			} catch( NumberFormatException nfe ) {
				nfe.printStackTrace();
				m.createPagedReply("I'm not rolling a sphere, thanks.").send();
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
