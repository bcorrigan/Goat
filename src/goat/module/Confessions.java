package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Stack;

/**
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class Confessions extends Module {

	private Stack confessions = new Stack();

	public Confessions() {
		getConfessions();
	}

	private void getConfessions() {
		String confession = "";
		try {
			URL grouphug = new URL("http://grouphug.us/random");
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (confessions.empty())
			getConfessions();
	}

	public String[] getCommands() {
		return new String[] {"confess"};
	}

	public void processPrivateMessage(Message m) {
		if (m.trailing.toLowerCase().matches("^\\s*confess\\W*"))
			sendMessage(m.createReply(confessions.pop().toString()));

		if (confessions.empty())
			getConfessions();
	}

	public void processChannelMessage(Message m) {
		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+confess\\W*")) {
			sendMessage(m.createReply(confessions.pop().toString()));
		}

		if (confessions.empty())
			getConfessions();
	}
}
