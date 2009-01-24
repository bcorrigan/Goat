package goat.module;

import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;
import goat.Goat;

import java.util.Date;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 22-Dec-2003</p>
 */
public class CTCP extends Module {

	public CTCP() {
		inAllChannels = true;
	}

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
	}

	public void processOtherMessage(Message m) {
		int i, j;
		if (m.getCommand().equals("PING")) {
			new Message("", "PONG", "", m.getTrailing()).send();
			return;
		}
		//dig out the sender
		String name = "";
		j = m.getPrefix().indexOf('!');
		if (j > -1) {
			name = m.getPrefix().substring(0, j);
		}
		if (m.getParams().trim().equals(BotStats.botname) || m.isCTCP()) {
			//check the command
			//sort ctcp bits
			if (m.isCTCP() && m.getCTCPCommand().equals("VERSION")) {
				Message.createCTCP(name, "NOTICE", "VERSION", Constants.BOLD + BotStats.version + Constants.BOLD
						+ " (" + "OS: " + System.getProperty("os.name") + " v" + System.getProperty("os.version") + ';'
						+ System.getProperty("os.arch") + " Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") 
						+ ')').send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("PING")) {
				Message.createCTCP(name, "NOTICE", "PING", m.getCTCPMessage()).send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("TIME")) {
				Message.createCTCP(name, "NOTICE", "TIME", (new Date()).toString()).send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("CLIENTINFO")) {
				Message.createCTCP(name, "NOTICE", "CLIENTINFO", "ACTION VERSION PING TIME CLIENTINFO SOURCE").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("SOURCE")) {
				Message.createCTCP(name, "NOTICE", "SOURCE", "You're not getting my source, you dirty hippy.").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("USERINFO")) {
				Message.createCTCP(name, "NOTICE", "USERINFO", "I am goat. All things goat.").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("ERRMSG")) {
				Message.createCTCP(name, "NOTICE", "ERRMSG", m.getCTCPMessage() + " : No Error").send() ;
			} else if (m.isCTCP() && !m.getCTCPCommand().equals("ACTION"))     //this one has to come last. This signifies an unknown CTCP command.
			{
				Message.createCTCP(name, "NOTICE", "ERRMSG", m.getCTCPCommand() + " : Unsupported CTCP command").send() ;
			}
		} else if (m.getCommand().equals("KICK")) {
			String[] words = m.getParams().split(" ");
			new Message("", "JOIN", words[0], "").send();
		} else if (m.getCommand().equals("NICK")) {
			if (m.getSender().equals(BotStats.botname)) {
				BotStats.botname = m.getTrailing();
			}
		}
		// numeric responses
		int intcommand;
		try {
			intcommand = Integer.parseInt(m.getCommand());
			if (intcommand == Constants.RPL_ENDOFNAMES) {    //End of /NAMES list.
				i = m.getParams().indexOf(' ');
				if (i > -1) {
					new Message("", "PRIVMSG", m.getParams().substring(i + 1), "Goat!").send();
				}
			} else if (intcommand == Constants.ERR_ERRONEUSNICKNAME) {
				BotStats.botname = "Goat";
				new Message("", "NICK", BotStats.botname, "").send();
				new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version).send();
			} else if (intcommand == Constants.RPL_ENDOFMOTD) {   //End of /MOTD command.
				//new Message("", "JOIN", m.channame, "").send();
				if (!Goat.sc.alreadySeenMOTD()) {
					BotStats.readConfFile();  	//we only want to read the conf file when we've joined the server
					Goat.sc.setAlreadySeenMOTD(true);
				}
			}
		} catch (NumberFormatException e) {
		}
	}

/*  This shouldn't be necessary, and it gums up the logging.
	public static String[] getCommands() {
		return new String[]{"ALL"};
	}
*/

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}
}
