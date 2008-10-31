package goat.module;

import goat.core.Message;
import goat.core.Module;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import static goat.util.Confessions.*;
import static goat.util.HTMLUtil.*;
import com.sleepycat.je.DatabaseException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;


public class Confessions2 extends Module {
	
	
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
	
	Confessions2() {
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
	 
	public static String[] getCommands() {
		return new String[]{"confess", "confession"};
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	static final Pattern queryPattern = Pattern.compile("\\s*about\\s+(.*)");
	
	public void processChannelMessage(Message m) {
		Matcher matcher = queryPattern.matcher(m.modTrailing);
		if(matcher.find()) {
			searchConfession(m, matcher.group(1));
		} else if(m.modTrailing.matches("\\s*random.*")) {
			randomConfession(m);
		} else if(m.modTrailing.toLowerCase().startsWith("stat")) {
			status(m);
		} else {
			defaultConfession(m);
		}
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
	
	private void searchConfession(Message m, String queryString) {
		queryString = Message.removeFormattingAndColors(queryString).trim();
		
		try {
			if (resultsQueue.hasNext(queryString))
				m.createPagedReply(textFromHTML(dbGet(resultsQueue.next(queryString)).content)).send();
			else {
				ArrayList<Integer> hits = searchRandomized(queryString, MAX_SEARCH_RESULTS);
				if(hits.size() == 0) {
					m.createPagedReply("I just don't feel guilty about " + queryString).send();
				} else if(hits.size() > 1) {
					int count = searchCount(queryString);
					m.createReply("I have " + count + " things to confess about " + Message.BOLD + queryString + Message.NORMAL + ", starting with:").send();
					String reply = textFromHTML(dbGet(hits.get(0)).content);
					m.createPagedReply(reply).send();
					hits.remove(0);
					resultsQueue.add(queryString, hits);
				} else
					m.createPagedReply(textFromHTML(dbGet(hits.get(0)).content)).send();
			}	
		} catch (CorruptIndexException cie) {
			m.createReply("I can't, my index is corrupt!").send();
			cie.printStackTrace();
		} catch (ParseException pe) {
			m.createReply("I'm sorry?  I can't figure out what you want me to confess about.").send();
			// pe.printStackTrace();
		} catch (IOException ioe) {
			m.createReply("I can't right now, my IO is exceptioning.").send();
			ioe.printStackTrace();
		} catch (DatabaseException dbe) {
			m.createReply("I'm sorry, but I'm having problems with my database.").send();
			dbe.printStackTrace();
		} catch (TooManyClauses tme) {
			if(m.prefix.trim().matches(".*\\.nyc\\.res\\.rr\\.com$"))  //TODO isQpt()
				m.createReply("You can go right to hell, qpt!").send();
			else
				m.createReply("I'm terribly sorry, but that query had way too many terms in it after I was done parsing and expanding it.").send();
			System.out.println("Confessions2:  search casued TooManyTerms exception in query parser:\n   " + m.modTrailing.replaceFirst("(?i)about", ""));
		}
	}
	
	private Confession getRandomConfession() throws DatabaseException {
		if(randomCache.isEmpty())
			randomCache = getRandomConfessionIds(1024);
		Confession confession = dbGet(randomCache.get(0));
		randomCache.remove(0);
		return confession;
	}
}

