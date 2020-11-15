package goat.module;

import goat.core.IrcMessage;
import goat.core.Module;

import static goat.util.Passwords.*;

/**
 * @author bc
 * A nickserv module that sends password to server when challenged
 */
public class NickServ extends Module {

    private long lastReAuth;

    public NickServ() {
        inAllChannels = true;
        new IrcMessage("", "PRIVMSG", "NickServ", "identify " + getPassword("irc.pass")).send();
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
        if( (now - lastReAuth) > 30000)
            if(m.getSender().equals("NickServ"))
                if (m.getTrailing().startsWith("This nickname is registered and protected."))
                    if(m.getPrefix().equals("NickServ!services@services.slashnet.org")) {
                        lastReAuth = System.currentTimeMillis();
                        new IrcMessage("", "PRIVMSG", "NickServ", "identify " + getPassword("irc.pass")).send();
                    }
    }

    public int messageType() {
        return WANT_ALL_MESSAGES;
    }

    public String[] getCommands() {
        return new String[0];
    }

}
