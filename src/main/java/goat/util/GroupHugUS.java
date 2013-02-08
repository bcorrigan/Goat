package goat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static goat.util.Confessions.*;
import com.sleepycat.je.DatabaseException;

public class GroupHugUS {

	private static final String CONFESSIONS_URL = "http://grouphug.us/frontpage";
	private static final String NEW_CONFESSIONS_URL = "http://grouphug.us/confessions/new";
	public static int connectTimeout = 5000; // five seconds
	public static int readTimeout = 10000; // ten seconds
	
	private static String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9) Gecko/2008052906 Firefox/3.0"; //Firefox 3.0 on windows
	
	private static ArrayList<Confession> scrapePage (BufferedReader br) throws IOException {
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

	public static ArrayList<Confession> getNewConfessionsFromPageNumber (int pageNumber) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		return scrapePage(getBufferedReaderForPageNumber(pageNumber, true));
	}

	public static ArrayList<Confession> getPromotedConfessionsFromPageNumber (int pageNumber) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		return scrapePage(getBufferedReaderForPageNumber(pageNumber, false));
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
	private static BufferedReader getBufferedReaderForPageNumber(int pageNumber, boolean newConfessions) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		URL page;
		if(newConfessions)
			page = getNewConfessionsURL(pageNumber);
		else
			page = getPromotedConfessionsURL(pageNumber);
		HttpURLConnection connection = (HttpURLConnection) page.openConnection();
		connection.setRequestProperty("User-agent", USER_AGENT);
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			//TODO anything?
			System.out.println("grouphug problem:  " + "HTTP " + connection.getResponseCode() + ", " + connection.getResponseMessage());
		}
		return new BufferedReader(new InputStreamReader(connection.getInputStream()));
	}

	private static URL getNewConfessionsURL(int pageNumber) throws MalformedURLException {
		return confessionsURL(pageNumber, NEW_CONFESSIONS_URL);
	}
	
	private static URL getPromotedConfessionsURL(int pageNumber) throws MalformedURLException {
		return confessionsURL(pageNumber, CONFESSIONS_URL);
	}

	private static URL confessionsURL(int pageNumber, String baseURL) throws MalformedURLException {
		return new URL(baseURL + "?page=" + pageNumber);
	}
	
	public static int getLastPromotedPageNumber() throws SocketTimeoutException, MalformedURLException, IOException {
		return getLastPageNumber(false);
	}
	
	public static int getLastNewPageNumber() throws SocketTimeoutException, MalformedURLException, IOException {
		return getLastPageNumber(true);
	}
	
	private static int getLastPageNumber(boolean newConfessions) throws SocketTimeoutException, MalformedURLException, IOException {
		BufferedReader br = getBufferedReaderForPageNumber(0, newConfessions);
		int ret = -1;
		String line;
		Pattern pat = Pattern.compile("<a href=\"/confessions/new?page=(\\d+)\"[^>]*>last.*</a>");
		Matcher matcher;
		while(null != (line = br.readLine())) {
			matcher = pat.matcher(line);
			if(matcher.find()) {
				ret = Integer.parseInt(matcher.group(1));
			}
		}
		return ret;
	}
	
	// BEGIN threads of fury
	
	private static final int NUM_EXTRACTORS = 6;
	private static ExecutorService pool = Executors.newFixedThreadPool(NUM_EXTRACTORS + 1);
	
	private static final long DATE_FUDGING_PAGE_OFFSET_MILLIS = 1000;
	private static final long DATE_FUDGING_CONFESSION_OFFSET_MILLIS = 10;
	private static final long DATE_FUDGING_NEW_CONFESSIONS_OFFSET_MILLIS = DATE_FUDGING_PAGE_OFFSET_MILLIS * 2; // because there are twice as many new confessions per page as promoted confessions.  go figure.
	private static final long DATE_FUDGING_NEW_CONFESSIONS_SHIFT_MILLIS = DATE_FUDGING_PAGE_OFFSET_MILLIS / 2; // to avoid collisions
	
	private class ExtractionMaster implements Runnable {
		private int pStartPage = 0;
		private int pEndPage = 0;
		private int nStartPage = 0;
		private int nEndPage = 0;
		
		public ExtractionMaster(int pstart, int pend, int nstart, int nend) {
			this.pStartPage = pstart;
			this.pEndPage = pend;
			this.nStartPage = nstart;
			this.nEndPage = nend;
		}
		
		public void run() {
			// figure out how many pages to get
			// spool up the extractors and let 'em go
			// wait
			// save timestamp
			// dump db to file?
		}
	}
	
	private class ConfessionExtractor implements Runnable {
		private Integer pageNum = -1;
		private boolean newConfessions = false;
		private Long startDate;
		private static final int MAX_RETRY_WAIT = 1000 * 60 * 60;  // 1 hour
		
		public ConfessionExtractor(Integer pageNum, boolean newConfessions, Long startDate) {
			this.pageNum = pageNum;
			this.newConfessions = newConfessions;
			this.startDate = startDate;
		}
		
		public void run() {
			boolean retry = true;
			int retry_wait = 125;
			ArrayList<Confession> extractedConfessions = new ArrayList<Confession>();
			while(retry) {
				try {
					extractedConfessions = scrapePage(getBufferedReaderForPageNumber(pageNum, newConfessions));
					retry = false;
				} catch (SocketTimeoutException ste) {
					if(retry_wait == MAX_RETRY_WAIT) {
						String type = "";
						if(newConfessions) type = "new"; else type = "promoted";
						System.out.println("retry timeout limit reached while trying to get page " + pageNum + " of " + type + " confessions.\n    Will retry in " + MAX_RETRY_WAIT + "ms.");
					}
					retry_wait = increaseRetryWait(retry_wait);
				} catch (MalformedURLException mue) {
					// we should never get this...
					mue.printStackTrace();
					System.out.println("Your code is messed up.  Go fix it.");
					return;
				} catch (IOException ioe) {
					System.out.println("Communications problem with grouphug.us, retrying.");
					retry_wait = increaseRetryWait(retry_wait);
				} finally {
					//nothing
				}
				if(retry)
					try {
						Thread.sleep(retry_wait);
					} catch (InterruptedException ie) {
						//nothing
					}
			}
			addConfessionsToDb(extractedConfessions);
		}

		private void addConfessionsToDb(ArrayList<Confession> confessions) {
			Confession confession;
			for(int i=0; i < confessions.size(); i++) {
				confession = confessions.get(i);
				confession.setAdded(fudgeDate(startDate, pageNum, i, newConfessions));
				try {
					if(dbContains(confession.id))
						continue;
					else if(dbContainsChecksum(confession.getChecksum())) {
						Confession existing = dbGetByChecksum(confession.getChecksum());
						if(existing.getContent().equals(confession.getContent())) {
							//TODO dupe at grouphug, save the node id somewhere?
						} else {
							//checksum collision, complain
							System.out.println("Two confessions have the same checksum!  Not adding new to db.\n   old:   " + existing.getContent() + "\n   new:   " + confession.getContent());
						}
						continue;
					}
				} catch (DatabaseException dbe) {
					System.out.println("Database fuckup while trying to import confessions.");
					dbe.printStackTrace();
					//TODO decide what to do, here
				}
			}
		}
		
		private long fudgeDate(long startDate, int pageNum, int confessionNum, boolean newConfessions) {
			long offset = 0;
			if(newConfessions)
				offset = pageNum * DATE_FUDGING_NEW_CONFESSIONS_OFFSET_MILLIS + DATE_FUDGING_NEW_CONFESSIONS_SHIFT_MILLIS;
			else
				offset = pageNum * DATE_FUDGING_PAGE_OFFSET_MILLIS;
			offset += confessionNum * DATE_FUDGING_CONFESSION_OFFSET_MILLIS;
			return startDate - offset;
		}
		
		private int increaseRetryWait(int currentWait) {
			if(currentWait < MAX_RETRY_WAIT) {
				currentWait *= 2;
				if(currentWait > MAX_RETRY_WAIT)
					currentWait = MAX_RETRY_WAIT;
			}
			return currentWait;
		}
		
	}
	
	

}
