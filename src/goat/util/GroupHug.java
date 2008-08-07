package goat.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.util.Date;
import java.util.Random;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;

import static goat.util.HTMLUtil.*;

public class GroupHug {
	
	@Entity
	public static class Confession {
		
		@PrimaryKey
		public Integer id;
		@SecondaryKey(relate=ONE_TO_ONE)
		public Integer node_id;
		public String content;
		
		Confession() {}
		
		Confession(Integer myId, Integer node, String text) {
			id = myId;
			node_id = node;
			content = text;
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
	
	public static long dbCount() throws DatabaseException {
		return getPrimaryIndex().count();
	}

	/*
	 * This is hideous.  The price we pay for non-sequential primary keys.
	 */
	public static Confession getRandomConfession() throws DatabaseException {
		EntityCursor<Integer> ec = getPrimaryIndex().keys();
		Random random = new Random();
		ec.first();
		// System.out.println("we've got " + getPrimaryIndex().count() + " entries");
		int entry = (int) (random.nextDouble() * getPrimaryIndex().count());
		// System.out.println("going for entry #" + entry);
		int i = 0;
		while(i < entry) {
			ec.next();
			i++;
		}
		Integer id = ec.next();
		Confession confession = dbGet(id);
		ec.close();
		return confession;
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

	public static IndexReader indexReader = null;
	
	public static Hits search(String searchString) throws CorruptIndexException, IOException, ParseException {
		if(null == indexReader)
			indexReader = IndexReader.open(INDEX_DIR);
	    Searcher searcher = new IndexSearcher(indexReader);
	    Analyzer analyzer = new StandardAnalyzer();
	    QueryParser parser = new QueryParser("content", analyzer);
	    Query query = parser.parse(searchString);
	    Hits hits = searcher.search(query);
	    // indexReader.close();
	    return hits;
	}
	
	// END search crap
	/**
	 * method to suck in all the confessions in bc's scraper-output file, giving them pseudo id and node_id
	 * 
	 * @path path to the gzipped confessions file
	 */
	public static void importBcFile(String path) throws IOException, DatabaseException {
		BufferedReader br =  new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path))));
		String line;
		int id = 0;
		String text = "";
		while((line = br.readLine()) != null) {
			if(line.startsWith("#ENDCONF")) {
				if(! dbContains(id))
					dbPut(new Confession(id, id, text));
				text = "";
				id++;
			} else {
				text += line;
			}
		}
		br.close();
	}
}
