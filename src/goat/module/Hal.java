package goat.module;
import org.jibble.jmegahal.*;

import java.io.*;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

/**
 * @author <p><b>?? Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */
public class Hal extends Module {

	private JMegaHal hal = new JMegaHal();

	private boolean rant = false;

	private int time=0;

	public int messageType() {
		return WANT_UNCLAIMED_MESSAGES;
	}

	public Hal() {
		File log = new File("erotic");
		try {
			BufferedReader in = new BufferedReader(new FileReader(log));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				hal.add(inputLine);
			}
            in.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+confess\\W*"))
			return;
		if (m.trailing.toLowerCase().matches("^\\s*goat\\W*")) {
			return;
		}
		if (m.trailing.toLowerCase().matches("^\\s*goat\\W*.*")) {
			sendMessage(m.createReply(hal.getSentence(m.trailing.toLowerCase())));
			hal.add(m.trailing.toLowerCase());
		}
		if (m.trailing.toLowerCase().matches("^\\s*rant\\W*"))
			rant = !rant;
		if (rant) {
			sendMessage(m.createReply(hal.getSentence(m.trailing.toLowerCase())));
			hal.add(m.trailing.toLowerCase());
		} else {
			if(time<=0) {
				sendMessage(m.createReply(hal.getSentence(m.modTrailing)));
				hal.add(m.modTrailing);
				time = (int) (Math.random() * 40 + 10);
			} else
				time--;
		}
	}
}
