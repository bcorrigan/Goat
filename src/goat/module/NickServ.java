package goat.module;

import goat.Goat;
import goat.core.Module;
import goat.core.Message;
import static goat.util.Passwords.*;

import java.io.*;

/**
 * @author bc
 * A nickserv module that sends password to server when challenged
 */
public class NickServ extends Module {

	private long lastAuth;

	public NickServ() {
	    inAllChannels = true;
	    new Message("", "PRIVMSG", "NickServ", "identify " + getPassword("irc.pass")).send();
	}

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
	}

    public void processOtherMessage(Message m) {
        processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {

	    //TODO really need notice functionality in this thing, or some better way of
	    //  doing reauth

	    //if our password is wrong we don't want to spam NickServ
	    long now = System.currentTimeMillis();
	    if( (now - lastAuth) > 30000)
		if(m.getSender().equals("NickServ"))
		    if(m.getPrefix().equals("NickServ!services@services.slashnet.org")) {
			lastAuth = System.currentTimeMillis();
			new Message("", "PRIVMSG", "NickServ", "identify " + getPassword("irc.pass")).send();
		    }
	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public String[] getCommands() {
	    // a bit fragile; we're counting on NickServ to begin its password challenge with the word "This"
	    return new String[]{"This"};
	}

	public static void main(String[] args) {
		NickServ ns = new NickServ();
		System.out.println(getPassword("irc.pass"));
	}
}
