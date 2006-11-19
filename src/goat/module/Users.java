package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.core.User;

import java.util.TimeZone ;

public class Users extends Module {

	private goat.core.Users users = goat.Goat.users ;
	
	public void processChannelMessage(Message m) {
		User user;
		if (users.hasUser(m.sender)) {
			user = users.getUser(m.sender) ;
		} else { // NOTE: new user is not added to users at this point; that should happen only if something interesting is done to this user.
			user = new User(m.sender) ;
		}
		if (m.modCommand.equalsIgnoreCase("timezone")) {
			if (m.modTrailing.matches("\\s*")) { // no code given
				if (user.getTimezone().equals("")) {
					m.createReply(user.getName() + ", your time zone is not set.  Instructions in /msg.").send();
				} else {
					m.createReply(user.getName() + ", your time zone is " + user.getTimezone() + " (" + TimeZone.getTimeZone(user.getTimezone()).getDisplayName() + ").  To change it, see instructions in /msg").send();
				}
		        Message.createPagedPrivmsg(m.sender, "To set your timezone, type 'timezone [code]'.  A list of valid codes follows, but you will probably find it easier to locate yours via a web index, like this one: http://twiki.org/cgi-bin/xtra/tzdatepick.html .   Valid codes are:").send();
			} else {
				String tz = m.modTrailing.trim();
				user.setTimeZone(tz) ;
				if (user.getTimezone().equals("")) {
					m.createReply(user.getName() + ", java couldn't parse \"" + tz + "\" as a time zone.  I'm very sorry.").send() ;
				} else {
					if(! users.hasUser(user.getName())) {
						users.addUser(user) ;
					}
					users.save();
					m.createReply(user.getName() + "'s time zone set to " + user.getTimezone() + " (" + TimeZone.getTimeZone(user.getTimezone()).getDisplayName() + ")").send();
				}
			}
		}
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}
	
	public static String[] getCommands() {
		return new String[]{"timezone"};
	}

}
