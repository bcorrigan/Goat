package goat.module;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;


/**
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 12-Jan-2004</p>
 */
public class Core extends Module {

	public void processPrivateMessage(Message m) {
		if (m.isAuthorised) {
			if (m.modCommand.toLowerCase().equals("part")) {
				if (BotStats.containsChannel(m.modTrailing)) {
					new Message("", "PART", m.modTrailing, "").send();
					m.createReply("Channel " + m.modTrailing + " parted!").send();
					BotStats.removeChannel(m.modTrailing);
					return;
				}
				m.createReply("I'm not on any such channel " + m.modTrailing + " :(").send();

			}
			else if (m.modCommand.toLowerCase().equals("join"))
				if (BotStats.isValidChannelName(m.modTrailing)) {
					new Message("", "JOIN", m.modTrailing, "").send();
					String response = "Channel " + m.modTrailing + " joined!" ;
					// we do this next dodge to avoid sending messages to nobody on startup
					if (m.replyTo.equals(""))
						System.out.println(response) ;
					else
						m.createReply(response).send();
					BotStats.addChannel(m.modTrailing);
				} else
					m.createReply("Sorry, that's not a valid channel name!").send();
			else if (m.modCommand.toLowerCase().equals("nick"))
				new Message("", "NICK", m.modTrailing, "").send();
			else if (m.modCommand.toLowerCase().equals("quit")) {
				new Message("", "QUIT", m.modTrailing, "").send();     //@TODO not sending the quit message properly!
				System.exit(0);
				}
			else if (m.modCommand.toLowerCase().equals("charset")) {
				Charset charset;
				try { 
					charset = Charset.forName( m.modTrailing.trim() );
				} catch(IllegalCharsetNameException icne) {
					m.createReply("That charset is illegally specified :(").send();
					return;
				} catch(UnsupportedCharsetException uce) {
					m.createReply("That charset is not supported in this JVM.").send();
					return;
				}
				BotStats.setCharset(charset);
				m.createReply("OK, changed to " + m.modTrailing.trim() + " charset.").send();
				}
			}
		if (m.modCommand.toLowerCase().equals("showcharset")) {
			m.createReply( "Current charset is " + BotStats.getCharset().toString()).send();
		}
	}


	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}

	public static String[] getCommands() {
		return new String[]{"part", "join", "nick", "quit", "charset", "showcharset"};
	}
}
