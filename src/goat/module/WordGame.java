package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.wordgame.Scores;
import goat.util.Dict;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;

/**
 * @author Barry Corrigan
 *         Date: Apr 25, 2004
 */

public class WordGame extends Module implements Runnable {

	private boolean playing;						//True if a game is being played just now
	private Dict dict = new Dict();					//the entire dictionary
	private ArrayList validWords; 					//all the valid answers
	private ArrayList letters;						//letters in this match
	private String answer;							//the answer word
	private int longestPossible;   					//longest possible word length for this game
	private String[] currentWinning; 				//nick of person currently winning with the shortest word, and the winning word     @TODO Awful choice of data structure, this
	private int score;           					//score for this one
	private Message target;							//just the target channel for any given game
	private Thread t;								//the timing thread
	private long top10time;							//how long since someone asked for the top10 table       <----\
	private long matchscorestime;					//how long since someone asked for the match score table <-----\__These two to stop users from being able to make the bot flood
	private Scores scores = new Scores();			//scores related stuff

	private static HashMap lastAnswers = new HashMap();				//HashMap of last answers across many channels

	private static final int NAME = 0;              //Various statics
	private static final int ANSWER = 1;

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
		if (!playing) {
			if (m.modCommand.equals("wordgame")) {
				playing = true;
				scores.setTarget(m);
				target = m;
				initGame();
				String letterString = " ";
				Iterator it = letters.iterator();
				while (it.hasNext()) {
					letterString += ((Character) it.next()).charValue() + " ";
				}
				m.createReply(Message.REVERSE + "***" + Message.REVERSE
						+ " New Letters:" + Message.BOLD
						+ letterString.toUpperCase()).send();
				return;
			}
			if (m.modCommand.equals("scores") & System.currentTimeMillis() - top10time > 30000L) {
				top10time = System.currentTimeMillis();
				scores.sendScoreTable(m);
			}
			if (m.modCommand.equals("matchscores") & System.currentTimeMillis() - matchscorestime > 30000L) {
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
		String reply;
		lastAnswers.put(m.channame, answer) ;
		if (currentWinning != null) {
			reply = currentWinning[NAME] + " has won with " + currentWinning[ANSWER] + " and gets " + score + " points!";
			if (currentWinning[ANSWER].length() == longestPossible) {
				reply += " This was the longest possible.";
				lastAnswers.put(m.channame, currentWinning[ANSWER]) ;
			} else
				reply += "  The longest possible word was \"" + answer + "\".";
		} else {
			reply = "Nobody guessed a correct answer :(";
			reply += "  A longest possible word was \"" + answer + "\".";
		}
		m.createReply(reply).send();
		if (currentWinning != null) {
			scores.commit(currentWinning, score);   //commit new score to league table etc
		}
		playing = false;
		validWords = null;
		currentWinning = null;
		t.stop();  //yikes! @TODO: FIX THIS! Make it pass a message to tell other thread to stop
	}

	private void initGame() {
		getLetters();
		t = new Thread(this);
		t.start();
		validWords = dict.getMatchingWords(answer);
		currentWinning = null;
	}

	private void checkMessageIn(Message m) {
		//tokenise message into an array of words
		String[] words = m.trailing.split("[\\s,.;]+");
		ArrayList correctWords = new ArrayList();
		for (int i = 0; i < words.length; i++) {
			if (dict.contains(words[i].toLowerCase())) {
				if (wordIsValid(words[i])) {
					correctWords.add(words[i].toLowerCase());
					if (currentWinning != null) {
						if (currentWinning[ANSWER].length() < words[i].length()) {
							currentWinning[NAME] = m.sender;
							currentWinning[ANSWER] = words[i].toLowerCase();
							score();
							m.createReply(m.sender + " steals the lead with " + words[i].toLowerCase()
									+ ". score: " + score).send();
						}
					} else {
						currentWinning = new String[3];
						currentWinning[NAME] = m.sender;
						currentWinning[ANSWER] = words[i].toLowerCase();
						score();
						m.createReply(m.sender + " sets the pace with " + words[i].toLowerCase()
								+ ". score:" + score).send();
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

	private boolean wordIsValid(String word) {
		Iterator it = validWords.iterator();
		while (it.hasNext()) {
			String word2 = (String) it.next();
			if (word2.toLowerCase().equals(word.toLowerCase()))
				return true;
		}
		return false;
	}

	private void score() {
		score = currentWinning[ANSWER].length();
		if (score == letters.size()) {
			score *= 2;
		}
	}

	private String getLongest(ArrayList list) {
		Iterator it = list.iterator();
		String longestWord = "";
		while (it.hasNext()) {
			String word = (String) it.next();
			if (word.length() == longestPossible) {
				longestWord = word;
				break;
			}
		}
		return longestWord;
	}

	/**
	 * Create random letters.
	 */

	private void getLetters() {
		String word;

		while (true) {
			word = dict.getRandomWord();
			if (word.length() < 6)
				continue;
			answer = word;
			longestPossible = word.length();
			letters = new ArrayList(word.length());
			for (int i = 0; i < word.length(); i++) {
				letters.add(new Character(word.charAt(i)));
			}
			break;
		}
		Collections.shuffle(letters);
	}

	public void run() {
		//wait 25 seconds
		try {
			Thread.sleep((letters.size() * 5 - 10) * 1000);
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

	/**
	 * You can get the answer to the last game played in the channel passed here.
	 *
	 * @param chan The channel for which caller wants last answer.
	 * @return Answer to the last game played in that channel, if any, or null.
	 */
	public String getLastAnswer(String chan) {
		if (lastAnswers.containsKey(chan))
			return (String) lastAnswers.get(chan);
		return null;
	}
}
