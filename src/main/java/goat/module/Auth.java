package goat.module;

import goat.core.Module;
import goat.core.IrcMessage;
import goat.core.BotStats;
import static goat.util.Passwords.*;

/**
 * <p>Date: 18-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */
public class Auth extends Module {

    private String password;
    private static final String passwordFile = "resources/password.txt" ;

    public Auth() {
        loadPassword();
    }

    public void processPrivateMessage(Message m) {
        if (checkPassword(m.getModTrailing().trim().toLowerCase())) {
            BotStats.getInstance().setOwner( m.getPrefix() );
            m.reply("Authorisation successful.");
            m.reply("You (" + m.getPrefix() + ") are now my registered owner.");	//TODO: This still needs to watch the user to determine if they drop.
            new IrcMessage("", "MODE", m.getChanname() + " +o " + BotStats.getInstance().getOwner(), "").send();
        } else
            m.reply("Invalid login.");
    }

    public void processChannelMessage(Message m) {
    	m.reply("You're supposed to do that in a private message, genius.");
    }

    public String[] getCommands() {
        return new String[]{"auth"};
    }

    private boolean checkPassword(String input) {
    	//we're not running fort knox, sod this hashing crap
    	loadPassword();
        return input.equals(password);
    }

    private void loadPassword() {
    	password = getPasswords().getProperty("auth.pass", "");
    }
}
