package goat.module;

import goat.core.Module;
import goat.core.Message;

/**
 * <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */
public class Say extends Module {
	public void processPrivateMessage(Message m) {
		if(m.isAuthorised) {
			String[] words = m.modTrailing.split(" ");
			String trailing="";
			for(int i=1;i<words.length;i++)
				trailing+=words[i] + " ";
			new Message("", "PRIVMSG", words[0], trailing).send();
		}
	}

	public void processChannelMessage(Message m) {

	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public String[] getCommands() {
		return new String[]{"say"};
	}
}
