package goat.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.Adler32;
import java.util.Date;
import java.util.Random;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;

import com.sleepycat.je.Environment;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.SecondaryIndex;

import static com.sleepycat.persist.model.Relationship.*;
import com.sleepycat.persist.EntityStore;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;

import static goat.util.HTMLUtil.*;

public class Confessions {
	
	@Entity
	public static class Confession {
		
		@PrimaryKey
		public Integer id;
		@SecondaryKey(relate=ONE_TO_ONE)
		public Integer node_id;
		@SecondaryKey(relate=MANY_TO_ONE)
		public Long added;
		@SecondaryKey(relate=ONE_TO_ONE)
		public Long checksum;
		
		public String content;
		
		Confession() {}
		
		Confession(Integer myId, Integer node, String text) {
			id = myId;
			node_id = node;
			content = text;
			added = (new Date()).getTime();
			checksum = computeChecksum(content);
		}
		
		public String toString() {
			String ls = System.getProperty("line.separator");
			return id + ls + node_id + ls + content.trim();
		}
		
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getNode_id() {
			return node_id;
		}

		public void setNode_id(Integer node_id) {
			this.node_id = node_id;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
		
		public Long getAdded() {
			return added;
		}

		public void setAdded(Long added) {
			this.added = added;
		}

		public Long getChecksum() {
			return checksum;
		}

		public void setChecksum(Long checksum) {
			this.checksum = checksum;
		}

		/*
		 * The checksum we're using is a concatination of the 32-bit java String checksum and a 32-bit Adlerian checksum
		 */
		public static Long computeChecksum(String string) {
			Adler32 checksumGenerator = new Adler32();
			checksumGenerator.update(string.getBytes());
			//return crc32.getValue();
			return (((long) string.hashCode()) << 32) | checksumGenerator.getValue();
		}
	}
	
	private static Environment environment = null;
	private static EntityStore entityStore = null;
	private static PrimaryIndex<Integer, Confession> primaryIndex = null;
	private static SecondaryIndex<Long, Integer, Confession> checksumIndex = null;
	
	private static final String STORE_NAME = "ConfessionStore" ; 
	
	private static final String DEFAULT_LOCAL_DIR = "resources" + File.separator + "confessions" + File.separator + "db";
	private static String localDir = DEFAULT_LOCAL_DIR;
	
	public static Long computeChecksum(String string) {
		return Confession.computeChecksum(string);
	}
	
	private static void initLocalDB() {
		try {
			Runtime.getRuntime().addShutdownHook(new ShutDown());
		    EnvironmentConfig envConfig = new EnvironmentConfig();
		    StoreConfig storeConfig = new StoreConfig();
		    envConfig.setAllowCreate(true);
		    storeConfig.setAllowCreate(true);
		    File dbDir = new File(localDir);
		    if(!(dbDir.exists() && dbDir.isDirectory()))
		    	dbDir.mkdirs();
		    environment = new Environment(dbDir, envConfig);
		    entityStore = new EntityStore(environment, STORE_NAME, storeConfig);
		} catch (DatabaseException dbe) {
		    dbe.printStackTrace();
		}
	}
	
	private static PrimaryIndex<Integer, Confession> getPrimaryIndex() throws DatabaseException {
		if(primaryIndex != null) {
			return primaryIndex;
		}
		if(null == environment || null == entityStore)
			initLocalDB();
		return entityStore.getPrimaryIndex(Integer.class, Confession.class);
	}
	
	private static SecondaryIndex<Long, Integer, Confession> getChecksumIndex() throws DatabaseException {
		if (null != checksumIndex)
			return checksumIndex;
		PrimaryIndex<Integer, Confession> pi = getPrimaryIndex();
		return entityStore.getSecondaryIndex(pi, Long.class, "checksum");
	}
	
	public static void dbPut(Confession confession) throws DatabaseException {
		getPrimaryIndex().put(confession);
		index(confession);
	}
	
	public static Confession dbGet(Integer id) throws DatabaseException {
		return getPrimaryIndex().get(id);
	}
	
	public static boolean dbContains(Integer id) throws DatabaseException {
		return getPrimaryIndex().contains(id);
	}
	
	public static boolean dbContainsChecksum(Long checksum) throws DatabaseException {
		return getChecksumIndex().contains(checksum);
	}
	
