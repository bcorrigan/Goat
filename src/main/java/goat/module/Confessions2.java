package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.StringUtil;
import static goat.util.Confessions.*;
import static goat.util.HTMLUtil.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;

import com.sleepycat.je.DatabaseException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;


public class Confessions2 extends Module {

	public boolean isThreadSafe() {
		return true;
	}

	private static int MAX_SEARCH_RESULTS = 16;
	private static int MAX_SEARCHES_SAVED = 64;

	private class ResultsQueue {
		private ArrayList<String> queries = new ArrayList<String>();
		private HashMap<String, ArrayList<Integer>> resultLists = new HashMap<String, ArrayList<Integer>>();

		public boolean hasNext() {
			return ! queries.isEmpty();
		}

		public Integer next() {
			// we could do this using nested method calls, it's broken up for easier debugging
			int queryIndex = queries.size() - 1;
			String  query = queries.get(queryIndex);
			ArrayList<Integer> results = resultLists.get(query);
			int resultIndex = 0;
			Integer result = results.get(resultIndex);

			results.remove(resultIndex);
			if(results.isEmpty()) {
				// housekeeping
				resultLists.remove(query);
				queries.remove(queryIndex);
			}
			return result;
		}

		public boolean hasNext(String query) {
			return queries.contains(query);
		}

		public Integer next(String query) {
			// move this query to the front of the line (i.e., the end of the ArrayList)
			queries.remove(query);
			queries.add(query);
			return next();
		}

		public void add(String query, ArrayList<Integer> results) {
			// we could add a check here to make sure we don't already have 
			//   some results for the given query, but that would be a little
			//   expensive (an ArrayList.contains() call, as currently implemented)
			//   as this class is only used internally, we'll trust ourselves to
			//   check for existing results before adding new ones.
			if(! results.isEmpty()) {
				queries.add(query);
				resultLists.put(query, results);
			}
			while(queries.size() > MAX_SEARCHES_SAVED) {
				resultLists.remove(queries.get(0));
				queries.remove(0);
			}
		}

		public int queryCount() {
			return queries.size();
		}

		public int totalResultsCount() {
			int total = 0;
			for(int i=0; i<queries.size();i++)
				total += resultLists.get(queries.get(i)).size();
			return total;
		}
	}

	private ResultsQueue resultsQueue = new ResultsQueue();

	private ArrayList<Integer> randomCache = new ArrayList<Integer>();

	public Confessions2() {
		super();
		final String confessionsFile = "resources/confessions.gz";
		try {
			if (0L == dbCount()) {
				System.out.println("Trying to import confessions from file: " + confessionsFile);
				goat.util.Confessions.importBcFile(confessionsFile);
			}
		} catch (DatabaseException dbe) {
			System.err.println("Database fuckup while trying to load confessions from file");
			dbe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("I/O error while trying to load confessions from file");
			ioe.printStackTrace();
		}
	}

