package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.*;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Iterator;

/**
 * User: bc
 * Date: Apr 25, 2004
 */

public class WordGame extends Module implements Runnable {

	boolean playing;
	ArrayList dict = new ArrayList();
	ArrayList validWords = new ArrayList();
	ArrayList letters;
	//String threeLettersString;
	int shortestPossible;   //shortest possible word length for this game
	String[] currentWinner; //nick of person currently winning with the shortest word, and the winning word
	//int guesses;
	int score;           //score for this one
	ArrayList scores;       //table of scores
	ArrayList topscores;	//all time best scores
	final String[] spaces = new String[20];        //for formatting purposes
	Message target = new Message(" ");	//just the target channel for any given game
	Thread t;
	long top10time;
	long matchscorestime;
	boolean warninggiven;


	public WordGame() {
		File words = new File("words");
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

		try {
			FileInputStream in = new FileInputStream("scores");
			ObjectInputStream s = new ObjectInputStream(in);
			scores = (ArrayList) s.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			scores = new ArrayList();  //score file doesn't exist, just create an empty ArrayList
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		//construct spaces array
		for (int i = 0; i < 20; i++) {
			spaces[i] = " ";
			for (int j = 0; j < (i - 1); j++) {
				spaces[i] += " ";
			}
		}
		try {
			FileInputStream in = new FileInputStream("topscores");
			ObjectInputStream s = new ObjectInputStream(in);
			topscores = (ArrayList) s.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			topscores = new ArrayList();  //score file doesn't exist, just create an empty ArrayList
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
		if (!playing) {
			if (m.modCommand.equals("wordgame")) {
				playing = true;
				target = m;
				initGame();
				String nineLettersString = new String(" ");
				Iterator it = letters.iterator();
				while (it.hasNext()) {
					nineLettersString += ((Character) it.next()).charValue() + " ";
				}
				sendMessage(m.createReply(Message.REVERSE + "***" + Message.REVERSE
						+ " New Letters:" + Message.BOLD
						+ nineLettersString.toUpperCase()));
				return;
			}
			if (m.modCommand.equals("scores") & ((System.currentTimeMillis()-top10time)>30000l)) {
				top10time = System.currentTimeMillis();
				int top;
				int largestNick = 0;
				int largesthScore = 0;
				int largestsScore = 0;
				if (scores.size() < 20)
					top = scores.size();
				else
					top = 20;

				if (top == 0) {
					sendMessage(m.createReply("Nobody's got any scores yet :("));
					return;
				}

				for (int i = 0; i < top; i++) {
					String[] entry = (String[]) scores.get(i);
					if (entry[0].length() > largestNick)
						largestNick = entry[0].length();
					if (entry[2].length() > largesthScore)
						largesthScore = entry[2].length();
					if (entry[1].length() > largestsScore)
						largestsScore = entry[1].length();
				}

				sendMessage(m.createReply("   " + Message.UNDERLINE + "Name" + spaces[(largestNick + 3) - 4]
						+ "HiScore" + spaces[(largesthScore + 7) - 7]
						+ "TotalScore"));
				for (int i = 0; i < top; i++) {
					String[] entry = (String[]) scores.get(i);
					String is = Integer.toString(i + 1);
					sendMessage(m.createReply(Message.BOLD + is + Message.BOLD + spaces[3 - is.length()] + entry[0] +
							spaces[(largestNick + 3) - entry[0].length()] +
							entry[2] + spaces[(largesthScore + 7) - entry[2].length()] + entry[1]));
				}
			}
			if (m.modCommand.equals("matchscores") & ((System.currentTimeMillis()-matchscorestime)>30000l)) {
				matchscorestime = System.currentTimeMillis();
				int top;
				int largestNick = 0;
				int largesthScore = 0;
				int largestsScore = 0;
				if (topscores.size() < 20)
					top = topscores.size();
				else
					top = 20;

				if (top == 0) {
					sendMessage(m.createReply("Nobody has won a match yet :("));
					return;
				}

				for (int i = 0; i < top; i++) {
					String[] entry = (String[]) topscores.get(i);
					if (entry[0].length() > largestNick)
						largestNick = entry[0].length();
					//if (entry[2].length() > largesthScore)
					//	largesthScore = entry[2].length();
					if (entry[1].length() > largestsScore)
						largestsScore = entry[1].length();
				}

				sendMessage(m.createReply("   " + Message.UNDERLINE + "Name" + spaces[(largestNick + 3) - 4]
						//+ "HiScore" + spaces[(largesthScore + 7) - 7]
						+ "Matches Won"));
				for (int i = 0; i < top; i++) {
					String[] entry = (String[]) topscores.get(i);
					String is = Integer.toString(i + 1);
					sendMessage(m.createReply(Message.BOLD + is + Message.BOLD + spaces[3 - is.length()] + entry[0] +
							//spaces[(largestNick + 3) - entry[0].length()] +
							/*entry[2] +*/ spaces[(largestNick + 3) - entry[0].length()] + entry[1]));
				}
			}
			return;
		} else {
			//check for words here and whatnot
			checkMessageIn(m);
			//guesses++;
			//if (guesses == 10) {
			//    finaliseGame(m);
			//}
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
		if (currentWinner != null) {
			reply = currentWinner[0] + " has won with " + currentWinner[1] + " and gets " + (int) score + " points!";
			if (currentWinner[1].length() == shortestPossible) {
				reply += " This was the longest possible.";
				shortest = true;
			}
		} else
			reply = "Nobody guessed a correct answer :(";
		if (!shortest)
			reply += " A longest possible word was \"" + getShortest(validWords) + "\".";
		/*if (validWords.size() > 1) {
			reply += " Other possible words included: ";
			int j;
			if (validWords.size() >= 10)
				j = 10;
			else
				j = validWords.size();
			Collections.shuffle(validWords); //randomise the list for output
			for (int i = 0; i < (j - 1); i++)
				reply += (String) validWords.get(i) + ", ";
			reply += (String) validWords.get(j - 1) + "...";
		}*/
		sendMessage(m.createReply(reply));
		if (currentWinner != null) {
			commit();   //commit new score to league table etc
		}
		playing = false;
		validWords = new ArrayList();
		currentWinner = null;
		t.stop();
	}

	private void initGame() {
		//first find valid three letters for this game
		boolean match = false;
		//guesses = 0;
		getLetters();
		t = new Thread(this);
		t.start();
		//now cache all words that match the letters, for speed purposes
		Iterator it = dict.iterator();
		shortestPossible = 1;
		while (it.hasNext()) {
			String word = (String) it.next();
			if (checkWord(word)) {
				validWords.add(word);
				if (word.length() > shortestPossible)
					shortestPossible = word.length();
			}
		}   ///////

		//threeLettersString = new String(letters);
		currentWinner = null;
	}

	private void checkMessageIn(Message m) {
		//tokenise message into an array of words
		String[] words = m.trailing.split("[\\s,.;]+");
		ArrayList correctWords = new ArrayList();
		for (int i = 0; i < words.length; i++) {
			if (scanDictForWord(words[i].toLowerCase())) {
				if (checkWord(words[i].toLowerCase())) { //you need to check it is actually a word you clown
					correctWords.add(words[i].toLowerCase());
					if (currentWinner != null) {
						if (currentWinner[1].length() < words[i].length()) {
							currentWinner[0] = m.sender;
							currentWinner[1] = words[i].toLowerCase();
							score();
							sendMessage(m.createReply(m.sender + " steals the lead with " + words[i].toLowerCase()
									+ ". score: " + (int) score));
						}
					} else {
						currentWinner = new String[3];
						currentWinner[0] = m.sender;
						currentWinner[1] = words[i].toLowerCase();
						score();
						sendMessage(m.createReply(m.sender + " sets the pace with " + words[i].toLowerCase()
								+ ". score:" + (int) score));
					}
					if (words[i].length() == shortestPossible) {
						//We have a winner!
						sendMessage(m.createReply(m.sender + " WINS IT!!"));
						finaliseGame(m);
					}
				}
			}
		}
	}

	private void score() {
		//double lengthScore = Math.pow(1.5, currentWinner[1].length());
		//double difficulty = (1.0 / validWords.size()) * 50.0;
		//score = (int) ((guesses + 1) + lengthScore + difficulty);
		//score = Math.round(score * 100) / 100; //round to 2 decimal places
		score = currentWinner[1].length();
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
			if (word.length() == shortestPossible) {
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
		/*String consonants = "bcdfghjklmnpqrstvwxyz";
		String vowels = "aeeiou";
		letters = new ArrayList();
		int numberVowels = 3 + ((int) Math.random()*3);
		for (int i = 0; i < numberVowels; i++)
			letters.add(new Character(vowels.charAt((int) (Math.random() * vowels.length()))));
		for(int i=0; i<(9-numberVowels); i++)
			letters.add(new Character(consonants.charAt((int) (Math.random() * consonants.length())))); */
		int dictsize = dict.size();
		boolean match = false;
		letters = new ArrayList();
		int wordLength = (int) (Math.random() * 8) + 7;
		while (!match) {
			String word = (String) dict.get((int) (Math.random() * dictsize));
			if (word.length() == wordLength) {
				for (int i = 0; i < word.length(); i++) {
					letters.add(new Character(word.charAt(i)));
				}
				match = true;
			}

		}
		Collections.shuffle(letters);
	}

	private void commit() {
		boolean match = false;
		boolean winner = false;
		if (scores.size() != 0) {
			for (int i = 0; i < scores.size(); i++) {
				String[] entry; //name, total score, highest score
				entry = (String[]) scores.get(i);
				if (entry[0].equals(currentWinner[0])) {
					int tscore = Integer.parseInt(entry[1]);
					int hscore = Integer.parseInt(entry[2]);
					tscore += score;
					if(tscore>=168)
						winner=true;
					if(tscore>=140 & warninggiven==false)
                    	giveWarning();
					entry[1] = Integer.toString(tscore);
					if (score > hscore)
						entry[2] = Integer.toString(score);
					scores.set(i, entry);
					match = true;
				}
			}
		}

		if(winner) {
			commitTopScores();
			return;
		}

		if (!match) {
			String[] entry = new String[3];
			entry[0] = currentWinner[0];
			entry[1] = Integer.toString(score);
			entry[2] = Integer.toString(score);
			scores.add(entry);
		}
		//reorder the list
		CompareScores comparer = new CompareScores();
		Collections.sort(scores, comparer);

		//now write the list out to a file
		try {
			FileOutputStream out = new FileOutputStream("scores");
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(scores);
			s.flush();
			s.close();
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		//wait 25 seconds
		try {
			Thread.sleep(((letters.size()*5)-10)*1000);
		} catch (InterruptedException e) {
		}

		sendMessage(target.createReply(Message.BOLD + "10 secs.."));

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}

        	playing=false;
			finaliseGame(target);


	}

	private void commitTopScores() {
		boolean match = false;
		sendMessage(target.createReply(Message.BOLD + currentWinner[0].toUpperCase() + Message.BOLD
										+ Message.YELLOW + " has " + Message.RED + Message.UNDERLINE
										+ "WON THE MATCH!!!"));
		if(scores.size()>1) {
			String[] entry1, entry2;
			entry1 = (String[]) scores.get(0);
			entry2 = (String[]) scores.get(1);
			int difference = Integer.parseInt(entry1[1]) - Integer.parseInt(entry2[1]);
			sendMessage(target.createReply(currentWinner[0] + " won by a clear " + difference + " points."));
		}
		if (topscores.size() != 0) {
			for (int i = 0; i < topscores.size(); i++) {
				String[] entry; //name, total score, highest score
				entry = (String[]) topscores.get(i);
				if (entry[0].equals(currentWinner[0])) {
					int tscore = Integer.parseInt(entry[1]);
					//int hscore = Integer.parseInt(entry[2]);
					tscore += 1;

					entry[1] = Integer.toString(tscore);
					//if (score > hscore)
					//	entry[2] = Integer.toString(score);
					topscores.set(i, entry);
					match = true;
				}
			}
		}

		if (!match) {
			String[] entry = new String[3];
			entry[0] = currentWinner[0];
			entry[1] = "1";
			//entry[2] = Integer.toString(score);
			topscores.add(entry);
		}

		//reorder the list
		CompareScores comparer = new CompareScores();
		Collections.sort(topscores, comparer);

		//now write the list out to a file
		try {
			FileOutputStream out = new FileOutputStream("topscores");
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(topscores);
			s.flush();
			s.close();
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		scores = new ArrayList();
		File file = new File("scores");
		file.delete();
		warninggiven = false;

	}

	private void giveWarning() {
		sendMessage(target.createReply(Message.BOLD + "Warning: " + Message.BOLD + currentWinner[0] + " is within striking distance of victory!"));
		warninggiven=true;

	}
}
