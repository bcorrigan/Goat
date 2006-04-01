package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.ListIterator;


/**
 * @author <p><b>Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class Confessions extends Module {

	private LinkedList confessions = new LinkedList();
	//Document document;
    private int noConfessions = 0;
	private int hits = 0;
	public Confessions() {
		getConfessions();
	}


	//TODO Just realised the page is an xml page so it'd prolly be a lot better to just get the info using a simple xml decoder or something
	private boolean getConfessions() {
        HttpURLConnection connection = null;
		try {
			URL grouphug = new URL("http://grouphug.us/random");
			connection = (HttpURLConnection) grouphug.openConnection();
			connection.setConnectTimeout(7000);  //just 7 seconds, we can't hang around
			confessions = parseConfession(connection);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
            if(connection!=null) connection.disconnect();
        }
		if (confessions.isEmpty())
			getConfessions();
		return true;
	}

	private LinkedList parseConfession(HttpURLConnection connection) throws IOException {
		String confession = "";
		LinkedList confessions = new LinkedList();
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
		return confessions;
	}


	public static String[] getCommands() {
		return new String[]{"confess", "search", "csize"};
	}

	public void processPrivateMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("confess")) {
			noConfessions++;
			if(m.modTrailing.toLowerCase().startsWith("about ")) {
				String searchReply = m.modTrailing.toLowerCase().substring(6);
				String confession = searchConfessions(searchReply);
				if(confession!=null)
					m.createPagedReply(confession).send();
				else
					m.createReply("I'm afraid I just don't feel guilty about that.").send();
			} else 
				m.createPagedReply(confessions.removeFirst().toString()).send();
		} else if(m.modCommand.equalsIgnoreCase("csize"))
			m.createReply("Number of confessions cached: " + confessions.size() + 
						  ". Number of confessions asked for: " + noConfessions + 
						  " Number of page requests sent: " + hits).send();

		if (confessions.isEmpty())
			if(!getConfessions())
				m.createPagedReply("I don't feel like confessing anymore, sorry.").send();
		
		//if cache is getting a bit on the big side, lets delete the 100 oldest
		if(confessions.size()>500)
			for(int i=0; i<100; i++)
				confessions.removeLast();
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}
	
	private String searchConfessions(String searchString) {
		//first search all the confessions stored for the search string
		String confession = searchStoredConfessions(searchString);
		if(confession!=null)
			return confession;
		LinkedList searchedConfessions = new LinkedList();
        HttpURLConnection connection = null;
		try {
			for(int i=((int) (Math.random()*3 + 2));i>=1;i--) {
				searchString = searchString.trim();
				searchString = searchString.replaceAll(" ", "%20");
				URL grouphug = new URL("http://grouphug.us/search/" + searchString + "/" + i*15 + "/n");
				connection = (HttpURLConnection) grouphug.openConnection();
				searchedConfessions = parseConfession(connection);
				if(!searchedConfessions.isEmpty()) {
					confession = searchedConfessions.remove((int) (Math.random()*searchedConfessions.size())).toString();
					//might as well add the rest to cache
					while(searchedConfessions.size()>0) {
						Object storeConfession = searchedConfessions.removeFirst();
						if(!confessions.contains(storeConfession))
							confessions.addFirst(storeConfession);
					}
					return confession;
				}
			}
			return null;
		} catch (IOException e) {
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
