package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;


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
		try {
			URL grouphug = new URL("http://grouphug.us/random");
			HttpURLConnection connection = (HttpURLConnection) grouphug.openConnection();
			/* incompatible with 1.4
			 * It seems java.net.Socket supports socket timeouts, but URLConnection does not expose the
			 * underlying socket's timeout ability before j2se1.5. So can either rework this code here to use Socket,
			 * or leave this commented out till 1.5 is released and more prevalent. Think this latter option is the best
			 * one (ie the one that involves least work).
			 */
			// connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
			confessions = parseConfession(connection);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
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
					if (inputLine.matches(".*</td>.*")) { //outside confession - break
						break;
					}
					confession += inputLine;
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


	public String[] getCommands() {
		return new String[]{"confess", "search", "csize"};
	}

	public void processPrivateMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("confess")) {
			noConfessions++;
			if(m.modTrailing.toLowerCase().startsWith("about ")) {
				String searchReply = m.modTrailing.toLowerCase().substring(6);
				LinkedList searchConf = searchConfessions(searchReply);
				if(searchConf!=null) {
					//send a random one
					m.createPagedReply(searchConf.remove((int) (Math.random()*searchConf.size())).toString()).send();
					//might as well add the rest to cache
					while(searchConf.size()>0) {
						Object confession = searchConf.removeFirst();
						if(!confessions.contains(confession))
							confessions.addFirst(confession);
					}
				}
				else
					m.createReply("I'm afraid I just don't feel guilty about that.").send();
			} else 
				m.createPagedReply(confessions.removeFirst().toString()).send();
		} else if(m.modCommand.equalsIgnoreCase("csize"))
			m.createReply("Number of confessions cached: " + confessions.size() + ". Number of confessions asked for: " + noConfessions + " Number of page requests sent: " + hits).send();

		if (confessions.isEmpty())
			if(!getConfessions())
				m.createPagedReply("I don't feel like confessing anymore, sorry.").send();
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}
	
	private LinkedList searchConfessions(String searchString) {
		LinkedList searchedConfessions = new LinkedList();
		try {
			for(int i=((int) (Math.random()*3 + 2));i>=1;i--) {
				searchString = searchString.trim();
				searchString = searchString.replaceAll(" ", "%20");
				URL grouphug = new URL("http://grouphug.us/search/" + searchString + "/" + i*15 + "/n");
				HttpURLConnection connection = (HttpURLConnection) grouphug.openConnection();
				searchedConfessions = parseConfession(connection);
				if(!searchedConfessions.isEmpty())
					return searchedConfessions;
			}
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
