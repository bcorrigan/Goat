package goat.wordgame;

import goat.core.Constants;
import goat.core.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;


public class Scores implements Comparator<String[]> {

	private static final int NAME = 0;              //Various final statics
	private static final int TOTAL_SCORE = 1;
	private static final int HIGHEST_SCORE = 2;
    private static final String[] SPACES = new String[20]; //for formatting purposes when printing table
    private static final String FILES_DIR = "resources" + File.separator + "wordgame" + File.separator;
    private static final String MATCHSCORES_FILE = FILES_DIR + "matchscores";
    
	private boolean warninggiven;
	private Message target = new Message(" ");		//just the target channel for any given game
	
	private static List<String[]> matchScores = Collections.synchronizedList(new ArrayList<String[]>()); //tracks match scores
	private static boolean isMatchScoresReadFromFile = false;
	
	// We're assuming that only one thread will be mucking with our scores collection, so it's not synchronized...
	private List<String[]> scores = new ArrayList<String[]>();   

	public Scores(Message m) {
		this.target = m;
		init();
	}
	
	/* prevent Scores object from being created without a target Message
	 * 
	 */
	private Scores() {
	}
	
	private void init() {
		try {
			FileInputStream in = new FileInputStream(getScoresFilename());
			ObjectInputStream s = new ObjectInputStream(in);
			scores = (ArrayList<String[]>) s.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			// scores = new ArrayList<String[]>();  //score file doesn't exist, just create an empty ArrayList
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		//construct SPACES array
		for (int i = 0; i < 20; i++) {
			SPACES[i] = " ";
			for (int j = 0; j < i - 1; j++) {
				SPACES[i] += " ";
			}
		}
		synchronized (matchScores) {
			if(! isMatchScoresReadFromFile) 
				try {
					File d = new File(getScoresDir());
					if(! d.exists() )
						d.mkdirs();
					File f = new File(MATCHSCORES_FILE);
					if(! f.exists())
						f.createNewFile();
					if(f.exists() && f.isFile()) {
						if(f.canRead()) {
							FileInputStream in = new FileInputStream(f);
							ObjectInputStream s = new ObjectInputStream(in);
							Object o = s.readObject();
							if(o instanceof ArrayList) {
								ArrayList<?> al = (ArrayList<?>) o;
								for(Object i: al)
									if(i instanceof String[])
										matchScores.add((String[])i);
							}
							in.close();
							isMatchScoresReadFromFile = true;
						} else {
							System.out.println("Can't read file " + MATCHSCORES_FILE);
							// isMatchScoresReadFromFile = true;
						}
					} else {
						System.out.println(MATCHSCORES_FILE + " does not exist, or is not a normal file");
						isMatchScoresReadFromFile = true;
					}
				} catch (FileNotFoundException e) {
					// matchScores = new ArrayList<String[]>();  //score file doesn't exist, just create an empty ArrayList
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
		}
	}

	public void sendScoreTable() {
		int top;
		int largestNick = 0;
		int largesthScore = 0;
		int largestsScore = 0;
		if (scores.size() < 20)
			top = scores.size();
		else
			top = 20;

		if (top == 0) {
			target.createReply("Nobody's got any scores yet :(").send();
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

		target.createReply("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
				+ "HiScore" + SPACES[largesthScore + 7 - 7]
				+ "TotalScore").send();
		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) scores.get(i);
			String is = Integer.toString(i + 1);
			target.createReply(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + entry[NAME] +
					SPACES[largestNick + 3 - entry[NAME].length()] +
					entry[HIGHEST_SCORE] + SPACES[largesthScore + 7 - entry[HIGHEST_SCORE].length()] + entry[TOTAL_SCORE]).send();
		}
	}

	public void sendMatchScoreTable() {
		int top;
		int largestNick = 0;
		int largestsScore = 0;
		synchronized (matchScores) {
			if (matchScores.size() < 20)
				top = matchScores.size();
			else
				top = 20;

			if (top == 0) {
				target.createReply("Nobody has won a match yet :(").send();
				return;
			}

			for (int i = 0; i < top; i++) {
				String[] entry = (String[]) matchScores.get(i);
				if (entry[NAME].length() > largestNick)
					largestNick = entry[NAME].length();
				if (entry[TOTAL_SCORE].length() > largestsScore)
					largestsScore = entry[TOTAL_SCORE].length();
			}

			target.createReply("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
			                                                            + "Matches Won").send();
			for (int i = 0; i < top; i++) {
				String[] entry = (String[]) matchScores.get(i);
				String is = Integer.toString(i + 1);
				target.createReply(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + entry[NAME] +
						SPACES[largestNick + 3 - entry[NAME].length()] + entry[TOTAL_SCORE]).send();
			}
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
					if (tscore >= 140 & !warninggiven) {
						target.createReply(Constants.BOLD + "Warning: " + Constants.BOLD + roundWinner[NAME] + " is within striking distance of victory!").send();
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
			File d = new File(getScoresDir());
			File f = new File(getScoresFilename());
			if(! d.isDirectory())
				d.mkdirs();
			if(f.exists()) {
				if(! f.canWrite()) {
					// might want to do something other than just complain here
					System.out.println("Can't write scores file '" + f + "'");
					target.createReply("I couldn't write to the high-scores file :(").send();
				}
			} else {
				f.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(f);
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
		boolean no_contest = false;
		target.createReply(Constants.BOLD + matchWinner[NAME].toUpperCase() + Constants.BOLD
				+ Constants.YELLOW + " has " + Constants.RED + Constants.UNDERLINE
				+ "WON THE MATCH!!!").send();
        target.createReply("Scores at close of play: ");
        sendScoreTable();
        
		if (scores.size() > 1) {
			String[] entry1, entry2;
			entry1 = (String[]) scores.get(0);
			entry2 = (String[]) scores.get(1);
			int difference = Integer.parseInt(entry1[1]) - Integer.parseInt(entry2[1]);
			target.createReply(matchWinner[NAME] + " won by a clear " + difference + " points.").send();
			if (difference > 150) {
				target.createReply("The scorekeeper has declared this match a " + Constants.UNDERLINE + "no contest" + Constants.UNDERLINE + ", by reason of overwhelming margin of victory.").send() ;
				no_contest = true ;
			}
		} else {
			target.createReply("The scorekeeper has declared this match a " + Constants.UNDERLINE + "no contest" + Constants.UNDERLINE + ", by reason of \"no one else was playing.\"").send() ;
			no_contest = true ;
		}
		
		synchronized (matchScores) {
			if (!no_contest) {
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
					FileOutputStream out = new FileOutputStream(MATCHSCORES_FILE);
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
			}

			scores = new ArrayList<String[]>();
			File file = new File(getScoresFilename());
			file.delete();
			warninggiven = false;
		}

	}

	/*
	public void setTarget(Message m) {
		target = m;
	}
	*/
	
	public String getScoresFilename() {
		return FILES_DIR + target.getChanname().replaceAll("#", "channel.");
	}
	
	public String getScoresDir() {
		return FILES_DIR;
	}

	public int compare(String[] entry1, String[] entry2) {
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
