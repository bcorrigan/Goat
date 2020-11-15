package goat.module;

import goat.core.Constants;
import goat.core.IrcMessage;
import goat.core.Module;
import goat.core.BotStats;
import goat.Goat;

import java.util.Date;

/**
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 22-Dec-2003</p>
 */
public class CTCP extends Module {

	public CTCP() {
		inAllChannels = true;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		if(m.getModCommand().equals("version")) {
			m.reply(getVersionString());
		}
	}

	public void processOtherMessage(Message m) {
		int i, j;
		if (m.getCommand().equals("PING")) {
			new IrcMessage("", "PONG", "", m.getTrailing()).send();
			return;
		}
		//dig out the sender
		String name = "";
		j = m.getPrefix().indexOf('!');
		if (j > -1) {
			name = m.getPrefix().substring(0, j);
		}
		if (m.getParams().trim().equals(BotStats.getInstance().getBotname()) || m.isCTCP()) {
			//check the command
			//sort ctcp bits
			if (m.isCTCP() && m.getCTCPCommand().equals("VERSION")) {
				IrcMessage.createCTCP(name, "NOTICE", "VERSION", getVersionString()).send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("PING")) {
				IrcMessage.createCTCP(name, "NOTICE", "PING", m.getCTCPMessage()).send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("TIME")) {
				IrcMessage.createCTCP(name, "NOTICE", "TIME", (new Date()).toString()).send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("CLIENTINFO")) {
				IrcMessage.createCTCP(name, "NOTICE", "CLIENTINFO", "ACTION VERSION PING TIME CLIENTINFO SOURCE").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("SOURCE")) {
				IrcMessage.createCTCP(name, "NOTICE", "SOURCE", "You're not getting my source, you dirty hippy.").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("USERINFO")) {
				IrcMessage.createCTCP(name, "NOTICE", "USERINFO", "I am goat. All things goat.").send() ;
			} else if (m.isCTCP() && m.getCTCPCommand().equals("ERRMSG")) {
				IrcMessage.createCTCP(name, "NOTICE", "ERRMSG", m.getCTCPMessage() + " : No Error").send() ;
			} else if (m.isCTCP() && !m.getCTCPCommand().equals("ACTION"))     //this one has to come last. This signifies an unknown CTCP command.
			{
				IrcMessage.createCTCP(name, "NOTICE", "ERRMSG", m.getCTCPCommand() + " : Unsupported CTCP command").send() ;
			}
		} else if (m.getCommand().equals("KICK")) {
			String[] words = m.getParams().split(" ");
			new IrcMessage("", "JOIN", words[0], "").send();
		} else if (m.getCommand().equals("NICK")) {
			if (m.getSender().equals(BotStats.getInstance().getBotname())) {
				BotStats.getInstance().setBotname( m.getTrailing() );
			}
		}
		// numeric responses
		int intcommand;
		try {
			intcommand = Integer.parseInt(m.getCommand());
			if (intcommand == Constants.RPL_ENDOFNAMES) {    //End of /NAMES list.
				i = m.getParams().indexOf(' ');
				if (i > -1) {
					new IrcMessage("", "PRIVMSG", m.getParams().substring(i + 1), "Goat!").send();
				}
			} else if (intcommand == Constants.ERR_ERRONEUSNICKNAME) {
				BotStats.getInstance().setBotname( "Goat" );
				new IrcMessage("", "NICK", BotStats.getInstance().getBotname(), "").send();
				new IrcMessage("", "USER", BotStats.getInstance().getBotname() + " nowhere.com " + BotStats.getInstance().getServername(), BotStats.getInstance().getClientName() + " v." + BotStats.getInstance().getVersion()).send();
			} else if (intcommand == Constants.RPL_ENDOFMOTD) {   //End of /MOTD command.
				//new Message("", "JOIN", m.channame, "").send();
				if (!Goat.sc.alreadySeenMOTD()) {
					BotStats.getInstance().readConfFile();  	//we only want to read the conf file when we've joined the server
					Goat.sc.setAlreadySeenMOTD(true);
				}
			}
		} catch (NumberFormatException e) {
		}
	}

/*  This shouldn't be necessary, and it gums up the logging.
	public String[] getCommands() {
		return new String[]{"ALL"};
	}
*/
	private String getVersionString() {
            goat.BuildInfo$ buildInfo = goat.BuildInfo$.MODULE$;
            return Constants.BOLD + BotStats.getInstance().getVersion() +
                Constants.NORMAL + " (" +
                "OS: " + System.getProperty("os.name") +
                " v" + System.getProperty("os.version") + ';'
		+ System.getProperty("os.arch") +
                "  Java: " + System.getProperty("java.vendor") + " " +
                System.getProperty("java.version") +
                "  Scala: " + buildInfo.scalaVersion() + " " +
                "  sbt: " + buildInfo.sbtVersion() +
		")";
	}

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}

	public String[] getCommands() { return new String[] {"version"}; }
}
