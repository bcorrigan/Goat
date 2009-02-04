package goat.module;

//import static goat.util.Scores.NAME;
import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.util.Dict;
import goat.util.Scores;
import static goat.util.Scores.*;
import goat.util.ScoresWithMatches;

import java.io.IOException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Barry Corrigan
 *         Date: Apr 25, 2004
 */

public class WordGame extends Module implements Runnable, Comparator<String> {
	
	private Map<String, GameTimer> gamesUnderway = Collections.synchronizedMap(new HashMap<String, GameTimer>());
	//private boolean isTheBoss = true;

	private boolean isTheBoss = true;				//True if a game is being played just now
	private boolean playing = false;
	private Dict dict = new Dict();					//the entire dictionary
	private ArrayList<String> validWords; 			//all the valid answers
    private ArrayList<String> anagrams;             //all the winning answers, ie anagrams of answer
	private ArrayList<Character> letters;			//letters in this match
	private String solution;						//the answer word
	private int longestPossible;   					//longest possible word length for this game

	private String currentWinningPlayer = null;
	private String currentWinningWord = null;
	private int currentWinningScore;

	//private int scoreToWinMatch = 68;
	private int warningThreshold = 40;
	private HashMap<String, Boolean> warningsGiven = new HashMap<String, Boolean>();
	
	private Message target;							//just the target channel for any given game
	
	private Map<String, Long> top10times = new HashMap<String, Long>();			//how long since someone asked for the top10 table       <----\
	private ScoresWithMatches scores;							// running game and match scores
	
	private ExecutorService pool = Goat.modController.getPool();
	
	private static Map<String, String> lastWinningWord;
	{
		lastWinningWord = Collections.synchronizedMap(new HashMap<String, String>());
	}
	
	private WordGame getWorkerInstance(Message target) throws IOException {
		WordGame ret = new WordGame();
		ret.isTheBoss = false;
		ret.target = target;
		ret.gamesUnderway = gamesUnderway;
		ret.scores = new ScoresWithMatches("wordgame",ret.target.getChanname());
		return ret;
	}
	
	public void processPrivateMessage(Message m) {
		
	}

	public void processChannelMessage(Message m) {
		if (isTheBoss) {
			synchronized (gamesUnderway) {
				String key = m.getChanname();
				long now = System.currentTimeMillis();
				if(gamesUnderway.containsKey(key)) {
					gamesUnderway.get(key).getGame().dispatchMessage(m);		
				} else if(m.getModCommand().equalsIgnoreCase("wordgame") 
						|| m.getModCommand().equalsIgnoreCase("nerdgame")) {
					try {
						WordGame newGame = getWorkerInstance(m);
						pool.execute(newGame);  // start up the new game's incoming message queue processor
						GameTimer newTimer = new GameTimer(newGame);
						gamesUnderway.put(key, newTimer);
						pool.submit(newTimer);
						//newTimer.setFuture(pool.submit(newTimer));  // submit() starts the timer, setFuture gives the timer a hook to interrupt its run() thread
					} catch (IOException ioe) {
						m.createReply("I couldn't start a new game due to an i/o exception while setting things up.").send();
						ioe.printStackTrace();
					}
				} else if ((m.getModCommand().equalsIgnoreCase("scores")
								|| m.getModCommand().equalsIgnoreCase("matchscores"))
						&& ((!top10times.containsKey(key)) 
								|| now - top10times.get(key) > 3000L)
						) {
					top10times.put(key, now);
					if(m.getModCommand().equalsIgnoreCase("scores"))
						sendScoreTable(m);
					else if (m.getModCommand().equalsIgnoreCase("matchscores"))
						sendMatchScoreTable(m);
				}
			}
		} else {
			//check for words here and whatnot
			checkMessageIn(m);
		}
		return;
	}

	public int messageType() {
		if (gameUnderway())
			return WANT_UNCLAIMED_MESSAGES;
		else
			return WANT_COMMAND_MESSAGES;
	}

	private boolean gameUnderway() {
		boolean ret = false;
		synchronized(gamesUnderway) {
			for(String chan: gamesUnderway.keySet()) {
				ret = gamesUnderway.get(chan).thisGame.playing;
				if(ret)
					break;
			}
		}
		return ret;
	}
	
	public static String[] getCommands() {
		return new String[]{"wordgame", "nerdgame", "scores", "matchscores"};
	}

