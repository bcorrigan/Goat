package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.zipcode.*;
import java.util.Random;

public class ZipCodes extends Module {

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
	
	public static String[] getCommands() {
		return new String[]{"zipcode"};
	}
	
	@Override
	public void processChannelMessage(Message m) {
		if(m.getModCommand().equalsIgnoreCase("zipcode"))
			zipcode(m);
		else
			System.out.println("Module zipcodes failed to process command: " + m.getModCommand()); 
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}
	
	private void zipcode(Message m) {
		String ret = "";
		String code = Constants.removeFormattingAndColors(m.getModTrailing()).trim();
		if(code.matches("[0-9]{3}[XxHh]")) {
			ret = "That's a ZIP Code Tabulation Area, not a ZIP code.  But I'm feeling generous.  "
				+ Zcta2000.get(code).toVerboseString();
		} else if (code.matches("[0-9]{5}")) {
			Combined zippy = Combined.get(code);
			if(zippy != null)
				ret = zippy.toVerboseString() ;
			else
				ret = "I don't think " + code + " is a real zip code.";
		} else {
			ret = "don't be a " + randomEpithet() + ", " + m.getSender();
		}
		if(code.equals("10048") || code.equals("77230"))
			ret += "  " + Constants.REVERSE + "NEVER FORGET" + Constants.NORMAL;
 		m.createPagedReply(ret).send();
	}
	
	private Random random = new Random();
	private String randomEpithet() {
		String[] epithets = new String[]{"douche","wanker","prick","cunt","qpt","dick","fag","tard","muppet","jerkoff"};
		return epithets[random.nextInt(epithets.length)];
	}
}
