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

	private boolean rant;

	private int time;

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
    //@TODO Some ugly stuff here from when Goat couldn't tell if a message was used by another module..
	public void processChannelMessage(Message m) {
		if (m.getTrailing().toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+confess\\W*"))
			return;
		if (m.getTrailing().toLowerCase().matches("^\\s*goat\\W*")) {
			return;
		}
		if (m.getTrailing().toLowerCase().matches("^\\s*goat\\W*.*")) {
			m.createReply(hal.getSentence(m.getTrailing().toLowerCase())).send();
			hal.add(m.getTrailing().toLowerCase());
		}
		if (m.getTrailing().toLowerCase().matches("^\\s*rant\\W*"))
			rant = !rant;
		if (rant) {
			m.createReply(hal.getSentence(m.getTrailing().toLowerCase())).send();
			hal.add(m.getTrailing().toLowerCase());
		} else {
			if(time<=0) {
				m.createReply(hal.getSentence(m.getModTrailing())).send();
				hal.add(m.getModTrailing());
				time = (int) (Math.random() * 40 + 10);
			} else
				time--;
		}
	}
}