	private void finaliseRound() {
		
		playing = false;
		
		String reply;
        boolean wonWithLongest=false;
		lastWinningWord.remove(target.getChanname());
        
		if (currentWinningPlayer != null) {
			reply = currentWinningPlayer + " has won with " + currentWinningWord + " and gets " + currentWinningScore + " points! ";
			if (currentWinningWord.length() == longestPossible) {
				reply += " This was the longest possible. ";
                wonWithLongest=true;
			}
		} else {
			reply = "Nobody guessed a correct answer :( ";
		}
        
		
		
        if(anagrams.size()>1) {
            reply+= anagrams.size() + " possible winning answers: ";
            for (int i=0; i<(anagrams.size()-1); i++) {
                reply += Constants.BOLD + anagrams.get(i) + Constants.BOLD + ", ";
            }
            
        } else if(!wonWithLongest) {
            reply+=" Longest possible: ";
        }
        
        if(anagrams.size() == 1)
        	lastWinningWord.put(target.getChanname(), solution);
        else
        	lastWinningWord.remove(target.getChanname());
        
        if( anagrams.size()>1&&wonWithLongest || !wonWithLongest )
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
		
		if (currentWinningPlayer != null) {
			scores.add(Scores.newEntry(currentWinningPlayer, currentWinningScore));   //commit new score to league table etc
		}
		
		//if(scores.highScore() > scoreToWinMatch) {
		//	finaliseMatch();
		//}
		
		//else if (null != currentWinningPlayer
        //		&& (scores.playerScore(currentWinningPlayer) >= warningThreshold)
        //		&& (null == warningsGiven.get(currentWinningPlayer))) {
		//	target.createReply(Constants.BOLD + "Warning: " + Constants.BOLD + currentWinningPlayer + " is within striking distance of victory!").send();
		//	warningsGiven.put(currentWinningPlayer, true);
		//}	
	}
	
	private void finaliseMatch() {
		//System.out.println("finaliseMatch(): finalising match");
		String[] entry1, entry2;
		entry1 = scores.copyOfEntry(0);
		entry2 = scores.copyOfEntry(1);
		
		target.createReply(Constants.BOLD + entry1[NAME].toUpperCase() + Constants.BOLD
				+ Constants.YELLOW + " has " + Constants.RED + Constants.UNDERLINE
				+ "WON THE MATCH!!!").send();
		target.createReply("Scores at close of play: ");
		sendScoreTable(target);

		boolean noContest = false;
		if (scores.size() > 1) {
			int difference = 0;
			difference = Integer.parseInt(entry1[TOTAL_SCORE]) - Integer.parseInt(entry2[TOTAL_SCORE]);
			target.createReply(entry1[NAME] + " won by a clear " + difference + " points.").send();

			if (difference > 50) {
				target.createReply("The scorekeeper has declared this match a " + Constants.UNDERLINE + "no contest" + Constants.UNDERLINE + ", by reason of overwhelming margin of victory.").send() ;
				noContest = true ;
			}
			//System.out.println("finaliseMatch(): finished scores greater than one");
		} else {
			target.createReply("The scorekeeper has declared this match a " + Constants.UNDERLINE + "no contest" + Constants.UNDERLINE + ", by reason of \"no one else was playing.\"").send() ;
			noContest = true ;
		}
		
		//System.out.println("finaliseMatch(): clearing warnings");
		warningsGiven.clear();
		
		//System.out.println("finaliseMatch(): committing scores");
		if(noContest) {
			//System.out.println("finaliseMatch(): no contest, clearing scores");
			scores.clear();
		} else {
			//System.out.println("finaliseMatch(): committing match scores");
			scores.endMatch();
		}
		//System.out.println("finaliseMatch(): scores saved");
	}

	private void initRound() {
        playing = true;
        currentWinningPlayer = null;
		currentWinningWord = null;
		currentWinningScore = 0;
		getLetters();
		validWords = dict.getMatchingWords(solution);
        anagrams = new ArrayList<String>();
        for(String word:validWords) {
            if( word.length() == solution.length() ) //anagram
                anagrams.add(word);
        }
        String letterString = " ";
        for (Character letter : letters) {
            letterString += letter + " ";
        }
		target.createReply(Constants.REVERSE + "***" + Constants.REVERSE
				+ " New Letters:" + Constants.BOLD
				+ letterString.toUpperCase()).send();
	}

