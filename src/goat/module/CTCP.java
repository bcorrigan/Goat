package goat.module;

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

	private static final String CONFIG_FILE = "config/goatrc" ;
	
	private boolean alreadySeenMOTD = false;

	public CTCP() {
		inAllChannels = true;
	}

	public void processPrivateMessage(Message m) {
	}

	public void processChannelMessage(Message m) {
	}

	public void processOtherMessage(Message m) {
		int i, j;
		if (m.command.equals("PING")) {
			new Message("", "PONG", "", m.trailing).send();
			return;
		}
		//dig out the sender
		String name = "";
		j = m.prefix.indexOf('!');
		if (j > -1) {
			name = m.prefix.substring(0, j);
		}
		if (m.params.trim().equals(BotStats.botname) || m.isCTCP) {
			//check the command
			//sort ctcp bits
			if (m.isCTCP && m.CTCPCommand.equals("VERSION")) {
				Message.createCTCP(name, "NOTICE", "VERSION", Message.BOLD + BotStats.version + Message.BOLD
						+ " (" + "OS: " + System.getProperty("os.name") + " v" + System.getProperty("os.version") + ';'
						+ System.getProperty("os.arch") + " Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") 
						+ ')').send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("PING")) {
				Message.createCTCP(name, "NOTICE", "PING", m.CTCPMessage).send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("TIME")) {
				Message.createCTCP(name, "NOTICE", "TIME", (new Date()).toString()).send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("CLIENTINFO")) {
				Message.createCTCP(name, "NOTICE", "CLIENTINFO", "ACTION VERSION PING TIME CLIENTINFO SOURCE").send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("SOURCE")) {
				Message.createCTCP(name, "NOTICE", "SOURCE", "You're not getting my source, you dirty hippy.").send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("USERINFO")) {
				Message.createCTCP(name, "NOTICE", "USERINFO", "I am goat. All things goat.").send() ;
			} else if (m.isCTCP && m.CTCPCommand.equals("ERRMSG")) {
				Message.createCTCP(name, "NOTICE", "ERRMSG", m.CTCPMessage + " : No Error").send() ;
			} else if (m.isCTCP && !m.CTCPCommand.equals("ACTION"))     //this one has to come last. This signifies an unknown CTCP command.
			{
				Message.createCTCP(name, "NOTICE", "ERRMSG", m.CTCPCommand + " : Unsupported CTCP command").send() ;
			}
		} else if (m.command.equals("KICK")) {
			String[] words = m.params.split(" ");
			new Message("", "JOIN", words[0], "").send();
		} else if (m.command.equals("NICK")) {
			if (m.sender.equals(BotStats.botname)) {
				BotStats.botname = m.trailing;
			}
		}
		// numeric responses
		int intcommand;
		try {
			intcommand = Integer.parseInt(m.command);
			if (intcommand == Message.RPL_ENDOFNAMES) {    //End of /NAMES list.
				i = m.params.indexOf(' ');
				if (i > -1) {
					new Message("", "PRIVMSG", m.params.substring(i + 1), "Goat!").send();
				}
			} else if (intcommand == Message.ERR_ERRONEUSNICKNAME) {
				BotStats.botname = "Goat";
				new Message("", "NICK", BotStats.botname, "").send();
				new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version).send();
			} else if (intcommand == Message.RPL_ENDOFMOTD) {   //End of /MOTD command.
				//new Message("", "JOIN", m.channame, "").send();
				if (!alreadySeenMOTD) {
					readConfFile();  	//we only want to read the conf file when we've joined the server
					alreadySeenMOTD = true;
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

	private void readConfFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(CONFIG_FILE));
			String lineIn;

			while ((lineIn = in.readLine()) != null) {
				Message m = new Message("", "", "", "");
				m.isAuthorised = true;
				m.isPrivate = true;
				if (lineIn.startsWith("#")) {
					continue;		//so the file can be commented :-)
				}
				String[] words = lineIn.split(" ");
				m.modCommand = words[0];
				for (int i = 1; i < words.length; i++) {
					m.modTrailing += words[i] + ' ';
				}
				m.command = "PRIVMSG";
				Goat.inqueue.enqueue(m);
			}
			in.close();

		} catch (FileNotFoundException e) {
			System.out.println("goatrc not found, starting anyway..");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
}
