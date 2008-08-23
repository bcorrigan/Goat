package goat.module;

import goat.core.Message;
import goat.core.Module;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import static goat.util.GroupHug.*;
import static goat.util.HTMLUtil.*;
import com.sleepycat.je.DatabaseException;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;


public class Confessions2 extends Module {
	
	private static int MAX_CACHE_SIZE = 1024;
	
	private class Cache {
		
		//note - the Cache works mainly as a stack, with the top at the end of the arraylist.
		
		private ArrayList<Integer> idCache = new ArrayList<Integer>();
		private HashMap<Integer, Integer> idToIndex = new HashMap<Integer, Integer>();
		
		public boolean hasNext() {
			return ! idCache.isEmpty();
		}
		
		public Integer next() {
			Integer ret = null;
			if (! idCache.isEmpty()) {
				int index = idCache.size() - 1;
				ret = idCache.get(index);
				idCache.remove(index);
				idToIndex.remove(ret);
			}
			return ret;
		}
		
		public void remove(Integer id) {
			if (! idToIndex.isEmpty() && idToIndex.containsKey(id)) {
				int index = idToIndex.get(id);
				idCache.remove(index);
				idToIndex.remove(id);
				for(int i = index; i < idCache.size(); i++)
					idToIndex.put(idCache.get(i), i);
			}
		}
		
		public void add(Integer id) {
			idCache.add(id);
			idToIndex.put(id, idCache.size() - 1);
			prune(MAX_CACHE_SIZE / 20);
		}
		
		public void add(ArrayList<Integer> ids) {
			for(int i = ids.size() - 1; i >= 0; i--) {
				add(ids.get(i));
			}
			prune();
		}
		
		public boolean contains(Integer id) {
			return idToIndex.containsKey(id);
		}
		
		public int getIndex(Integer id) {
			int ret = -1;
			if(idToIndex.containsKey(id))
				ret = idToIndex.get(id);
			return ret;
		}
		
		public int getId(Integer index) {
			return idCache.get(index);
		}
		
		private void prune() {
			if(idCache.size() > MAX_CACHE_SIZE) {
				int removeNum = idCache.size() - MAX_CACHE_SIZE;
				for(int i = 0; i < removeNum; i++) {
					idToIndex.remove(idCache.get(0));
					idCache.remove(0);
				}
				for(int i = 0; i < idCache.size(); i++)
					idToIndex.put(idCache.get(i), i);
			}
		}
		
		private void prune(int overflow) {
			if(idCache.size() > MAX_CACHE_SIZE + overflow)
				prune();
		}
		
	}
	
	private Cache cache = new Cache();
	
	private ArrayList<Integer> randomCache = new ArrayList<Integer>();
	
	private static int MAX_SEARCH_RESULTS = 64;
	
	public static String[] getCommands() {
		return new String[]{"confess"};
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
		} else {
			defaultConfession(m);
		}
	}
	
	private void defaultConfession(Message m) {
		try {
			if(cache.hasNext()) {
				Confession reply = dbGet(cache.next());
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
	
	private void searchConfession(Message m, String queryString) {
		queryString = Message.removeFormattingAndColors(queryString).trim();
		try {
			ArrayList<Integer> hits = searchRandomized(queryString, MAX_SEARCH_RESULTS);
			Integer hitCount = 0;
			if(hits.size() == 0) {
				m.createPagedReply("I don't feel at all guilty about " + queryString).send();
				return;
			} else if(hits.size() > 1) {
				ArrayList<Integer> allHits = search(queryString);  // so didn't want to do this... potentially a 400,000+ element ArrayList
				Iterator<Integer> hitIt = allHits.iterator();
				int id;
				int highestCacheIndex = -1;
				while(hitIt.hasNext()) {
					id = hitIt.next();
					if(cache.contains(id)) {
						int index = cache.getIndex(id);
						if (index > highestCacheIndex)
							highestCacheIndex = index;
					}
				}
				if(highestCacheIndex >= 0) {
						m.createPagedReply(textFromHTML(dbGet(highestCacheIndex).content).trim()).send();
						cache.remove(cache.getId(highestCacheIndex));
						return;
				}
				hitCount = searchCount(queryString);
				m.createPagedReply("I have " + hitCount + " things to confess about " + Message.BOLD + queryString + Message.NORMAL + ", starting with:").send() ;
			}
			
			Confession reply = dbGet(hits.get(0));
			m.createPagedReply(textFromHTML(reply.content).trim()).send();
			hits.remove(0);
			if(hits.size() > 0)
				cache.add(hits);
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

