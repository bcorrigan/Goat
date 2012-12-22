package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.util.Dict;
import goat.util.CommandParser;
import static goat.core.Constants.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

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
       return new String[]{
           "randword",
           "randwords",
           "bandname",
           "headline",
           "emoji",
           "goatji",
           "brofist",
           "slowclap",
       };
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
			if (parser.hasVar("num"))
				numString = parser.get("num") ;
			else if (parser.remaining().matches("^\\d+$"))
				numString = parser.remaining() ;
			if (! numString.equals(""))
				try {
					num = Integer.parseInt(numString) ;
				} catch (NumberFormatException e) {
					m.reply("Don't fuck with me, tough guy.") ;
					return ;
				}
			if (num > 1000) {
				m.reply("Now you're just being a prick.") ;
				return ;
			} else if (num > 100) {
				m.reply("Don't be ridiculous.") ;
				return ;
			} else if (num < 1) {
				m.reply("er...") ;
				return ;
			} else if (num > 30) {
				num = 30 ;
			}
			m.reply(randWordString(num)) ;
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
			m.reply(reply) ;
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
			m.reply(reply) ;
		} else if (m.getModCommand().equalsIgnoreCase("emoji")) {
                    m.reply(randEmojiWithName());
                } else if (m.getModCommand().equalsIgnoreCase("goatji")) {
                    m.reply(new String(Character.toChars(128016)));
                } else if (m.getModCommand().equalsIgnoreCase("brofist")) {
                    m.reply(new String(Character.toChars(128074)));
                } else if (m.getModCommand().equalsIgnoreCase("slowclap")) {
                    try {
                        Thread.sleep(1500);
                        m.reply(new String(Character.toChars(128079)));
                        Thread.sleep(2000);
                        m.reply("  " + new String(Character.toChars(128079)));
                        Thread.sleep(2500);
                        m.reply("  " + NORMAL + "  " + new String(Character.toChars(128079)));
                    } catch(InterruptedException tie) {
                    }
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

    static ArrayList<Integer> emoji = new ArrayList<Integer>();

    private void initEmoji() {
        int ranges[][] = {{127744, 128511}, {128512, 128591}, {128640, 128767}, {9728, 9983}};
        for (int j = 0; j < ranges.length; j++)
            for (int i = ranges[j][0]; i <= ranges[j][1]; i++)
                if (Character.isDefined(i))
                    emoji.add(i);
    }

    private String randEmojiWithName() {
        if (emoji.isEmpty())
            initEmoji();
        int ch = emoji.get(random.nextInt(emoji.size()));
        return new String(Character.toChars(ch)) + "  " + Character.getName(ch);
    }
}
