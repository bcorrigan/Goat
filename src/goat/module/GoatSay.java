package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

/**
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class GoatSay extends Module {

	private long mem;
	private int memRounded;

	private long init = System.currentTimeMillis();

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		if (m.trailing.toLowerCase().matches("^\\s*goat\\W*")) {
			sendMessage(m.createReply("Goat!"));
		}

		if (m.trailing.toLowerCase().matches("^\\s*moo*\\W*")) {
			moo(m);
		}

		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+moo*\\W*")) {
			moo(m);
		}

		if (m.isPrivate)
			System.out.print("PRIVATE: ");
		System.out.println(m.sender + ": " + m.trailing);

		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+mem\\W*")) {
			mem = Runtime.getRuntime().totalMemory() / 1024; //mem = kb
			memRounded = (int) mem;
			sendMessage(m.createReply(mem + "kb"));
		}

		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+uptime\\W*")) {
			long uptime = System.currentTimeMillis() - init;
			long seconds = uptime / 1000;
			int days = (int) (seconds / 86400);
			int hours = (int) ((seconds - (days * 86400)) / 3600);
			int minutes = (int) ((seconds - (days * 86400) - (hours * 3600)) / 60);
			seconds = (int) (seconds - (days * 86400) - (hours * 3600) - (minutes * 60));
			if (days != 0) {
				sendMessage(m.createReply(days + "d " + hours + "h " + minutes + "m " + seconds + "s."));
				return;
			}
			if (hours != 0) {
				sendMessage(m.createReply(hours + "h " + minutes + "m " + seconds + "s."));
				return;
			}
			if (minutes != 0) {
				sendMessage(m.createReply(minutes + "m " + seconds + "s."));
				return;
			}
			sendMessage(m.createReply(seconds + "s."));
		}
	}

	private void moo(Message m) {
		int i = (int) (Math.random() * 6);

		switch (i) {
			case 0:
				sendMessage(m.createReply("Moooooo!"));
				break;
			case 1:
				sendMessage(m.createReply("moooo"));
				break;
			case 2:
				sendMessage(m.createReply("MOOO"));
				break;
			case 3:
				sendMessage(m.createReply("mooOOOoOo"));
				break;
			case 4:
				sendMessage(m.createReply("moo"));
				break;
			case 5:
				sendMessage(m.createReply("moo :)"));
		}
	}
}

