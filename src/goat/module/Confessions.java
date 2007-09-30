package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.ListIterator;


/**
 * @author <p><b>Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class Confessions extends Module {

	private LinkedList<String> confessions = new LinkedList<String>();
	//Document document;
    private int noConfessions = 0;
	private int hits = 0;
	public Confessions() {
		//debug
		System.out.println("loading Confessions module...");
		getConfessions(null);
	}


	//TODO Just realised the page is an xml page so it'd prolly be a lot better to just get the info using a simple xml decoder or something
	private boolean getConfessions(Message m) {
		boolean ret = false;
        HttpURLConnection connection = null;
		try {
			URL grouphug = new URL("http://grouphug.us/random");
			connection = (HttpURLConnection) grouphug.openConnection();
			connection.setConnectTimeout(3000);  // just 3 seconds, we can't hang around
			connection.setReadTimeout(10000); // ten seconds, give it a little longer to actually get the page once connected
			confessions = parseConfession(connection);
			if (confessions.isEmpty()) {
				// why we risk a recursion blowout here, I'm not sure.
				//	getConfessions(m);
			} else {
				ret = true;
			}
		} catch (SocketTimeoutException e) {
			if (null != m)
				m.createReply("Timed out trying to extract confessions").send() ;
			else
				System.out.println("Timed out trying to extract confessions");
			// e.printStackTrace() ;
		} catch (IOException e) {
			if (m != null)
				m.createReply("I/O problem while trying to extract confessions").send() ;
			else
				System.out.println("I/O problem while trying to extract confessions");
			e.printStackTrace();
		} finally {
            if(connection!=null) {
					connection.disconnect();
				} else
					System.out.println("null connection, Confession extraction aborted");

        }
		return ret;
	}

	private LinkedList<String> parseConfession(HttpURLConnection connection) throws SocketTimeoutException, IOException {
		//debug
		String confession = "";
		LinkedList<String> confessions = new LinkedList<String>();
		try {
			connection.connect();
			hits++;
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				System.out.println("Fuck at grouphug, HTTP Response code: " + connection.getResponseCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.matches(".*conf-text.*")) {  //inside confession
					while (true) {
						confession += inputLine + " ";
						if (inputLine.matches(".*</td>.*")) { //outside confession - break
							break;
						}
						inputLine = in.readLine();
					}
					confession = confession.replaceAll("<.*?>", "");
					confession = confession.replaceAll("\\s{2,}?", " ");
					confession = confession.replaceAll("\\r", "");
					confession = confession.replaceAll("\\t", "");
					confession = confession.trim();
					if (confession.length() > 0) {
						confessions.addFirst(confession);
					}
					confession = "";
				}
			}
			in.close();
		} catch(SocketTimeoutException ste) {
			// System.out.println("Connection timed out");
			throw ste;
		}
		return confessions;
	}


	public static String[] getCommands() {
		return new String[]{"confess", "search", "csize"};
	}

	public void processPrivateMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("confess")) {
			noConfessions++;
			if(m.modTrailing.toLowerCase().startsWith("about ")) {
				// grab query string
				String query = m.modTrailing.substring(6);
				// strip away any irc gunk and leading/trailing whitespace
				query = Message.removeFormattingAndColors(query).trim();
				// remove quote marks, we'll put them back later if we need them
				query = query.replaceAll("\"", "") ;
				// condense whitespace.  This can change search results, so you might not want to do it
				//query = query.replaceAll("\\s+", " ") ;
				String confession = null;
				// stick quotes around queries with whitespace in them.
				//   we should change this up if grouphug ever implements any 
				//   sort of boolean or otherwise advanced search options.
				//if (query.matches(".*\\s+.*"))
				//	confession = searchConfessions("\"" + query + "\"", m);
				//else
					confession = searchConfessions(query, m) ;
			} else if(confessions.isEmpty()) {
				if(getConfessions(m))
					m.createPagedReply(confessions.removeFirst()).send();
				else
					m.createReply("I don't have anything to confess about right now.") ;
			} else {
				m.createPagedReply(confessions.removeFirst()).send();
			}
		} else if(m.modCommand.equalsIgnoreCase("csize"))
			m.createReply("Number of confessions cached: " + confessions.size() + 
						  ". Number of confessions asked for: " + noConfessions + 
						  " Number of page requests sent: " + hits).send();
		
		//if cache is getting a bit on the big side, lets delete the 100 oldest
		if(confessions.size()>500)
			for(int i=0; i<100; i++)
				confessions.removeLast();
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}
	
	private String searchConfessions(String searchString, Message m) {
		//first search all the confessions stored for the search string
		String confession = searchStoredConfessions(searchString);
		if(confession!=null)
			return confession;
		LinkedList<String> searchedConfessions = new LinkedList<String>();
        HttpURLConnection connection = null;
		try {
			// why on god's green earth is this for loop here,
			// and why does it bother with Math.random() when
			// it multiplies the result by the integer 3, 
			// which always results in 3?
			// for(int i=((int) (Math.random()*3 + 2));i>=1;i--) {
				searchString = searchString.trim();
				searchString = Message.removeFormattingAndColors(searchString);
				String query = searchString;
				// pop our search string in quotes if it's got spaces in it.
				if (query.matches(".*\\s+.*"))
					query = "\"" + query + "\"";
				// we use URI here with a multi-argument constructor
				// because that way it encodes characters properly for us.
				URI searchURI = new URI("http", "//grouphug.us/search/" + query, null);
				URL grouphug = searchURI.toURL();
				connection = (HttpURLConnection) grouphug.openConnection();
				connection.setConnectTimeout(3000) ;
				connection.setReadTimeout(10000);
				searchedConfessions = parseConfession(connection);
				if(!searchedConfessions.isEmpty()) {
					confession = searchedConfessions.remove((int) (Math.random() * searchedConfessions.size()));
					//might as well add the rest to cache
					while(searchedConfessions.size()>0) {
						String storeConfession = searchedConfessions.removeFirst();
						if(!confessions.contains(storeConfession))
							confessions.addFirst(storeConfession);
					}
					if(m != null)
						m.createPagedReply(confession).send();
					return confession;
				} else {
					if (m != null)
						m.createReply("I'm afraid I just don't feel guilty about " + searchString + ".").send();
				}
			// }
		} catch (URISyntaxException e) {
			m.createReply("Um, I couldn't make a valid URI out of \"" + searchString + "\".").send();
		} catch (SocketTimeoutException e) {
			if (null != m)
				m.createReply("Timed out while trying to extract confessions about " + searchString).send();
			// e.printStackTrace() ;
		} catch (IOException e) {
			if (null != m)
				m.createReply("I/O problem while trying to extract confessions about " + searchString).send();
			e.printStackTrace();
            if(connection!=null) connection.disconnect();
		}
		return null;
	}
	
	private String searchStoredConfessions(String searchString) {
		StringTokenizer st = new StringTokenizer(searchString);
		LinkedList filteredConfessions = (LinkedList) confessions.clone();	//this is a shallow clone
		while(st.hasMoreTokens()) {
			String term = st.nextToken();
			ListIterator confIter = filteredConfessions.listIterator(0);
			while(confIter.hasNext()) {
				String confession = (String) confIter.next();
				if(!confession.matches(".*" + term + ".*"))
					confIter.remove();	//no match, so we take this entry out
			}
		}
		if(filteredConfessions.size()>0) {
			String confession = (String) filteredConfessions.removeFirst();
			confessions.remove(confession);	//also remove it from the overall cache
			return confession;
		}
		return null;
	}
}
