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

	private int namecount = 1;
	private String botdefaultname = BotStats.botname;
	private boolean alreadySeenMOTD = false;

	public CTCP() {
		inAllChannels = true;
	}

	public void processPrivateMessage(Message m) {
		processOtherMessage(m);
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
		System.out.println("botname: " + BotStats.botname + "; m.params: " + m.params);
		if (m.params.trim().equals(BotStats.botname)) {
			//check the command
			//sort ctcp bits
			if (m.isCTCP && m.CTCPCommand.equals("VERSION")) {
				new Message("", "NOTICE", name, (char) 0x01 + "VERSION " + "Goatbot" + ':' + BotStats.version + ":(" + System.getProperty("os.name") + " v" + System.getProperty("os.version") + ';' + System.getProperty("os.arch") + ')' + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("PING")) {
				new Message("", "NOTICE", name, (char) 0x01 + "PING " + m.CTCPMessage + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("TIME")) {
				new Message("", "NOTICE", name, (char) 0x01 + "TIME :" + (new Date()).toString() + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("CLIENTINFO")) {
				new Message("", "NOTICE", name, (char) 0x01 + "CLIENTINFO ACTION VERSION PING TIME CLIENTINFO SOURCE" + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("SOURCE")) {
				new Message("", "NOTICE", name, (char) 0x01 + "SOURCE You're not getting my source, hippy." + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("USERINFO")) {
				new Message("", "NOTICE", name, (char) 0x01 + "USERINFO I am goat. All things goat." + (char) 0x01).send();
			} else if (m.isCTCP && m.CTCPCommand.equals("ERRMSG")) {
				new Message("", "NOTICE", name, (char) 0x01 + "ERRMSG " + m.CTCPMessage + " :No error" + (char) 0x01).send();
			} else if (m.isCTCP && !m.CTCPCommand.equals("ACTION"))     //this one has to come last. This signifies an unknown CTCP command.
			{
				new Message("", "NOTICE", name, (char) 0x01 + "ERRMSG " + m.CTCPCommand + " :Unsupported CTCP command" + (char) 0x01).send();
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
			} else if (intcommand == Message.ERR_NICKNAMEINUSE) {
				namecount++;
				BotStats.botname = botdefaultname + namecount;
				new Message("", "NICK", BotStats.botname, "").send();
				new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version).send();
			} else if (intcommand == Message.ERR_ERRONEUSNICKNAME) {
				BotStats.botname = "Plum";
				new Message("", "NICK", BotStats.botname, "").send();
				new Message("", "USER", BotStats.botname + " nowhere.com " + BotStats.servername, BotStats.clientName + " v." + BotStats.version).send();
			} else if (intcommand == Message.RPL_ENDOFMOTD) {   //End of /MOTD command.
				namecount = 1;
				new Message("", "JOIN", m.channame, "").send();
				if (!alreadySeenMOTD) {
					readConfFile();  	//we only want to read the conf file when we've joined the server
					alreadySeenMOTD = true;
				}
			} else if (intcommand == Message.RPL_ISON) {
				if (!m.trailing.equals(botdefaultname)) {
					new Message("", "NICK", botdefaultname, "").send();
				}
			}
		} catch (NumberFormatException e) {
		}
	}

	public String[] getCommands() {
		return new String[]{"ALL"};
	}

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}

	private void readConfFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("goatrc"));
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
