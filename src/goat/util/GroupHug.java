package goat.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Adler32;
import java.util.Date;
import java.util.Random;
//import java.util.Arrays;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

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

public class GroupHug {
	
	@Entity
	public static class Confession {
		
		@PrimaryKey
		public Integer id;
		@SecondaryKey(relate=ONE_TO_ONE)
		public Integer node_id;
		@SecondaryKey(relate=MANY_TO_ONE)
		public Long added;
		@SecondaryKey(relate=ONE_TO_ONE)
		public Long adler32;
		
		public String content;
		
		Confession() {}
		
		Confession(Integer myId, Integer node, String text) {
			id = myId;
			node_id = node;
			content = text;
			added = (new Date()).getTime();
			Adler32 ad = new Adler32();
			ad.update(content.getBytes());
			adler32 = ad.getValue();
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

		public Long getAdler32() {
			return adler32;
		}

		public void setAdler32(Long adler32) {
			this.adler32 = adler32;
		}

	}
	
	private static final String NEW_CONFESSIONS_URL = "http://beta.grouphug.us/confessions/new";
	
	public static URL confessionsPage(int pageNumber) throws MalformedURLException {
		return new URL(NEW_CONFESSIONS_URL + "?page=" + pageNumber);
	}
	
	public static int connectTimeout = 5000; // five seconds
	public static int readTimeout = 10000; // ten seconds

	public static ArrayList<Confession> getConfessionsFromPageNumber (int pageNumber) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		return scrapePage(getBufferedReaderForPageNumber(pageNumber));
	}
	
	/**
	 * Gets the confessions from the specified grouphug.us page
	 * 
	 * @param pageNumber The number of the page in grouphug's "new confessions" listing to grab from.
	 * @return
	 * @throws SocketTimeoutException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static BufferedReader getBufferedReaderForPageNumber(int pageNumber) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) confessionsPage(pageNumber).openConnection();
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			//TODO anything?
			System.out.println("grouphug problem:  " + "HTTP " + connection.getResponseCode() + ", " + connection.getResponseMessage());
		}
		return new BufferedReader(new InputStreamReader(connection.getInputStream()));
	}
	
	public static ArrayList<Confession> scrapePage (BufferedReader br) throws IOException {
		// this is ugly.  life is like that, sometimes.
		ArrayList<Confession> ret = new ArrayList<Confession>();
		String line = "";
		Pattern commentStart = Pattern.compile(".*<div id=\"node-(\\d+)\".*");
		Pattern idLine = Pattern.compile(".*<a href=\"/confessions/(\\d+)\".*");
		Pattern contentStart = Pattern.compile("\\s*<div class=\"content\">\\s*");
		Pattern contentEnd = Pattern.compile("\\s*</div>\\s*");
		Matcher matcher;
		while((line = br.readLine()) != null) {
			matcher = commentStart.matcher(line);
			if(matcher.find()) {
				int nodeId = Integer.parseInt(matcher.group(1));
				while((line = br.readLine()) != null) {
					matcher = idLine.matcher(line);
					if(matcher.find()) {
						int id = Integer.parseInt(matcher.group(1));
						while((line = br.readLine()) != null) {
							matcher = contentStart.matcher(line);
							if(matcher.find()) {
								String content = "";
								while((line = br.readLine()) != null) {
									matcher = contentEnd.matcher(line);
									if(matcher.find()) {
										ret.add(new Confession(id, nodeId, content));
										break;
									} else {
										content += line;
									}
								}
								break;
							}
						}
						break;
					}
				}
			}
		}
		return ret;
	}
	
	
	// START db crap
	
	private static Environment environment = null;
	private static EntityStore entityStore = null;
	private static PrimaryIndex<Integer, Confession> primaryIndex = null;
	private static SecondaryIndex<Long, Integer, Confession> adler32Index = null;
	
	private static final String STORE_NAME = "ConfessionStore" ; 
	
	private static final String DEFAULT_LOCAL_DIR = "resources" + File.separator + "confessions" + File.separator + "db";
	private static String localDir = DEFAULT_LOCAL_DIR;
	
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
	
	private static SecondaryIndex<Long, Integer, Confession> getAdler32Index() throws DatabaseException {
		if (null != adler32Index)
			return adler32Index;
		PrimaryIndex<Integer, Confession> pi = getPrimaryIndex();
		return entityStore.getSecondaryIndex(pi, Long.class, "adler32");
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
	
	public static boolean dbContainsAdler32(Long adler32) throws DatabaseException {
//		if(null == environment || null == entityStore)
//			initLocalDB();
//		PrimaryIndex<Integer, Confession> pi = getPrimaryIndex();
//		SecondaryIndex<Long, Integer, Confession> si = entityStore.getSecondaryIndex(pi, Long.class, "adler32");
		return getAdler32Index().contains(adler32);
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
		Adler32 ad32 = new Adler32();
		
		while((line = br.readLine()) != null) {
			if(line.startsWith("#ENDCONF")) {
				ad32.reset();
				ad32.update(text.getBytes());
				if(! dbContains(id))
					if (! dbContainsAdler32(ad32.getValue()))
						dbPut(new Confession(id, id, text));
					else
						dupes++;
				if(0 == id % dotIncrement) {
					System.out.print(".");
					outLength++;
				}
				if(0 == id % countIncrement) {
					System.out.print(id);
					outLength += String.valueOf(id).length();
				}
				if(outLength > outWrap) {
					System.out.println();
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
	}
	
	
	
	// BEGIN confessions extractor mayhem
	
	private static final int NUM_COLLECTORS = 6;
	private static ExecutorService pool = Executors.newFixedThreadPool(NUM_COLLECTORS);
}
