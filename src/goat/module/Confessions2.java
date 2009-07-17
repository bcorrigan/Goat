package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.StringUtil;
import goojax.search.SearchResponse;
import goojax.search.web.WebSearcher;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import static goat.util.Confessions.*;
import static goat.util.HTMLUtil.*;
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
				m.createReply("Nobody told me what to do with that command.").send();
			}
		} catch (CorruptIndexException cie) {
			m.createReply("I can't, my index is corrupt!").send();
			cie.printStackTrace();
		} catch (ParseException pe) {
			m.createReply("I'm sorry?  I can't figure out what you want from me.").send();
			// pe.printStackTrace();
		} catch (IOException ioe) {
			m.createReply("I can't right now, my IO is exceptioning.").send();
			ioe.printStackTrace();
		} catch (DatabaseException dbe) {
			m.createReply("I'm sorry, but I'm having problems with my database.").send();
			dbe.printStackTrace();
		} catch (TooManyClauses tme) {
			if(m.getPrefix().trim().matches(".*\\.nyc\\.res\\.rr\\.com$"))  //TODO isQpt()
				m.createReply("You can go right to hell, qpt!").send();
			else
				m.createReply("I'm terribly sorry, but that query had way too many terms in it after I was done parsing and expanding it.").send();
			System.out.println("Confessions2:  search casued TooManyTerms exception in query parser:\n   " + m.getModTrailing().replaceFirst("(?i)about", ""));
		}

	}

	private void confessionCount(Message m) 
	throws ParseException, CorruptIndexException, IOException {
		String query = StringUtil.removeFormattingAndColors(m.getModTrailing()).trim();
		int count = searchCount(query);
		if(0 == count)
			m.createReply("Sorry, I have no regrets about " + query + ".").send();
		else if(1 == count)
			m.createReply("I have only one regret about " + query + ".").send();
		else
			m.createReply("I have " + count + " regrets about " + query + ".").send();
	} 

	private void defaultConfession(Message m) {
		try {
			if(resultsQueue.hasNext()) {
				Confession reply = dbGet(resultsQueue.next());
				m.createPagedReply(textFromHTML(reply.content).trim()).send();
				return;
			} else {
				randomConfession(m);
			}
		} catch (DatabaseException dbe) {
			m.createReply("oops, something in my database seems to be fucked up.  Sorry.").send();
			dbe.printStackTrace();
		}
	}

	private void randomConfession(Message m) {
		try {
			m.createPagedReply(textFromHTML(getRandomConfession().content).trim()).send();
		} catch (DatabaseException dbe) {
			m.createReply("I had a database fuckup while I was trying to get a random confession").send();
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
		m.createPagedReply(reply).send();
	}

	private void searchConfession(Message m, String queryString) 
	throws ParseException, DatabaseException, CorruptIndexException, IOException {
		queryString = StringUtil.removeFormattingAndColors(queryString).trim();
		if (resultsQueue.hasNext(queryString))
			m.createPagedReply(textFromHTML(dbGet(resultsQueue.next(queryString)).content)).send();
		else {
			ArrayList<Integer> hits = searchRandomized(queryString, MAX_SEARCH_RESULTS);
			if(hits.size() == 0) {
				m.createPagedReply("I just don't feel guilty about " + queryString).send();
			} else if(hits.size() > 1) {
				int count = searchCount(queryString);
				m.createReply("I have " + count + " things to confess about " + Constants.BOLD + queryString + Constants.NORMAL + ", starting with:").send();
				String reply = textFromHTML(dbGet(hits.get(0)).content);
				m.createPagedReply(reply).send();
				hits.remove(0);
				resultsQueue.add(queryString, hits);
			} else
				m.createPagedReply(textFromHTML(dbGet(hits.get(0)).content)).send();
		}
	}

	private void guiltfight(Message m) 
	throws ParseException, CorruptIndexException, IOException {
		String [] contestants = StringUtil.removeFormattingAndColors(m.getModTrailing()).split("\\s+[vV][sS]\\.?\\s+") ;
		if (contestants.length < 2) {
			m.createReply("Usage:  \"guiltfight \"dirty dogs\" vs. \"fat cats\" [vs. ...]\"").send() ;
			return ;
		}
		for (int i = 0 ; i < contestants.length ; i++) 
			contestants[i] = contestants[i].trim() ;
		int [] scores = getResultCounts(contestants) ;
		int [] winners = getWinners(scores) ;
		switch(winners.length) {
		case 0 : // no winner
			m.createReply("I don't feel guilty about any of that.").send() ;
			break;
		case 1 : // normal, one winner
			m.createReply("I feel guiltiest about " + Constants.BOLD + contestants[winners[0]] + Constants.BOLD + ", I've got " + scores[winners[0]] + " regrets about it.").send() ;
			break;
		default : // tie
			String winnerString = Constants.BOLD + contestants[winners[0]] + Constants.BOLD ;
			for (int i=1 ; i < winners.length ; i++)
				winnerString += " and " + Constants.BOLD + contestants[winners[i]] + Constants.BOLD ;
			m.createReply("I'm torn, I feel equally guilty about " + winnerString + ", with " + scores[winners[0]] + " regrets about each.").send() ;
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

