package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;


/**
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 12-Jan-2004</p>
 */
public class Core extends Module {

	public void processPrivateMessage(Message m) {
		if (m.isAuthorised) {
			if (m.modCommand.equals("part".toLowerCase())) {
				if (BotStats.containsChannel(m.modTrailing)) {
					new Message("", "PART", m.modTrailing, "").send();
					m.createReply("Channel " + m.modTrailing + " parted!").send();
					BotStats.removeChannel(m.modTrailing);
					return;
				}
				m.createReply("I'm not on any such channel " + m.modTrailing + " :(").send();

			}
		else if (m.modCommand.equals("join".toLowerCase()))
			if (BotStats.isValidChannelName(m.modTrailing)) {
				new Message("", "JOIN", m.modTrailing, "").send();
				m.createReply("Channel " + m.modTrailing + " joined!").send();
				BotStats.addChannel(m.modTrailing);
			} else
				m.createReply("Sorry, that's not a valid channel name!").send();
		else if (m.modCommand.equals("nick".toLowerCase()))
			new Message("", "NICK", m.modTrailing, "").send();
		else if (m.modCommand.equals("quit".toLowerCase())) {
			new Message("", "QUIT", m.modTrailing, "").send();     //@TODO not sending the quit message properly!
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
