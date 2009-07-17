package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.util.Dict ;
import goat.util.CommandParser ;

import java.util.ArrayList ;
import java.util.Iterator ;
import java.util.Random ;

/**
 * Title:    
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * <p/>
 * @author encontrado
 *
 * @version 1.0
 */

public class RandWords extends Module {

	private Random random = new Random() ;
	private Dict dict = new Dict() ;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"randword", "randwords", "bandname", "headline"};
   }
	
	public RandWords() {
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		int num = 1 ;
		if (m.getModCommand().equalsIgnoreCase("randword") 
				|| m.getModCommand().equals("randwords")) {
			String numString = "";
			CommandParser parser = new CommandParser(m) ;
			if (parser.has("num")) 
				numString = parser.get("num") ;
			else if (parser.remaining().matches("^\\d+$")) 
				numString = parser.remaining() ;
			if (! numString.equals(""))
				try {
					num = Integer.parseInt(numString) ;
				} catch (NumberFormatException e) {
					m.createReply("Don't fuck with me, tough guy.").send() ;
					return ;
				}
			if (num > 1000) {
				m.createReply("Now you're just being a prick.").send() ;
				return ;
			} else if (num > 100) {
				m.createReply("Don't be ridiculous.").send() ;
				return ;
			} else if (num < 1) {
				m.createReply("er...").send() ;
				return ;
			} else if (num > 30) {
				num = 30 ;
			}		
			m.createReply(randWordString(num)).send() ;
		} else if (m.getModCommand().equalsIgnoreCase("bandname")) {
			String arg = m.getModTrailing().trim() ;
			String reply;
			if (arg.equals("") || arg == null ) {
				reply = randWordString(2) ;
			} else {
				if (random.nextBoolean()) {
					reply = arg + ' ' + getWord() ;
				} else {
					reply = getWord() + ' ' + arg ;
				}
			}
			m.createReply(reply).send() ;
		} else if (m.getModCommand().equalsIgnoreCase("headline")) {
			CommandParser parser = new CommandParser(m) ;
			String reply = "";
			ArrayList<String> seeds = parser.remainingAsArrayList() ;
			if (seeds.isEmpty()) {
				reply = randWordString(4) ;
			} else if (seeds.size() > 3) {
				reply = "Too long, kid.  Give me three words or less." ;
			} else {
				while (seeds.size() < 4) {
					seeds.add(getWord()) ;
				}
				while (seeds.size() != 1) {
					reply += ((String) seeds.remove(random.nextInt(seeds.size())) ) + " " ;
				}
				reply += seeds.remove(0) ;
			}
			m.createReply(reply).send() ;
		}
	}

	public String randWordString(int num) {
		// spit out num random words as a single string
		if (0 == num) {
			//complain
			return "um..." ;
		} else if( 1 == num ) {
			// do it the quick way
			return getWord() ;
		} else {
			// do it the hard way
			String returnpoop = "" ;
			ArrayList words = getWords(num) ;
			Iterator it = words.iterator() ;
            for (Object word : words) {
                returnpoop += word + " ";
            }
			return returnpoop.trim() ;
		}
	}
	
	public String getWord() {
		// return one random word as a string
		return dict.getWord(random.nextInt(dict.numWords)) ;
	}

	public ArrayList getWords(int num) {
		// return num random words as an ArrayList
		ArrayList wordList = new ArrayList(num) ;
		for (int i=0; i < num; i++) {
			wordList.add(dict.getWord(random.nextInt(dict.numWords))) ;
		}
		return wordList ;
	}
}