	public static Confession dbGetByChecksum(Long checksum) throws DatabaseException {
		return getChecksumIndex().get(checksum);
	}
	
	public static long dbCount() throws DatabaseException {
		return getPrimaryIndex().count();
	}

	private static int MAX_RANDOM = 1024;
	/*
	 * This is kinda expensive, works best when you get your random keys in bulk.
	 * The price we pay for non-sequential primary keys, eh.
	 */
	public static ArrayList<Integer> getRandomConfessionIds(int num) throws DatabaseException {
		if(num > MAX_RANDOM)
			num = MAX_RANDOM;
		if(num < 0)
			num = 0;
		ArrayList<Integer> ids = new ArrayList<Integer>(num);
		EntityCursor<Integer> ec = getPrimaryIndex().keys();
		Random random = new Random();
		
		Long numRecords = getPrimaryIndex().count();
		int i = 0;
		while(i++ < num)
			ids.add((int) (random.nextDouble() * numRecords));
		Collections.sort(ids);
		ec.first();
		int here = 0;
		i = 0;
		Iterator<Integer> it = ec.iterator();
		while(it.hasNext()) {
			if(ids.get(i) == here) {
				ids.remove(i);
				ids.add(i, it.next());
				i++;
				if(i >= num)
					break;
			} else {
				it.next();
			}
			here++;
		}
		ec.close();
		
		// randomize the return ArrayList, remember we sorted ids for the sake of one-pass access
		Collections.shuffle(ids);
		return ids;
	}
	
	/**
	 * Ugly expose-the-plumbing way to get at all db entries, because "bdb java edition" just has to be a bitch that way.
	 * 
	 * If you don't call "close()" on the EntityCursor you get here, you'll fuck everything up.  Sleepycat loves you.
	 * 
	 * CLOSE YOUR CURSOR WHEN YOU'RE DONE.
	 * 
	 * @return EntityCursor for the entire db.  call iterator() on it to plough through the whole db.
	 * 
	 * @throws DatabaseException
	 * 
	 * @see com.sleepycat.persist.EntityCursor
	 */
	public static EntityCursor<Confession> dbEntityCursor() throws DatabaseException {
		return getPrimaryIndex().entities();
	}
	
