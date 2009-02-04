package goat.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;


public class Scores implements Comparator<String[]> {

	public static final int NAME = 0;              //Various final statics
	public static final int TOTAL_SCORE = 1;
	public static final int SCORE = 1;
	public static final int HIGHEST_SCORE = 2;
    protected static final String[] SPACES = new String[20]; //for formatting purposes when printing table
    
    private static final String BASE_DIR = "resources" + File.separator + "scores";
   // private static final String FILES_DIR = "resources" + File.separator + "wordgame" + File.separator;
    //private static final String MATCHSCORES_FILE = FILES_DIR + "matchscores";
    
	// private boolean warninggiven;
	// private Message target = new Message(" ");		//just the target channel for any given game
	
	// private static List<String[]> matchScores = Collections.synchronizedList(new ArrayList<String[]>()); //tracks match scores
	// private static boolean isMatchScoresReadFromFile = false;
	

  
	//private File directory;
	private File file;
	protected List<String[]> scores = Collections.synchronizedList(new ArrayList<String[]>());
	private Map<String, String[]> nameToEntry = Collections.synchronizedMap(new HashMap<String, String[]>());

	private static Map<String, List<String[]>> cache;
	private static Map<String, File> filesCache;
	{
		cache = Collections.synchronizedMap(new HashMap<String, List<String[]>>());
		filesCache = Collections.synchronizedMap(new HashMap<String, File>());
	}
	
	protected Scores() {
	}
		
	/*
	protected Scores(String dirname, String filename) throws IOException {	
		//directory = getDirectory(dirname);
		file = getFile(dirname, filename);
	}
	 */

	public Scores(String dirname, String filename) throws IOException {
		String key = makeKey(dirname, filename);
		
		synchronized(cache) {
			if(cache.containsKey(key)) {
				scores = cache.get(key);
				file = filesCache.get(key);
			} else {
				file = getFile(dirname, filename);
				try {
					readFile();
				} catch (FileNotFoundException fnfe) {
					// expected, OK, we're creating a new file down the road
				} catch (ClassNotFoundException cnfe) {
					System.out.println("Scores file " + file + " contained something other than scores objects.  Continuing, file will be overwritten!");
				}
				cache.put(key, this.scores);
				filesCache.put(key, this.file);
			}
		}
	}
	
	private static File getFile(String dirname, String filename) {
		String name = BASE_DIR + File.separator + dirname + File.separator + filename;
		return new File(name);
	}
	
	private static String makeKey(String dirname, String filename) throws IOException {
		return getFile(dirname,filename).getCanonicalPath();
	}
	
	public static String[] newEntry(String name, int score) {
		return new String[]{name, Integer.toString(score), Integer.toString(score)};
	}
	
	public static String[] newEntry(String name, int score, int extraFields) {
		String ret[] = new String[3 + extraFields];
		ret[NAME] = name;
		ret[TOTAL_SCORE] = Integer.toString(score);
		ret[HIGHEST_SCORE] = ret[TOTAL_SCORE];
		return ret;
	}
	
	public int size() {
		return scores.size();
	}

	public void add(String[] roundWinner) {
		synchronized(scores) {
			synchronized(nameToEntry) {
				if (nameToEntry.containsKey(roundWinner[NAME])) {
					int roundScore = Integer.parseInt(roundWinner[SCORE]);
					int roundHighScore = Integer.parseInt(roundWinner[HIGHEST_SCORE]);
					String[] entry = nameToEntry.get(roundWinner[NAME]);
					int tscore = Integer.parseInt(entry[TOTAL_SCORE]);
					int hscore = Integer.parseInt(entry[HIGHEST_SCORE]);
					tscore += roundScore;
					entry[TOTAL_SCORE] = Integer.toString(tscore);
					if (roundHighScore > hscore)
						entry[HIGHEST_SCORE] = Integer.toString(roundHighScore);
				} else {
					// System.out.println("don't already have user " + roundWinner[NAME] + " in nameToEntry");
					scores.add(roundWinner);
					nameToEntry.put(roundWinner[NAME], roundWinner);
				}
				Collections.sort(scores, this);
				try {
					saveToFile();
				} catch (FileNotFoundException fnfe) {
					System.out.println("Problem writing scores table: " + fnfe.getMessage());
				} catch (IOException ioe) {
					System.out.println("Problem writing scores table: " + ioe.getMessage());
				}
			}
		}
	}

