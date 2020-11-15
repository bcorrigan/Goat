package goat.module;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import goat.core.IrcMessage;
import goat.core.Module;
import goat.core.BotStats;


/**
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 12-Jan-2004</p>
 */
public class Core extends Module {

    public void processPrivateMessage(Message m) {
        if (m.isAuthorised()) {
            if (m.getModCommand().toLowerCase().equals("part")) {
                if (BotStats.getInstance().containsChannel(m.getModTrailing())) {
                    new IrcMessage("", "PART", m.getModTrailing(), "").send();
                    m.reply("Channel " + m.getModTrailing() + " parted!");
                    BotStats.getInstance().removeChannel(m.getModTrailing());
                    return;
                }
                m.reply("I'm not on any such channel " + m.getModTrailing() + " :(");

            }
            else if (m.getModCommand().toLowerCase().equals("join"))
                if (BotStats.getInstance().isValidChannelName(m.getModTrailing())) {
                    new IrcMessage("", "JOIN", m.getModTrailing(), "").send();
                    String response = "Channel " + m.getModTrailing() + " joined!" ;
                    // we do this next dodge to avoid sending messages to nobody on startup
                    if (m.getReplyTo().equals(""))
                        System.out.println(response) ;
                    else
                        m.reply(response);
                    BotStats.getInstance().addChannel(m.getModTrailing());
                } else
                    m.reply("Sorry, that's not a valid channel name!");
            else if (m.getModCommand().toLowerCase().equals("nick"))
                new IrcMessage("", "NICK", m.getModTrailing(), "").send();
            else if (m.getModCommand().toLowerCase().equals("quit")) {
                new IrcMessage("", "QUIT", m.getModTrailing(), "").send();     //@TODO not sending the quit message properly!
                System.exit(0);
            }
            else if (m.getModCommand().toLowerCase().equals("charset")) {
                Charset charset;
                try {
                    charset = Charset.forName( m.getModTrailing().trim() );
                } catch(IllegalCharsetNameException icne) {
                    m.reply("That charset is illegally specified :(");
                    return;
                } catch(UnsupportedCharsetException uce) {
                    m.reply("That charset is not supported in this JVM.");
                    return;
                }
                BotStats.getInstance().setCharset(charset);
                m.reply("OK, changed to " + m.getModTrailing().trim() + " charset.");
            } else if (m.getModCommand().toLowerCase().equals("ignore")) {
                String name = m.getModTrailing().trim();
                String response = "Ignored " + name + "." ;
                // we do this next dodge to avoid sending messages to nobody on startup
                if (m.getReplyTo().equals(""))
                    System.out.println(response);
                else
                    m.reply(response);
                BotStats.getInstance().addIgnoreName(name);
            }
        }
        if (m.getModCommand().toLowerCase().equals("showcharset")) {
            m.reply( "Current charset is " + BotStats.getInstance().getCharset().toString());
        }
    }


    public void processChannelMessage(Message m) {
        processPrivateMessage(m);
    }

    public String[] getCommands() {
        return new String[]{"part", "join", "nick", "quit", "charset", "showcharset", "ignore"};
    }
}