	public String[] getCommands() {
		return new String[]{"confess", "confession", "guiltiness", "confesscount", "guiltfight"};
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	static final Pattern queryPattern = Pattern.compile("\\s*about\\s+(.*)");

	public void processChannelMessage(Message m) {
		try {
			if("confesscount".equalsIgnoreCase(m.getModCommand())
					|| "guiltiness".equalsIgnoreCase(m.getModCommand()))
				confessionCount(m);
			else if("guiltfight".equalsIgnoreCase(m.getModCommand()))
				guiltfight(m);
			else if("confess".equalsIgnoreCase(m.getModCommand())
					|| "confession".equalsIgnoreCase(m.getModCommand())) {
				Matcher matcher = queryPattern.matcher(m.getModTrailing());
				if(matcher.find()) {
					searchConfession(m, matcher.group(1));
				} else if(m.getModTrailing().matches("\\s*random.*")) {
					randomConfession(m);
				} else if(m.getModTrailing().toLowerCase().startsWith("stat")) {
					status(m);
				} else {
					defaultConfession(m);
				}
			} else {
				m.reply("Nobody told me what to do with that command.");
			}
		} catch (CorruptIndexException cie) {
			m.reply("I can't, my index is corrupt!");
			cie.printStackTrace();
		} catch (ParseException pe) {
			m.reply("I'm sorry?  I can't figure out what you want from me.");
			// pe.printStackTrace();
		} catch (IOException ioe) {
			m.reply("I can't right now, my IO is exceptioning.");
			ioe.printStackTrace();
		} catch (DatabaseException dbe) {
			m.reply("I'm sorry, but I'm having problems with my database.");
			dbe.printStackTrace();
		} catch (TooManyClauses tme) {
			if(m.getPrefix().trim().matches(".*\\.nyc\\.res\\.rr\\.com$"))  //TODO isQpt()
				m.reply("You can go right to hell, qpt!");
			else
				m.reply("I'm terribly sorry, but that query had way too many terms in it after I was done parsing and expanding it.");
			System.out.println("Confessions2:  search casued TooManyTerms exception in query parser:\n   " + m.getModTrailing().replaceFirst("(?i)about", ""));
		}

	}

	private void confessionCount(Message m) 
	throws ParseException, CorruptIndexException, IOException {
		String query = StringUtil.removeFormattingAndColors(m.getModTrailing()).trim();
		int count = searchCount(query);
		if(0 == count)
			m.reply("Sorry, I have no regrets about " + query + ".");
		else if(1 == count)
			m.reply("I have only one regret about " + query + ".");
		else
			m.reply("I have " + count + " regrets about " + query + ".");
	} 

	private void defaultConfession(Message m) {
		try {
			if(resultsQueue.hasNext()) {
				Confession reply = dbGet(resultsQueue.next());
				m.pagedReply(textFromHTML(reply.content).trim());
				return;
			} else {
				randomConfession(m);
			}
		} catch (DatabaseException dbe) {
			m.reply("oops, something in my database seems to be fucked up.  Sorry.");
			dbe.printStackTrace();
		}
	}

	private void randomConfession(Message m) {
		try {
			m.pagedReply(textFromHTML(getRandomConfession().content).trim());
		} catch (DatabaseException dbe) {
			m.reply("I had a database fuckup while I was trying to get a random confession");
			dbe.printStackTrace();
		}
	}

	private void status(Message m) {
		String reply = "";
		try {
			reply += "I've heard " + dbCount() + " confessions.  ";
		} catch (DatabaseException dbe) {
			reply += "I'm having a problem with my confessions filing cabinet.  ";
		}
		reply += "I have " + resultsQueue.totalResultsCount() + " search result";
		if(resultsQueue.totalResultsCount() != 1)
			reply += "s";
		reply += " for " + resultsQueue.queryCount() + " quer";
		if(resultsQueue.queryCount() != 1)
			reply += "ies";
		else
			reply += "y";
		reply += ".  ";
		reply += "The last time I heard a new confession was " + "... um, I don't remember.";
		m.pagedReply(reply);
	}

	private void searchConfession(Message m, String queryString) 
	throws ParseException, DatabaseException, CorruptIndexException, IOException {
		queryString = StringUtil.removeFormattingAndColors(queryString).trim();
		if (resultsQueue.hasNext(queryString))
			m.pagedReply(textFromHTML(dbGet(resultsQueue.next(queryString)).content));
		else {
			ArrayList<Integer> hits = searchRandomized(queryString, MAX_SEARCH_RESULTS);
			if(hits.size() == 0) {
				m.pagedReply("I just don't feel guilty about " + queryString);
			} else if(hits.size() > 1) {
				int count = searchCount(queryString);
				m.reply("I have " + count + " things to confess about " + Constants.BOLD + queryString + Constants.NORMAL + ", starting with:");
				String reply = textFromHTML(dbGet(hits.get(0)).content);
				m.pagedReply(reply);
				hits.remove(0);
				resultsQueue.add(queryString, hits);
			} else
				m.pagedReply(textFromHTML(dbGet(hits.get(0)).content));
		}
	}

	private void guiltfight(Message m) 
	throws ParseException, CorruptIndexException, IOException {
		String [] contestants = StringUtil.removeFormattingAndColors(m.getModTrailing()).split("\\s+[vV][sS]\\.?\\s+") ;
		if (contestants.length < 2) {
			m.reply("Usage:  \"guiltfight \"dirty dogs\" vs. \"fat cats\" [vs. ...]\"") ;
			return ;
		}
		for (int i = 0 ; i < contestants.length ; i++) 
			contestants[i] = contestants[i].trim() ;
		int [] scores = getResultCounts(contestants) ;
		int [] winners = getWinners(scores) ;
		switch(winners.length) {
		case 0 : // no winner
			m.reply("I don't feel guilty about any of that.") ;
			break;
		case 1 : // normal, one winner
			m.reply("I feel guiltiest about " + Constants.BOLD + contestants[winners[0]] + Constants.BOLD + ", I've got " + scores[winners[0]] + " regrets about it.") ;
			break;
		default : // tie
			String winnerString = Constants.BOLD + contestants[winners[0]] + Constants.BOLD ;
			for (int i=1 ; i < winners.length ; i++)
				winnerString += " and " + Constants.BOLD + contestants[winners[i]] + Constants.BOLD ;
			m.reply("I'm torn, I feel equally guilty about " + winnerString + ", with " + scores[winners[0]] + " regrets about each.") ;
			break;
		}
	}

	public int[] getResultCounts(String[] queries)
	throws ParseException, CorruptIndexException, IOException {
		int [] counts = new int[queries.length] ;
		for (int i = 0 ; i < queries.length; i++) {
			if (queries[i].matches("\\s*")) { // if string is empty
				counts[i] = -1 ;
			} else {
				counts[i] = searchCount(queries[i]);
			}
		}
		return counts ;
	}

	public int [] getWinners(int [] scores) {
		int[] indices = new int[scores.length] ;
		if (indices.length == 0) 
			return indices ;
		indices[0] = 0 ;
		int lastIndex = -1 ;
		for (int i = 0 ; i < scores.length ; i++ ) {
			if(scores[i] > 0)
				if (scores[i] > scores[indices[0]]) { // new high
					indices[0] = i ;
					lastIndex = 0 ;
				} else if (scores[i] == scores[indices[0]]) { // tie
					indices[++lastIndex] = i ;
				}
		}
		int [] ret = new int[lastIndex + 1] ;
		if(ret.length > 0)
			System.arraycopy(indices, 0, ret, 0, lastIndex + 1) ;
		return ret ;
	}

	private Confession getRandomConfession() throws DatabaseException {
		if(randomCache.isEmpty())
			randomCache = getRandomConfessionIds(1024);
		Confession confession = dbGet(randomCache.get(0));
		randomCache.remove(0);
		return confession;
	}
}
