package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

/**
 * @author <p><b>Barry Corrigan</b> All Rights Reserved.</p>
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
			m.createReply("Goat!").send();
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
			m.createReply(mem + "kb").send();
		}

		if (m.trailing.toLowerCase().matches("^\\s*" + BotStats.botname + "\\W+uptime\\W*")) {
			long uptime = System.currentTimeMillis() - init;
			long seconds = uptime / 1000;
			int days = (int) (seconds / 86400);
			int hours = (int) ((seconds - days * 86400) / 3600);
			int minutes = (int) ((seconds - days * 86400 - hours * 3600) / 60);
			seconds = (int) (seconds - days * 86400 - hours * 3600 - minutes * 60);
			if (days != 0) {
				m.createReply(days + "d " + hours + "h " + minutes + "m " + seconds + "s.").send();
				return;
			}
			if (hours != 0) {
				m.createReply(hours + "h " + minutes + "m " + seconds + "s.").send();
				return;
			}
			if (minutes != 0) {
				m.createReply(minutes + "m " + seconds + "s.").send();
				return;
			}
			m.createReply(seconds + "s.").send();
		}
	}

	private void moo(Message m) {
		int i = (int) (Math.random() * 6);

		switch (i) {
			case 0:
				m.createReply("Moooooo!").send();
				break;
			case 1:
				m.createReply("moooo").send();
				break;
			case 2:
				m.createReply("MOOO").send();
				break;
			case 3:
				m.createReply("mooOOOoOo").send();
				break;
			case 4:
				m.createReply("moo").send();
				break;
			case 5:
				m.createReply("moo :)").send();
		}
	}
}

