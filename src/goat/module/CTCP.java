package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.util.Date;

/**
 * @version <p>Date: 22-Dec-2003</p>
 * @author <p><b>� Barry Corrigan</b> All Rights Reserved.</p>
 */
public class CTCP extends Module {

	private int namecount = 1;
	private String botdefaultname = BotStats.botname;

	public void processPrivateMessage(Message m) {
		processOtherMessage(m);
	}

	public void processChannelMessage(Message m) {
	}

	public void processOtherMessage(Message m) {
		int i, j;
		if (m.command.equals("PING")) {
			sendMessage(new Message("", "PONG", "", m.trailing));
			return;
		}
		//dig out the sender
		String name = "";
		j = m.prefix.indexOf('!');
		if (j > -1) {
			name = m.prefix.substring(0, j);
		}
		System.out.println("botname: " + BotStats.botname + "; m.params: " + m.params);
		if (m.params.trim().equals(BotStats.botname)) {
			//check the command
			//sort ctcp bits
			if (m.isCTCP && m.CTCPCommand.equals("VERSION")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "VERSION " + "Goatbot" + ":" + BotStats.version + ":(" + System.getProperty("os.name") + " v" + System.getProperty("os.version") + ";" + System.getProperty("os.arch") + ")" + (char) 0x01));
			} else if (m.isCTCP && m.CTCPCommand.equals("PING")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "PING " + m.CTCPMessage + (char) 0x01));
			} else if (m.isCTCP && m.CTCPCommand.equals("TIME")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "TIME :" + (new Date()).toString() + (char) 0x01));
			} else if (m.isCTCP && m.CTCPCommand.equals("CLIENTINFO")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "CLIENTINFO ACTION VERSION PING TIME CLIENTINFO SOURCE" + (char) 0x01));
			} else if (m.isCTCP && m.CTCPCommand.equals("SOURCE")) {
				sendMessage(Message.createCTCP("NOTICE", name, "SOURCE", "You're not getting my source, hippy."));
			} else if (m.isCTCP && m.CTCPCommand.equals("USERINFO")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "USERINFO I am goat. All things goat." + (char) 0x01));
			} else if (m.isCTCP && m.CTCPCommand.equals("ERRMSG")) {
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "ERRMSG " + m.CTCPMessage + " :No error" + (char) 0x01));
			} else if (m.isCTCP && !m.CTCPCommand.equals("ACTION"))     //this one has to come last. This signifies an unknown CTCP command.
			{
				sendMessage(new Message("", "NOTICE", name, (char) 0x01 + "ERRMSG " + m.CTCPCommand + " :Unsupported CTCP command" + (char) 0x01));
			}
		} else if (m.command.equals("KICK")) {
			sendMessage(new Message("", "JOIN", m.channame, ""));
		}
		  else if (m.command.equals("NICK")) {
			if (m.sender.equals(BotStats.botname)) {
				BotStats.botname = m.trailing;
			}
		}
		// numeric responses
		int intcommand;
		try {
			intcommand = Integer.parseInt(m.command);
			if (intcommand == 366)     //End of /NAMES list.
			{
				i = m.params.indexOf(' ');
				if (i > -1) {
					sendMessage(new Message("", "PRIVMSG", m.params.substring(i + 1), "Goat!"));
				}
			} else if (intcommand == 433) {
				namecount++;
				BotStats.botname = botdefaultname + namecount;
				sendMessage(new Message("", "NICK", BotStats.botname, ""));
				sendMessage(new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version));
			} else if (intcommand == 432) {
				BotStats.botname = "Plum";
				sendMessage(new Message("", "NICK", BotStats.botname, ""));
				sendMessage(new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version));
			} else if (intcommand == 376)     //End of /MOTD command.
			{
				namecount = 1;
				sendMessage(new Message("", "JOIN", m.channame, ""));
			} else if (intcommand == 303) {
				if (!m.trailing.equals(botdefaultname)) {
					sendMessage(new Message("", "NICK", botdefaultname, ""));
				}
			}
		} catch (NumberFormatException e) {
		}
	}

	public String[] getCommands() {
		return new String[]{"ALL"};
	}
}
