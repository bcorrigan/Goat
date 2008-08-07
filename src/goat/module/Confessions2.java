package goat.module;

import goat.core.Message;
import goat.core.Module;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import static goat.util.GroupHug.*;
import static goat.util.HTMLUtil.*;
import com.sleepycat.je.DatabaseException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.HitIterator;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.document.Document;

public class Confessions2 extends Module {
	
	private ArrayList<Confession> cache = new ArrayList<Confession>();
	
	private static int MAX_SEARCH_RESULTS = 100;
	private static int MAX_CACHE_SIZE = 200;
	
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
		} else {
			randomConfession(m);
		}
	}
	
	private void randomConfession(Message m) {
		if(! cache.isEmpty()) {
			Confession reply = cache.get(0);
			m.createPagedReply(textFromHTML(reply.content).trim()).send();
			cache.remove(0);
			return;
		}
		try {
			m.createPagedReply(textFromHTML(getRandomConfession().content).trim()).send();
		} catch (DatabaseException dbe) {
			m.createReply("oops, something in my database seems to be fucked up.  Sorry.").send();
			dbe.printStackTrace();
		}
	}
	
	private void searchConfession(Message m, String queryString) {
		queryString = Message.removeFormattingAndColors(queryString).trim();
		try {
			Hits hits = search(queryString);
			if(hits.length() == 0) {
				m.createPagedReply("I don't feel at all guilty about " + queryString).send();
				return;
			} else if(hits.length() > 1) {
				m.createPagedReply("I have " + hits.length() + " things to confess about " + queryString).send();
			}
			
			Iterator<Hit> it = hits.iterator();
			Confession reply = dbGet(Integer.parseInt(it.next().get("id")));
			m.createPagedReply(textFromHTML(reply.content).trim()).send();
			int i = 0;
			while(it.hasNext() && i < MAX_SEARCH_RESULTS) {
				cache.add(i, dbGet(Integer.parseInt(it.next().get("id"))));
				i++;
			}
			pruneCache();
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
		}
	}
	
	private void pruneCache() {
		while(cache.size() > MAX_CACHE_SIZE)
			cache.remove(cache.size() - 1);
	}
}

