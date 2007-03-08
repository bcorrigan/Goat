package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.core.User;

import java.util.TimeZone;
import java.util.ArrayList;
import java.util.Iterator;

public class Users extends Module {

	public static final String TIMEZONE_HELP_MESSAGE = 
	"To set your timezone, type 'timezone [code]', or 'timezone unset' to erase your setting.  A partial code will work, if I can resolve it to a single timeszone.  To find your code, try a web index, like this one: http://twiki.org/cgi-bin/xtra/tzdatepick.html";
	
	private static final int MAX_LISTINGS = 10;
	
	private goat.core.Users users ;

	public Users() {
		users = goat.Goat.getUsers() ;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}
	
	public static String[] getCommands() {
		return new String[]{"timezone"};
	}
	
	public void processChannelMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("timezone")) {
			timezone(m) ;
		}
	}
	
	private void timezone(Message m) {
		User user;
		if (users.hasUser(m.sender)) {
			user = users.getUser(m.sender) ;
		} else { // NOTE: new user is not added to users at this point; that should happen only if something interesting is done to this user.
			user = new User(m.sender) ;
		}
		String tz = Message.removeFormattingAndColors(m.modTrailing).trim();
		if (tz.matches("")) { // no input
			if (user.getTimeZone().equals("")) {
				m.createReply(user.getName() + ", your time zone is not set.  Instructions in /msg.").send();
			} else {
				m.createReply(user.getName() + ", your time zone is \"" + user.getTimeZone() + "\" (" + TimeZone.getTimeZone(user.getTimeZone()).getDisplayName() + ").  To change it, see instructions in /msg").send();
			}
	        Message.createPagedPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE).send();
		} else if (tz.equalsIgnoreCase("unset")) {
			user.setTimeZone("") ;
			m.createReply("Time zone unset for user " + user.getName()).send() ;
			if (users.hasUser(user))
				users.save() ;
		} else {
			String[] ids = TimeZone.getAvailableIDs() ;
			ArrayList<String> matches = new ArrayList<String>();
			boolean found = false;
			for(int i=0; i < ids.length; i++) {
				if (ids[i].equalsIgnoreCase(tz)) {
					user.setTimeZone(ids[i]);
					found = true;
					break;
				} else if (ids[i].toLowerCase().contains(tz.toLowerCase())) {
					matches.add(ids[i]);
				} else if (ids[i].replaceAll("_", " ").toLowerCase().contains(tz.toLowerCase())) {
					matches.add(ids[i]);
				}
			}
			if (! found) {
				if (matches.size() == 1) {
					user.setTimeZone(matches.get(0));
					found = true;
				} else if(matches.size() == 0) {
					m.createReply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.").send();
					Message.createPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE).send();
				} else if(matches.size() > MAX_LISTINGS) {
					m.createReply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.").send();
				} else {
					m.createReply("Time zones matching " + Message.BOLD + tz + Message.NORMAL + ":  " + quotedList(matches)).send();
				}
			}
			if (found) {
				m.createReply(user.getName() + "'s time zone set to \"" + user.getTimeZone() + "\" (" + TimeZone.getTimeZone(user.getTimeZone()).getDisplayName() + ")").send();
				if(! users.hasUser(user.getName())) {
					users.addUser(user) ;
				}
				users.save();
			}
		}
	}

	private String quotedList(ArrayList<String> strings) {
		String ret = "" ;
		Iterator i = strings.iterator();
		if (i.hasNext())
			ret += "\"" + i.next() + "\"";
		while (i.hasNext())
			ret += ", \"" + i.next() + "\"";
		return ret;
	}
}
