package goat.wordgame;

import goat.core.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class Scores implements Comparator {

	private final static int NAME = 0;              //Various final statics
	private final static int TOTAL_SCORE = 1;
	private final static int HIGHEST_SCORE = 2;
    private final static String[] SPACES = new String[20]; //for formatting purposes when printing table

	private boolean warninggiven;
	private Message target = new Message(" ");		//just the target channel for any given game
	private ArrayList scores;       				//table of scores
	private ArrayList matchScores;					//tracks match scores

	public Scores() {
		try {
			FileInputStream in = new FileInputStream("wordgame/scores");
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
		//construct SPACES array
		for (int i = 0; i < 20; i++) {
			SPACES[i] = " ";
			for (int j = 0; j < (i - 1); j++) {
				SPACES[i] += " ";
			}
		}
		try {
			FileInputStream in = new FileInputStream("wordgame/matchScores");
			ObjectInputStream s = new ObjectInputStream(in);
			matchScores = (ArrayList) s.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			matchScores = new ArrayList();  //score file doesn't exist, just create an empty ArrayList
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void sendScoreTable(Message m) {
		int top;
		int largestNick = 0;
		int largesthScore = 0;
		int largestsScore = 0;
		target = m;
		if (scores.size() < 20)
			top = scores.size();
		else
			top = 20;

		if (top == 0) {
			m.createReply("Nobody's got any scores yet :(").send();
			return;
		}

		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) scores.get(i);
			if (entry[NAME].length() > largestNick)
				largestNick = entry[0].length();
			if (entry[HIGHEST_SCORE].length() > largesthScore)
				largesthScore = entry[2].length();
			if (entry[TOTAL_SCORE].length() > largestsScore)
				largestsScore = entry[1].length();
		}

		m.createReply("   " + Message.UNDERLINE + "Name" + SPACES[(largestNick + 3) - 4]
				+ "HiScore" + SPACES[(largesthScore + 7) - 7]
				+ "TotalScore").send();
		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) scores.get(i);
			String is = Integer.toString(i + 1);
			m.createReply(Message.BOLD + is + Message.BOLD + SPACES[3 - is.length()] + entry[NAME] +
					SPACES[(largestNick + 3) - entry[NAME].length()] +
					entry[HIGHEST_SCORE] + SPACES[(largesthScore + 7) - entry[HIGHEST_SCORE].length()] + entry[TOTAL_SCORE]).send();
		}
	}

	public void sendMatchScoreTable(Message m) {
		int top;
		int largestNick = 0;
		int largestsScore = 0;
		if (matchScores.size() < 20)
			top = matchScores.size();
		else
			top = 20;

		if (top == 0) {
			m.createReply("Nobody has won a match yet :(").send();
			return;
		}

		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) matchScores.get(i);
			if (entry[NAME].length() > largestNick)
				largestNick = entry[NAME].length();
			if (entry[TOTAL_SCORE].length() > largestsScore)
				largestsScore = entry[TOTAL_SCORE].length();
		}

		m.createReply("   " + Message.UNDERLINE + "Name" + SPACES[(largestNick + 3) - 4]
				+ "Matches Won").send();
		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) matchScores.get(i);
			String is = Integer.toString(i + 1);
			m.createReply(Message.BOLD + is + Message.BOLD + SPACES[3 - is.length()] + entry[NAME] +
					SPACES[(largestNick + 3) - entry[NAME].length()] + entry[TOTAL_SCORE]).send();
		}
	}

	public void commit(String[] roundWinner, int score) {
		boolean match = false;
		boolean winner = false;
		if (scores.size() != 0) {
			for (int i = 0; i < scores.size(); i++) {
				String[] entry; //name, total score, highest score
				entry = (String[]) scores.get(i);
				if (entry[0].equals(roundWinner[NAME])) {
					int tscore = Integer.parseInt(entry[TOTAL_SCORE]);
					int hscore = Integer.parseInt(entry[HIGHEST_SCORE]);
					tscore += score;
					if (tscore >= 168)
						winner = true;
					if (tscore >= 140 & warninggiven == false) {
						target.createReply(Message.BOLD + "Warning: " + Message.BOLD + roundWinner[NAME] + " is within striking distance of victory!").send();
						warninggiven = true;
					}
					//giveWarning();
					entry[TOTAL_SCORE] = Integer.toString(tscore);
					if (score > hscore)
						entry[HIGHEST_SCORE] = Integer.toString(score);
					scores.set(i, entry);
					match = true;
				}
			}
		}

		if (winner) {
			commitTopScores(roundWinner);
			return;
		}

		if (!match) {
			String[] entry = new String[3];
			entry[NAME] = roundWinner[NAME];
			entry[TOTAL_SCORE] = Integer.toString(score);
			entry[HIGHEST_SCORE] = Integer.toString(score);
			scores.add(entry);
		}
		//reorder the list
		Collections.sort(scores, this);

		//now write the list out to a file
		try {
			FileOutputStream fos = new FileOutputStream("wordgame/scores");
			ObjectOutput out = new ObjectOutputStream(fos);
			out.writeObject(scores);
			out.flush();
			fos.flush();
			out.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void commitTopScores(String[] matchWinner) {
		boolean match = false;
		target.createReply(Message.BOLD + matchWinner[NAME].toUpperCase() + Message.BOLD
				+ Message.YELLOW + " has " + Message.RED + Message.UNDERLINE
				+ "WON THE MATCH!!!").send();
		if (scores.size() > 1) {
			String[] entry1, entry2;
			entry1 = (String[]) scores.get(0);
			entry2 = (String[]) scores.get(1);
			int difference = Integer.parseInt(entry1[1]) - Integer.parseInt(entry2[1]);
			target.createReply(matchWinner[NAME] + " won by a clear " + difference + " points.").send();
		}
		if (matchScores.size() != 0) {
			for (int i = 0; i < matchScores.size(); i++) {
				String[] entry; //name, total score, highest score
				entry = (String[]) matchScores.get(i);
				if (entry[NAME].equals(matchWinner[NAME])) {
					int tscore = Integer.parseInt(entry[TOTAL_SCORE]);
					tscore += 1;
					entry[TOTAL_SCORE] = Integer.toString(tscore);
					matchScores.set(i, entry);
					match = true;
				}
			}
		}

		if (!match) {
			String[] entry = new String[3];
			entry[NAME] = matchWinner[NAME];
			entry[TOTAL_SCORE] = "1";
			matchScores.add(entry);
		}

		//reorder the list
		Collections.sort(matchScores, this);

		//now write the list out to a file
		try {
			FileOutputStream out = new FileOutputStream("wordgame/matchScores");
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(matchScores);
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
		File file = new File("wordgame/scores");
		file.delete();
		warninggiven = false;

	}

	public void setTarget(Message m) {
		target = m;
	}

	public int compare(Object o, Object o1) {
		String[] entry1 = (String[]) o;
		String[] entry2 = (String[]) o1;
		int score1 = Integer.parseInt(entry1[TOTAL_SCORE]);
		int score2 = Integer.parseInt(entry2[TOTAL_SCORE]);
		if (score1 > score2)
			return -1;
		if (score1 < score2)
			return 1;
		if (score1 == score2)
			return 0;
		return 0;
	}
}
