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
		return WANT_ALL_MESSAGES;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		if (m.getTrailing().toLowerCase().matches("^\\s*goat\\W*")) {
			m.reply("Goat!");
		}

		if (m.getTrailing().toLowerCase().matches("^\\s*moo*\\W*")) {
			moo(m);
		}

		if (m.getTrailing().toLowerCase().matches("^\\s*" + BotStats.getInstance().getBotname() + "\\W+moo*\\W*")) {
			moo(m);
		}

		/* 
		if (m.isPrivate)
			System.out.print("PRIVATE: ");
		System.out.println(m.sender + ": " + m.trailing);
		*/	
		if (m.getTrailing().toLowerCase().matches("^\\s*" + BotStats.getInstance().getBotname() + "\\W+mem\\W*")) {
			mem = Runtime.getRuntime().totalMemory() / 1024; //mem = kb
			m.reply(mem + "kb");
		}

		if (m.getTrailing().toLowerCase().matches("^\\s*" + BotStats.getInstance().getBotname() + "\\W+uptime\\W*")) {
			long uptime = System.currentTimeMillis() - init;
			m.reply(StringUtil.vshortDurationString(uptime));
		}
	}

	private void moo(Message m) {
		int i = (int) (Math.random() * 6);

		switch (i) {
			case 0:
				m.reply("Moooooo!");
				break;
			case 1:
				m.reply("moooo");
				break;
			case 2:
				m.reply("MOOO");
				break;
			case 3:
				m.reply("mooOOOoOo");
				break;
			case 4:
				m.reply("moo");
				break;
			case 5:
				m.reply("moo :)");
		}
	}
	
	public String[] getCommands() { return new String[0]; }
}

