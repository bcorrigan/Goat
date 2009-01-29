package goat.module;

import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.wordgame.Scores;
import goat.util.Dict;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Barry Corrigan
 *         Date: Apr 25, 2004
 */

public class WordGame extends Module implements Runnable, Comparator<String> {
	
	private Map<String, GameTimer> gamesUnderway = Collections.synchronizedMap(new HashMap<String, GameTimer>());
	//private boolean isTheBoss = true;

	private boolean playing = false;				//True if a game is being played just now
	private Dict dict = new Dict();					//the entire dictionary
	private ArrayList<String> validWords; 			//all the valid answers
    private ArrayList<String> anagrams;             //all the winning answers, ie anagrams of answer
	private ArrayList<Character> letters;			//letters in this match
	private String answer;							//the answer word
	private int longestPossible;   					//longest possible word length for this game
	private String[] currentWinning; 				//nick of person currently winning with the shortest word, and the winning word     @TODO Awful choice of data structure, this
	private int score;           					//score for this one
	private Message target;							//just the target channel for any given game
	
	// only used in manager instance
	private Map<String, Long> top10times = new HashMap<String, Long>();			//how long since someone asked for the top10 table       <----\
	private Scores scores;							//scores related stuff
	
	// We're going to assume only the manager instance will be accessing the scores map, in a single thread, so it's not synchronized
	private Map<String, Scores> scoresMap = new HashMap<String, Scores>();				

	private static HashMap<String, String> lastAnswers = new HashMap<String, String>();				//HashMap of last answers across many channels

	private static final int NAME = 0;              //Various statics
	private static final int ANSWER = 1;

	private ExecutorService pool = Goat.modController.getPool();
	
	private WordGame getWorkerInstance(Message target) {
		WordGame ret = new WordGame();
		ret.target = target;
		ret.gamesUnderway = gamesUnderway;
		ret.playing = true;
		if(scoresMap.containsKey(target.getChanname())) {
			ret.scores = scoresMap.get(target.getChanname());
		} else {
			ret.scores = new Scores(ret.target);
			scoresMap.put(ret.target.getChanname(), ret.scores);
		}
		ret.initGame();
		return ret;
	}
	
	/*
	public boolean isThreadSafe() {
		return false;
	}
	*/
	
	public void processPrivateMessage(Message m) {
		
	}

	public void processChannelMessage(Message m) {
		if (!playing) {
			synchronized (gamesUnderway) {
				String key = m.getChanname();
				long now = System.currentTimeMillis();
				if(gamesUnderway.containsKey(key)) {
					gamesUnderway.get(key).getGame().dispatchMessage(m);		
				} else if(m.getModCommand().equalsIgnoreCase("wordgame") 
						|| m.getModCommand().equalsIgnoreCase("nerdgame")) {
					WordGame newGame = getWorkerInstance(m);
					pool.execute(newGame);  // start up the new game's incoming message queue processor
					GameTimer newTimer = new GameTimer(newGame);
					gamesUnderway.put(key, newTimer);
					newTimer.setFuture(pool.submit(newTimer));  // submit() starts the timer, setFuture gives the timer a hook to interrupt its run() thread
				} else if ((m.getModCommand().equalsIgnoreCase("scores")
								|| m.getModCommand().equalsIgnoreCase("matchscores"))
						&& ((!top10times.containsKey(key)) 
								|| now - top10times.get(key) > 3000L)
						) {
					Scores s;
					if(scoresMap.containsKey(key))
						s = scoresMap.get(key);
					else {
						s = new Scores(m);
						scoresMap.put(key, s);
					}
					top10times.put(key, now);
					if(m.getModCommand().equalsIgnoreCase("scores"))
						s.sendScoreTable();
					else if (m.getModCommand().equalsIgnoreCase("matchscores"))
						s.sendMatchScoreTable();
				}
			}
		} else {
			//check for words here and whatnot
			checkMessageIn(m);
		}
		
		return;
	}

	public int messageType() {
		if (gamesUnderway.isEmpty() && !playing)
			return WANT_COMMAND_MESSAGES;
		else
			return WANT_UNCLAIMED_MESSAGES;
	}

	public static String[] getCommands() {
		return new String[]{"wordgame", "nerdgame", "scores", "matchscores"};
	}