	private void saveToFile() 
	throws FileNotFoundException, IOException {
		synchronized(file) {
			synchronized(scores) {
				if(! file.getParentFile().exists() )
					file.getParentFile().mkdirs();			
				FileOutputStream out = new FileOutputStream(file);
				ObjectOutputStream s = new ObjectOutputStream(out);
				s.writeObject(scores);
				s.close();
				out.close();
			}
		}
	}
	
	private void readFile() 
	throws FileNotFoundException, ClassNotFoundException, IOException {
		synchronized (file) {
			if(! file.getParentFile().exists() )
				throw new FileNotFoundException("Scores directory " + file.getParent() + " does not exist.");
			if(! file.exists()) {
				throw new FileNotFoundException("Scores file " + file + " not found.");
			} else if(file.isFile()) {
				if(file.canRead()) {
					FileInputStream in = new FileInputStream(file);
					ObjectInputStream s = new ObjectInputStream(in);
					Object o = s.readObject();
					synchronized(scores) {
						synchronized(nameToEntry) {
							if(o instanceof List) {
								List<?> al = (List<?>) o;
								for(Object i: al)
									if(i instanceof String[]) {
										String entry[] =( (String[]) i );
										if(entry.length > 2 && !entry[NAME].equals("")) {
											scores.add(entry);
											nameToEntry.put(entry[NAME], entry);
										}
									}
							} else if(o instanceof String[]) {
								boolean eof = false;
								do {
									if(o instanceof String[]) {
										String[] entry = (String[]) o;
										if(entry.length > 2 && !entry[NAME].equals("")) {
											scores.add(entry);
											nameToEntry.put(entry[NAME], entry);
										}
									}
									try {
										o = s.readObject();
									} catch (EOFException ee) {
										eof = true;
									}
								} while (! eof);
							}
							Collections.sort(scores, this);
						}
					}
					in.close();
				} else {
					throw new IOException("Can't read scores file: " + file);
				}
			} else {
				throw new IOException("Expected normal file at " + file);
			}
		}
	}
	
	public void clear() {
		synchronized(file) {
			synchronized(scores) {
				synchronized(nameToEntry) {
					scores.clear();
					nameToEntry.clear();
					if(file.exists() && file.isFile())
						file.delete();
				}
			}
		}
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
	
	public int highScore() {
		int ret = 0;
		synchronized(scores) {
			if(scores.size() != 0)
				ret = Integer.parseInt(scores.get(0)[TOTAL_SCORE]);
		}
		return ret;
	}
	
	public int playerScore(String player) {
		int ret = 0;
		synchronized(scores) {
			synchronized(nameToEntry) {
				if(nameToEntry.containsKey(player))
					ret = Integer.parseInt(nameToEntry.get(player)[TOTAL_SCORE]);
			}
		}
		return ret;
	}
	
	public String[] copyOfEntry(int index) {
		String ret[];
		synchronized(scores) {
			if(index >= 0 && scores.size() > index) {
				String entry[] = scores.get(index);
				int len = entry.length;
				ret = new String[len];
				System.arraycopy(entry, 0, ret, 0, len);
			} else {
				ret = new String[0];
			}
		}
		return ret;
	}
	
	public String[] copyOfHighScoreEntry() {
		return copyOfEntry(0);
	}

	public List<String> scoreTable() {
		IrcTablePrinter itp = new IrcTablePrinter();
		return itp.scoreTable(scores);
	}
	
	public List<String> matchScoreTable() {
		IrcTablePrinter itp = new IrcTablePrinter();
		return itp.matchScoreTable(scores);
	}
}
