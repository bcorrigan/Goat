package goat.module;

import goat.core.Module;
import goat.core.Message;

/**
 * @version <p>Date: 12-Jan-2004</p>
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
 */
public class Core extends Module {

	public void processPrivateMessage(Message m) {
		if (m.isAuthorised) {
			if (m.modCommand.equals("part".toLowerCase()))
				sendMessage(new Message("", "PART", m.modTrailing, ""));
			else if (m.modCommand.equals("join".toLowerCase()))
				sendMessage(new Message("", "JOIN", m.modTrailing, ""));
			else if (m.modCommand.equals("nick".toLowerCase()))
				sendMessage(new Message("", "NICK", m.modTrailing, ""));
			else if (m.modCommand.equals("quit".toLowerCase())) {
				sendMessage(new Message("", "QUIT", m.modTrailing, ""));
				System.exit(0);
			}
		}
	}

	public void processChannelMessage(Message m) {

	}

	public String[] getCommands() {
		return new String[]{"part", "join", "nick", "quit"};
	}
}