	private void finaliseGame() {
		
		// order sort of matters here; first we get this game out of the dispatch table, then we 
		// stop the game timer if it's still running, then we set playing to not true, and 
		// finally we stop the game object's message dispatcher;
		GameTimer gt = gamesUnderway.remove(target.getChanname());
		gt.killTimer();
		playing = false;
		gt.getGame().stopDispatcher();
		
		String reply;
        boolean won=false;
		lastAnswers.put(target.getChanname(), answer) ;
		if (currentWinning != null) {
			reply = currentWinning[NAME] + " has won with " + currentWinning[ANSWER] + " and gets " + score + " points! ";
			if (currentWinning[ANSWER].length() == longestPossible) {
				reply += " This was the longest possible. ";
                won=true;
				lastAnswers.put(target.getChanname(), currentWinning[ANSWER]);
			}
		} else {
			reply = "Nobody guessed a correct answer :( ";
		}
        
        if(anagrams.size()>1) {
            reply+= anagrams.size() + " possible winning answers: ";
            for (int i=0; i<(anagrams.size()-1); i++) {
                reply += Constants.BOLD + anagrams.get(i) + Constants.BOLD + ", ";
            }
            
        } else if(!won) {
            reply+=" Longest possible: ";
        }
        
        if( anagrams.size()>1&&won || !won )
            reply+=Constants.BOLD + anagrams.get(anagrams.size()-1) + Constants.BOLD + ". ";
        
        //bung on 5 highest scoring words as examples of answers
        //potentially there could be a bug here if there is only one possible answers,
        //but as all the letters that make up a word are valid answers and all
        //answers are at least 6 letters I don't think it can occur.
        Collections.sort(validWords, this);
        int numberAnswers = validWords.size();
        validWords.removeAll(anagrams);
        int examples = 5;
        if( validWords.size()<5 )
            examples = validWords.size();
        reply+="Top " + examples + " non-winning out of " + numberAnswers + ": ";
        for( int i=0; i<(examples-1); i++ ) {
            reply += validWords.get(i) + ", ";
        } 
        reply += validWords.get(examples-1) + ".";        
        
		target.createPagedReply(reply).send();
		if (currentWinning != null) {
			scores.commit(currentWinning, score);   //commit new score to league table etc
		}

	}

	private void initGame() {
		getLetters();
		validWords = dict.getMatchingWords(answer);
        anagrams = new ArrayList<String>();
        for(String word:validWords) {
            if( word.length() == answer.length() ) //anagram
                anagrams.add(word);
        }
		currentWinning = null;
	}

	private void checkMessageIn(Message m) {
		//tokenise message into an array of words
		String[] words = m.getTrailing().split("[\\s,.;]+");
		ArrayList<String> correctWords = new ArrayList<String>();
        for (String word : words) {
            if (wordIsValid(word)) {
                correctWords.add(word.toLowerCase());
                if (currentWinning != null) {
                    if (currentWinning[ANSWER].length() < word.length()) {
                        currentWinning[NAME] = m.getSender();
                        currentWinning[ANSWER] = word.toLowerCase();
                        score();
                        m.createReply(m.getSender() + " steals the lead with " + word.toLowerCase()
                                + ". score: " + score).send();
                    }
                } else {
                    currentWinning = new String[3];
                    currentWinning[NAME] = m.getSender();
                    currentWinning[ANSWER] = word.toLowerCase();
                    score();
                    m.createReply(m.getSender() + " sets the pace with " + word.toLowerCase()
                            + ". score:" + score).send();
                }
                if (word.length() == longestPossible) {
                    //We have a winner!
                    m.createReply(m.getSender() + " WINS IT!!").send();
                    finaliseGame();
                }
            }
        }
    }

	private boolean wordIsValid(String word) {
        for (Object validWord : validWords) {
            String word2 = (String) validWord;
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

	//unused
	/*
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
	*/

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
			letters = new ArrayList<Character>(word.length());
			for (int i = 0; i < word.length(); i++) {
				letters.add(word.charAt(i));
			}
			break;
		}
		Collections.shuffle(letters);
	}

	private class GameTimer implements Runnable {
		
		WordGame thisGame;
		private Future<?> future;
		
		// make no-args constructor inaccessible
		private GameTimer() {
			// make no-args constructor
		}
		
		public GameTimer(WordGame game) {
			thisGame = game; 
		}

		public void run() {
			// Start up the game
			String letterString = " ";
	        for (Character letter : thisGame.letters) {
	            letterString += letter + " ";
	        }
			thisGame.target.createReply(Constants.REVERSE + "***" + Constants.REVERSE
					+ " New Letters:" + Constants.BOLD
					+ letterString.toUpperCase()).send();

			//wait some seconds, depending on word length
			try {
				Thread.sleep((thisGame.letters.size() * 5 - 10) * 1000);
			} catch (InterruptedException e) {
				System.out.println("Timer for wordgame in " + thisGame.target.getChanname() + " interrupted, game over.");
				return;
			}

			thisGame.target.createReply(Constants.BOLD + "10 secs..").send();

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				System.out.println("Timer for wordgame in " + thisGame.target.getChanname() + " interrupted, game over.");
				return;
			}
			thisGame.finaliseGame();
		}
		
		public void killTimer() {
			thisGame.stopDispatcher();
			future.cancel(true);
		}
		
		public WordGame getGame() {
			return thisGame;
		}

		public Future<?> getFuture() {
			return future;
		}

		public void setFuture(Future<?> future) {
			this.future = future;
		}
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

    public int compare(String o1, String o2) {
        if( o1.length()>o2.length() )
            return -1;
        else if ( o1.length() < o2.length() )
            return 1;
        return 0;
    }
}