	private void checkMessageIn(Message m) {
		//tokenise message into an array of words
		String[] words = m.getTrailing().split("[\\s,.;]+");
		ArrayList<String> correctWords = new ArrayList<String>();
        for (String word : words) {
            if (wordIsValid(word)) {
                correctWords.add(word.toLowerCase());
                
                boolean firstWin = (currentWinningPlayer == null);
                if (firstWin || currentWinningWord.length() < word.length()) {
                        currentWinningPlayer = m.getSender();
                        currentWinningWord = word.toLowerCase();
                        score();
                        if(firstWin)
                            m.createReply(currentWinningPlayer + " sets the pace with " + currentWinningWord
                                    + ". score:" + currentWinningScore).send();
                        else
                        	m.createReply(currentWinningPlayer + " steals the lead with " + currentWinningWord
                                + ". score: " + currentWinningScore).send();
                    
                } 
                if (word.length() == longestPossible) {
                    //We have a winner!
                    m.createReply(currentWinningPlayer + " WINS IT!!").send();
                    playing = false;
                    finaliseRound();
                    gamesUnderway.get(m.getChanname()).interrupt();
                    
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
		currentWinningScore = currentWinningWord.length();
		if (currentWinningScore == letters.size()) {
			currentWinningScore *= 2;
		}
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
			solution = word;
			longestPossible = word.length();
			letters = new ArrayList<Character>(word.length());
			for (int i = 0; i < word.length(); i++) {
				letters.add(word.charAt(i));
			}
			break;
		}
		Collections.shuffle(letters);
	}
	
	public static String getLastWinningWord(String channel) {
		return lastWinningWord.get(channel);
	}
	
	private void sendScoreTable(Message m) {
		try {
			Scores s = new ScoresWithMatches("wordgame", m.getChanname());
			List<String> lines = s.scoreTable();
			for(String line: lines)
				m.createReply(line).send();
		} catch (IOException ioe) {
			m.createReply("I had an i/o problem while trying to fetch the scores table").send();
		}
	}
	
	private void sendMatchScoreTable(Message m) {
		try {
			Scores s = new ScoresWithMatches("wordgame", m.getChanname());
			List<String> lines = s.matchScoreTable();
			for(String line: lines)
				m.createReply(line).send();
		} catch (IOException ioe) {
			m.createReply("I had an i/o problem while trying to fetch the match scores table").send();
		}
	} 

	private class GameTimer implements Callable<Boolean> {
		
		WordGame thisGame;
		//private Future<?> future;
		private Thread thread = null;
		
		private boolean pauseMatch = false;
		private boolean matchIsPaused = false;
		
		// make no-args constructor inaccessible
		//private GameTimer() {
		//}
		
		public GameTimer(WordGame game) {
			thisGame = game; 
		}

		public Boolean call() {
			thread = Thread.currentThread();
			// do the actual game-playing stuff
			autoMatch(10);
			
			// Debugging line, if we don't see this, we didn't get a clean exit
			// thisGame.target.createReply("Good game, nerds!").send();
			
			// We're done with the game object now, we 
			//   should stop its message-dispatching thread
			//   instead of hoping the garbage collector
			//   will take care of it
			thisGame.stopDispatcher();
			//   and take our game object out of the dispatch table
			gamesUnderway.remove(thisGame.target.getChanname());
			thread = null;
			return true;
		}
		
		public void autoMatch(int rounds) {
			for(int round = 0; round < rounds; round++) {		
				if(round > 0) {
					thisGame.target.createReply("Round " + (round + 1) + " of " + rounds + " starts in 10 seconds").send();
					try{
						Thread.sleep(8000);
					} catch (InterruptedException ie) {}
					if(pauseMatch)
						try {
							matchIsPaused = true;
							wait();
						} catch (InterruptedException ie) {
							matchIsPaused = false;
						}
						
					// count it down
					thisGame.target.createReply("Ready!").send();
					try{
						Thread.sleep(1000);
					} catch (InterruptedException ie) {}
					thisGame.target.createReply("Set!").send();
					try{
						Thread.sleep(1000);
					} catch (InterruptedException ie) {}
				}
				// Start up the game
				thisGame.initRound();

				//wait some seconds, depending on word length
				try {
					Thread.sleep((thisGame.letters.size() * 5 - 10) * 1000);
				} catch (InterruptedException e) {}

				if(thisGame.playing) 
					thisGame.target.createReply(Constants.BOLD + "10 secs..").send();
				if(thisGame.playing)
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {}
				if(thisGame.playing)
					thisGame.finaliseRound();
				if(round < rounds - 1)
					try {
						Thread.sleep(3000);
					} catch (InterruptedException ie) {}			
			}
			// System.out.println("autoMatch(): got outside for loop");
			thisGame.finaliseMatch();
			// System.out.println("autoMatch(): finalized match");
		}
		
		public void interrupt() {
			if(thread != null)
				thread.interrupt();  // send interrupt
		}
		
		public WordGame getGame() {
			return thisGame;
		}
		
		public void pauseMatch() {
			pauseMatch = true;
		}
		
		public void resumeMatch() {
			pauseMatch = false;
			if(matchIsPaused)
				thread.interrupt();
		}
	}

    public int compare(String o1, String o2) {
        if( o1.length()>o2.length() )
            return -1;
        else if ( o1.length() < o2.length() )
            return 1;
        return 0;
    }
}
