package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Stack;


/**
 * @author <p><b>Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class Confessions extends Module {

	private Stack confessions = new Stack();
	//Document document;

	public Confessions() {
		getConfessions();
	}

	//TODO Just realised the page is an xml page so it'd prolly be a lot better to just get the info using a simple xml decoder or something
	private boolean getConfessions() {
		String confession = "";
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
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				System.out.println("Fuck at grouphug, HTTP Response code: " + connection.getResponseCode());
				return false;
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(grouphug.openStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.matches(".*conf-text.*")) {  //inside confession
					in.readLine();
					while (true) {
						inputLine = in.readLine();
						if (inputLine.matches(".*</td>.*")) { //outside confession - break
							break;
						}
						confession += inputLine;
					}
					confession = confession.replaceAll("<.*?>", "");
					confession = confession.replaceAll("\\s{2,}?", " ");
					confession = confession.replaceAll("\\r", "");
					confession = confession.replaceAll("\\t", "");
					confession = confession.trim();
					if (confession.length() <= 456) {
						confessions.push(confession);
					}
					confession = "";
				}
			}
			in.close();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (confessions.empty())
			getConfessions();
		return true;
	}


	public String[] getCommands() {
		return new String[]{"confess", "search"};
	}

	public void processPrivateMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("confess")) {
			m.createPagedReply(confessions.pop().toString()).send();
		}

		if (confessions.empty())
			if(!getConfessions())
				m.createPagedReply("I don't feel like confessing anymore, sorry.").send();
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}
}