	static class ShutDown extends Thread {
		public ShutDown() {}
		public void run() {
			try {
				if(entityStore != null)
					entityStore.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if(environment != null)
					environment.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// END db crap
	
	// BEGIN search crap
	
	public static final String INDEX_DIR = "resources" + File.separator + "confessions" + File.separator + "index";
	public static IndexWriter indexWriter = null;
	
	public static Document Document(Confession confession) {
		Document doc = new Document();
		doc.add(new Field("id", confession.id.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field("node_id", confession.node_id.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field("time",
				DateTools.timeToString(new Date().getTime(), DateTools.Resolution.SECOND),
				Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field("content", new StringReader(textFromHTML(confession.content).trim())));
		return doc;
	}
	
	public static void index(Confession confession) {
		try {
			if(null == indexWriter)
				indexWriter = new IndexWriter(INDEX_DIR, new StandardAnalyzer());
			indexWriter.addDocument(Document(confession));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// might want to consider capping # of search results returned
	public static ArrayList<Integer> search(String searchString, int maxResults) throws CorruptIndexException, IOException, ParseException, TooManyClauses {
		IndexReader indexReader = IndexReader.open(INDEX_DIR);
	    Searcher searcher = new IndexSearcher(indexReader);
	    Analyzer analyzer = new StandardAnalyzer();
	    QueryParser parser = new QueryParser("content", analyzer);
	    parser.setDefaultOperator(QueryParser.AND_OPERATOR);
	    Query query = parser.parse(searchString);
	    Hits hits = searcher.search(query);
	    ArrayList<Integer> ret = new ArrayList<Integer>();
	    Iterator<Hit> it = hits.iterator();
	    if(maxResults <= 0)
	    	while(it.hasNext())
	    		ret.add(Integer.parseInt(it.next().get("id")));
	    else
	    	while(it.hasNext() && maxResults-- > 0) {
	    		ret.add(Integer.parseInt(it.next().get("id")));
	    	}
	    indexReader.close();
	    return ret;
	}
	
	public static ArrayList<Integer> search(String searchString) throws CorruptIndexException, IOException, ParseException, TooManyClauses {
		return search(searchString, 0);
	}
	
	public static ArrayList<Integer> searchRandomized(String searchString, int maxResults) throws CorruptIndexException, IOException, ParseException, TooManyClauses {
		IndexReader indexReader = IndexReader.open(INDEX_DIR);
	    Searcher searcher = new IndexSearcher(indexReader);
	    Analyzer analyzer = new StandardAnalyzer();
	    QueryParser parser = new QueryParser("content", analyzer);
	    parser.setDefaultOperator(QueryParser.AND_OPERATOR);
	    Query query = parser.parse(searchString);
	    Hits hits = searcher.search(query);
	    ArrayList<Integer> ret = new ArrayList<Integer>();
	    Iterator<Hit> it = hits.iterator();
	     
	    int i = 0;
	    // take the first n search results, where n <= maxresults, and fill our return set with those initially
	    while(it.hasNext() && i++ < maxResults)
	    	ret.add(Integer.parseInt(it.next().get("id")));

	    if(it.hasNext()) {
	    	// if there are still search hits that aren't in our results set, go through the results set
	    	// one element at a time, picking a search hit at random and overwriting the result element
	    	// with the search hit if our results set does not already contain that search hit.
	    	//
	    	// this might not produce a perfectly random results set, but if not, the randomness
	    	// is still going to be pretty good.
	    	i = 0;
	    	int id;
	    	Random random = new Random();
	    	for (i=0; i < maxResults; i++) {
	    		id = Integer.parseInt(hits.doc(random.nextInt(hits.length())).get("id"));
	    		if(! ret.contains(id))
	    			ret.set(i, id);
	    	}
	    }
	    // make sure the order of results is random 
	    //  (it won't be at this point, if there were fewer search hits than maxResults)
	    Collections.shuffle(ret);
	    return ret;
	}
	
	public static int searchCount(String searchString) throws CorruptIndexException, IOException, ParseException, TooManyClauses {
		IndexReader indexReader = IndexReader.open(INDEX_DIR);
	    Searcher searcher = new IndexSearcher(indexReader);
	    Analyzer analyzer = new StandardAnalyzer();
	    QueryParser parser = new QueryParser("content", analyzer);
	    parser.setDefaultOperator(QueryParser.AND_OPERATOR);
	    Query query = parser.parse(searchString);
	    Hits hits = searcher.search(query);
	    return hits.length();
	}
	
	// END search crap
	
	/**
	 * method to suck in all the confessions in bc's scraper-output file, giving them pseudo id and node_id
	 * 
	 * @path path to the gzipped confessions file
	 */
	public static void importBcFile(String path) throws IOException, DatabaseException {
		System.out.println("Importing confessions from bc-format gzipped file...");
		System.out.print("   ");
		final int dotIncrement = 1000;
		final int countIncrement = 10000;
		final int outWrap = 72;
		int outLength = 3;
		BufferedReader br =  new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
		String line;
		int id = 1;
		String text = "";
		int dupes = 0;
		int falseDupes = 0;
		
		while((line = br.readLine()) != null) {
			if(line.startsWith("#ENDCONF")) {
				if(! dbContains(id)) {
					Long checksum = computeChecksum(text);
					if (! dbContainsChecksum(checksum))
						dbPut(new Confession(id, id, text));
					else {
						dupes++;
						Confession conf = dbGetByChecksum(checksum);
						if(! text.equals(conf.content)) {
							falseDupes++;
							System.out.println("\nfalse match:\n   \"" + text + "\"\n   \"" + conf.content + "\"");
						}
					}
				}
				if(0 == id % dotIncrement) {
					System.out.print(".");
					outLength++;
				}
				if(0 == id % countIncrement) {
					System.out.print(id);
					outLength += String.valueOf(id).length();
				}
				if(outLength > outWrap) {
					System.out.print("\n   ");
					outLength = 0;
				}
				text = "";
				id++;
			} else {
				text += line;
			}
		}
		System.out.println();
		br.close();
		System.out.println("\n" + dupes + " dupes found in file");
		System.out.println("   " + falseDupes + " were false matches");
	}
	
	
	

}
