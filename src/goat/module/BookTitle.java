package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.* ;
import java.util.Arrays ;
import java.util.ArrayList ;
import java.util.Random ;

/**
 * Title:    
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * <p/>
 * @author encontrado
 *
 * @version 1.0
 */

public class BookTitle extends Module {

	private Random random = new Random() ;
	
	private static final String ADJ_FILE = "resources/booktitle.adjectives" ;
	private static final String NOUN_FILE = "resources/booktitle.nouns" ;
	private static final int WORD_LIMIT = 256 ;
	private static String adjectives[] = new String[WORD_LIMIT] ;
	private static String nouns[] = new String[WORD_LIMIT] ;
	private static int num_nouns = 0 ;
	private static int num_adjs = 0 ;
	private static boolean wordsLoaded = false ;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"title", "titleadjectives", "titlenouns"};
   }
	
	public BookTitle() {
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		if (! wordsLoaded) {
			loadWords() ;
		}
		if (m.modCommand.equalsIgnoreCase("title")) {
			String arg = m.modTrailing.trim() ;
			String noun1 = getRandomNoun() ;
			String noun2 = getRandomNoun() ;
			String adj = getRandomAdjective() ;
			boolean noArg = false ;
			if (arg.equals("") || arg.equals(null)) {
				noArg = true ;
			} else {
				if (random.nextBoolean()) {
					noun1 = arg ;
				} else {
					noun2 = arg ;
				}
			}
			ArrayList titles = new ArrayList() ;
			if (noArg) { 
				titles.add(adj + " " + noun1);
				titles.add("The " + adj + " " + noun1);
			} else {
				titles.add(adj + " " + arg);
				titles.add("The " + adj + " " + arg); 
			}
			titles.add(noun1 + " of " + noun2) ;
			titles.add("The " + noun1 + "\'s " + noun2) ;
			titles.add("The " + noun1 + " of the " + noun2) ;
			titles.add(noun1 + " in the " + noun2) ;
			m.createReply((String) titles.remove(random.nextInt(titles.size()))).send();
		} else if(m.modCommand.equalsIgnoreCase("titlenouns")) {
			String reply = num_nouns + " nouns:  " ;
			for(int i=0;i<num_nouns;i++) {
				reply += nouns[i] + " " ;
			}
			m.createPagedReply(reply).send() ;
		} else if(m.modCommand.equalsIgnoreCase("titleadjectives")) {
			String reply = num_adjs + " adjectives:  " ;
			for(int i=0;i<num_adjs;i++) {
				reply += adjectives[i] + " " ;
			}
			m.createPagedReply(reply).send() ;
		} else if(m.modCommand.equalsIgnoreCase("titlewords")) {
			m.createReply("I'm not telling.").send() ;
		}
	}
	
	private String getRandomNoun() {
		if (! wordsLoaded) {
			loadWords() ;
		}
		return nouns[random.nextInt(num_nouns)] ;
	}
	
	private String getRandomAdjective() {
		if (! wordsLoaded) {
			loadWords();
		}
		return adjectives[random.nextInt(num_adjs)] ;
	}
	
	private void loadWords() {
		try {
			String word ;
			BufferedReader br = new BufferedReader(new FileReader(NOUN_FILE));
				while (((word = br.readLine()) != null) && num_nouns < WORD_LIMIT) {
					nouns[num_nouns] = new String(word.trim()) ;
					++num_nouns;
				}
			br.close();
			br = new BufferedReader(new FileReader(ADJ_FILE));
				while (((word = br.readLine()) != null) && num_adjs < WORD_LIMIT) {
					adjectives[num_adjs] = new String(word.trim()) ;
					++num_adjs;
				}
			br.close();
			Arrays.sort(nouns, 0, num_nouns) ;
			Arrays.sort(adjectives, 0, num_adjs) ;
			wordsLoaded = true ;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

}