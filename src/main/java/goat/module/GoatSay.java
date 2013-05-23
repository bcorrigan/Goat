package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;
import goat.util.StringUtil;

/**
 * @author <p><b>Barry Corrigan</b> All Rights Reserved.</p>
 * @version 0.1 <p>Date: 26-Nov-2003</p>
 */

public class GoatSay extends Module {

	private long mem;

	private long init = System.currentTimeMillis();

	public int messageType() {
		return WANT_UNCLAIMED_MESSAGES;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		String msg = m.getTrailing().toLowerCase();
    String botname  = BotStats.getInstance().getBotname().toLowerCase();

		if (msg.matches("^\\s*" + botname + "\\W+mem\\W*")) {
			mem = Runtime.getRuntime().totalMemory() / 1024; //mem = kb
			m.reply(mem + "kb");
		} else if (msg.matches("^\\s*" + botname + "\\W+uptime\\W*")) {
			long uptime = System.currentTimeMillis() - init;
			m.reply(StringUtil.vshortDurationString(uptime));
		} else if (msg.matches(".*goat\\W*$")) {
			// Goat doesn't like talking to other goats.
			if (!m.getSender().toLowerCase().matches("goat")) {
				m.reply("Goat!");
			}
		}
	}

	public String[] getCommands() { return new String[0]; }
}

