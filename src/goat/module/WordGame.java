package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.wordgame.Scores;

import java.io.*;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Iterator;

/**
 * User: bc
 * Date: Apr 25, 2004
 */

public class WordGame extends Module implements Runnable {

	private boolean playing;						//True if a game is being played just now
	private ArrayList dict = new ArrayList();		//the entire dictionary
	private ArrayList validWords = new ArrayList(); //all the valid answers
	private ArrayList letters;						//letters in this match
	private int longestPossible;   					//shortest possible word length for this game
	private String[] currentWinning; 				//nick of person currently winning with the shortest word, and the winning word     @TODO Awful choice of data structure, this
	private int score;           					//score for this one
	private Message target = new Message(" ");		//just the target channel for any given game
	private Thread t;								//the timing thread
	private long top10time;							//how long since someone asked for the top10 table       <----\
	private long matchscorestime;					//how long since someone asked for the match score table <-----\__These two to stop users from being able to make the bot flood
	private Scores scores = new Scores();			//scores related stuff

	private final static int NAME = 0;              //Various statics
	private final static int ANSWER = 1;


	public WordGame() {
		File words = new File("resources/words");
		try {
			BufferedReader in = new BufferedReader(new FileReader(words));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				dict.add(inputLine.toLowerCase());
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
		if (!playing) {
			if (m.modCommand.equals("wordgame")) {
				playing = true;
				scores.setTarget(m);
				target = m;
				initGame();
				String nineLettersString = new String(" ");
				Iterator it = letters.iterator();
				while (it.hasNext()) {
					nineLettersString += ((Character) it.next()).charValue() + " ";
				}
				m.createReply(Message.REVERSE + "***" + Message.REVERSE
						+ " New Letters:" + Message.BOLD
						+ nineLettersString.toUpperCase()).send();
				return;
			}
			if (m.modCommand.equals("scores") & ((System.currentTimeMillis() - top10time) > 30000l)) {
				top10time = System.currentTimeMillis();
				scores.sendScoreTable(m);
			}
			if (m.modCommand.equals("matchscores") & ((System.currentTimeMillis() - matchscorestime) > 30000l)) {
				matchscorestime = System.currentTimeMillis();
				scores.sendMatchScoreTable(m);
			}
			return;
		} else {
			//check for words here and whatnot
			checkMessageIn(m);
		}
	}

	public int messageType() {
		if (!playing)
			return WANT_COMMAND_MESSAGES;
		else
			return WANT_UNCLAIMED_MESSAGES;
	}

	public String[] getCommands() {
		return new String[]{"wordgame", "scores", "matchscores"};
	}

	private void finaliseGame(Message m) {
		String reply = "";
		boolean shortest = false;
		if (currentWinning != null) {
			reply = currentWinning[NAME] + " has won with " + currentWinning[ANSWER] + " and gets " + (int) score + " points!";
			if (currentWinning[ANSWER].length() == longestPossible) {
				reply += " This was the longest possible.";
				shortest = true;
			}
		} else
			reply = "Nobody guessed a correct answer :(";
		if (!shortest)
			reply += " A longest possible word was \"" + getShortest(validWords) + "\".";
		m.createReply(reply).send();
		if (currentWinning != null) {
			scores.commit(currentWinning, score);   //commit new score to league table etc
		}
		playing = false;
		validWords = new ArrayList();
		currentWinning = null;
		t.stop();  //yikes! @TODO: FIX THIS! Make it pass a message to tell other thread to stop
	}

	private void initGame() {
		//first find valid three letters for this game
		getLetters();
		t = new Thread(this);
		t.start();
		//now cache all words that match the letters, for speed purposes
		Iterator it = dict.iterator();
		longestPossible = 1;
		while (it.hasNext()) {
			String word = (String) it.next();
			if (checkWord(word)) {
				validWords.add(word);
				if (word.length() > longestPossible)
					longestPossible = word.length();
			}
		}
		currentWinning = null;
	}

	private void checkMessageIn(Message m) {
		//tokenise message into an array of words
		String[] words = m.trailing.split("[\\s,.;]+");
		ArrayList correctWords = new ArrayList();
		for (int i = 0; i < words.length; i++) {
			if (scanDictForWord(words[i].toLowerCase())) {
				if (checkWord(words[i].toLowerCase())) { //you need to check it is actually a word you clown
					correctWords.add(words[i].toLowerCase());
					if (currentWinning != null) {
						if (currentWinning[ANSWER].length() < words[i].length()) {
							currentWinning[NAME] = m.sender;
							currentWinning[ANSWER] = words[i].toLowerCase();
							score();
							m.createReply(m.sender + " steals the lead with " + words[i].toLowerCase()
									+ ". score: " + (int) score).send();
						}
					} else {
						currentWinning = new String[3];
						currentWinning[NAME] = m.sender;
						currentWinning[ANSWER] = words[i].toLowerCase();
						score();
						m.createReply(m.sender + " sets the pace with " + words[i].toLowerCase()
								+ ". score:" + (int) score).send();
					}
					if (words[i].length() == longestPossible) {
						//We have a winner!
						m.createReply(m.sender + " WINS IT!!").send();
						finaliseGame(m);
					}
				}
			}
		}
	}

	private void score() {
		score = currentWinning[ANSWER].length();
		if (score == letters.size()) {
			score *= 2;
		}
	}

	private boolean scanDictForWord(String word) {
		if (dict.contains(word))
			return true;
		return false;
	}

	/**
	 * Checks if a given word is valid for current char arraylist.
	 *
	 * @param word Word to be checked.
	 * @return True if matches, false if not.
	 */
	private boolean checkWord(String word) {
		Iterator it = letters.iterator();
		ArrayList wordLetters = new ArrayList();
		for (int i = 0; i < word.length(); i++) {
			wordLetters.add(new Character(word.charAt(i)));
		}
		while (it.hasNext()) {
			char letter = ((Character) it.next()).charValue();
			for (int i = 0; i < wordLetters.size(); i++) {
				if (wordLetters.size() == 0)
					return false;
				char wordLetter = ((Character) wordLetters.get(i)).charValue();
				if (wordLetter == letter) {
					wordLetters.remove(i);
					break;
				}
			}
		}
		if (wordLetters.size() == 0)
			return true;
		return false;
	}

	private String getShortest(ArrayList list) {
		Iterator it = list.iterator();
		String shortestWord = "";
		while (it.hasNext()) {
			String word = (String) it.next();
			if (word.length() == longestPossible) {
				shortestWord = word;
				break;
			}
		}
		return shortestWord;
	}

	/**
	 * Create random letters.
	 */

	private void getLetters() {
		int dictsize = dict.size();
		letters = new ArrayList();
		int wordLength = (int) (Math.random() * 8) + 7;
		while (true) {
			String word = (String) dict.get((int) (Math.random() * dictsize));
			if (word.length() == wordLength) {
				for (int i = 0; i < word.length(); i++) {
					letters.add(new Character(word.charAt(i)));
				}
				break;
			}
		}
		Collections.shuffle(letters);
	}

	public void run() {
		//wait 25 seconds
		try {
			Thread.sleep(((letters.size() * 5) - 10) * 1000);
		} catch (InterruptedException e) {
		}

		target.createReply(Message.BOLD + "10 secs..").send();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

		playing = false;
		finaliseGame(target);


	}
}
