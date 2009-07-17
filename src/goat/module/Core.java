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
		if (m.isAuthorised()) {
			if (m.getModCommand().toLowerCase().equals("part")) {
				if (BotStats.containsChannel(m.getModTrailing())) {
					new Message("", "PART", m.getModTrailing(), "").send();
					m.createReply("Channel " + m.getModTrailing() + " parted!").send();
					BotStats.removeChannel(m.getModTrailing());
					return;
				}
				m.createReply("I'm not on any such channel " + m.getModTrailing() + " :(").send();

			}
			else if (m.getModCommand().toLowerCase().equals("join"))
				if (BotStats.isValidChannelName(m.getModTrailing())) {
					new Message("", "JOIN", m.getModTrailing(), "").send();
					String response = "Channel " + m.getModTrailing() + " joined!" ;
					// we do this next dodge to avoid sending messages to nobody on startup
					if (m.getReplyTo().equals(""))
						System.out.println(response) ;
					else
						m.createReply(response).send();
					BotStats.addChannel(m.getModTrailing());
				} else
					m.createReply("Sorry, that's not a valid channel name!").send();
			else if (m.getModCommand().toLowerCase().equals("nick"))
				new Message("", "NICK", m.getModTrailing(), "").send();
			else if (m.getModCommand().toLowerCase().equals("quit")) {
				new Message("", "QUIT", m.getModTrailing(), "").send();     //@TODO not sending the quit message properly!
				System.exit(0);
				}
			else if (m.getModCommand().toLowerCase().equals("charset")) {
				Charset charset;
				try { 
					charset = Charset.forName( m.getModTrailing().trim() );
				} catch(IllegalCharsetNameException icne) {
					m.createReply("That charset is illegally specified :(").send();
					return;
				} catch(UnsupportedCharsetException uce) {
					m.createReply("That charset is not supported in this JVM.").send();
					return;
				}
				BotStats.setCharset(charset);
				m.createReply("OK, changed to " + m.getModTrailing().trim() + " charset.").send();
				}
			}
		if (m.getModCommand().toLowerCase().equals("showcharset")) {
			m.createReply( "Current charset is " + BotStats.getCharset().toString()).send();
		}
	}


	public void processChannelMessage(Message m) {
		processPrivateMessage(m);
	}

	public String[] getCommands() {
		return new String[]{"part", "join", "nick", "quit", "charset", "showcharset"};
	}
}
