package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.core.User;

import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Iterator;

public class Users extends Module {

	public static final String TIMEZONE_HELP_MESSAGE = 
	"To set your timezone, type 'timezone [code]', or 'timezone unset' to erase your setting.  A partial code will work, if I can resolve it to a single timeszone.  To find your code, try a web index, like this one: http://twiki.org/cgi-bin/xtra/tzdatepick.html";
	
	private static final int MAX_LISTINGS = 30;
	
	private goat.core.Users users ;

	public Users() {
		users = goat.Goat.getUsers() ;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}
	
	public static String[] getCommands() {
		return new String[]{"timezone", "usertime", "localtime", "worldclock", "worldtime"};
	}
	
	public void processChannelMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("timezone")) 
			timezone(m) ;
		else if(m.modCommand.equalsIgnoreCase("usertime") || m.modCommand.equalsIgnoreCase("localtime"))
			usertime(m);
		else if(m.modCommand.equalsIgnoreCase("worldclock") || m.modCommand.equalsIgnoreCase("worldtime"))
			worldclock(m);
		else {
			System.out.println("module Users failed to process command: \"" + m.modCommand + "\"");
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
			ArrayList<String> matches = timezoneSearch(tz);
			if (matches.size() == 1) {
				user.setTimeZone(matches.get(0));
				if(! users.hasUser(user.getName())) 
					users.addUser(user) ;
				users.save();
				m.createReply(user.getName() + "'s time zone set to \"" + user.getTimeZone() + "\"  Current time is: " + timeString(user.getTimeZone())).send();
			} else if(matches.size() == 0) {
				m.createReply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.").send();
				Message.createPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE).send();
			} else if(matches.size() > MAX_LISTINGS) {
				m.createReply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.").send();
			} else {
				m.createReply("Time zones matching " + Message.BOLD + tz + Message.NORMAL + ":  " + quotedList(matches)).send();
			}
		}
	}

	private void usertime(Message m) {
		String uname = m.modTrailing.trim();
		uname = Message.removeFormattingAndColors(uname);
		String reply = "";
		if (m.sender.equalsIgnoreCase(uname) || uname.equals("")) {
			if(! users.hasUser(m.sender)) {
				reply="I don't know anything about you, " + m.sender + ".";
				Message.createPrivmsg(m.sender, "Try setting your timezone with the command \"timezone [your time zone]\"").send();
			} else if(users.getUser(m.sender).getTimeZone().equals("")) {
				reply="I don't know your time zone, " + m.sender + ".";
				Message.createPrivmsg(m.sender, "Try setting your timezone with the command \"timezone [your time zone]\"").send();
			}
			else 
				reply="Your current time is " + timeString(users.getUser(m.sender).getTimeZone()); 
		} else if (! uname.matches("[a-zA-Z0-9^{}\\[\\]`\\\\^_-|]+"))
			reply = "You're being difficult";
		else if(! users.hasUser(uname))
			reply="I don't know anything about user \"" + uname + "\"";
		else if(users.getUser(uname).getTimeZone().equals(""))
			reply="I don't know " + uname + "'s time zone.";
		else 
			reply="Current time for " + uname + " is " + timeString(users.getUser(uname).getTimeZone()); 
		m.createReply(reply).send();
	}
	
	/**
	 * World clock.
	 * 
	 * This probably doesn't belong in User, really, but as long as we've got the other
	 * timezone fiddling stuff here, we might as well add this in, too. 
	 * 
	 * @param m
	 */
	private void worldclock(Message m) {
		String tz = m.modTrailing;
		tz = Message.removeFormattingAndColors(m.modTrailing).trim();
		if (tz.equals("")) {
			m.createReply("You need to give me a time zone.").send();
			return;
		}
		ArrayList<String> matches = timezoneSearch(tz);
		if (matches.size() == 1) {
			m.createReply("Time in zone \"" + matches.get(0) + "\" is " + timeString(matches.get(0))).send() ;
		} else if(matches.size() == 0) {
			m.createReply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.").send();
			// Message.createPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE).send();
		} else if(matches.size() > MAX_LISTINGS) {
			m.createReply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.").send();
		} else {
			m.createReply("Time zones matching " + Message.BOLD + tz + Message.NORMAL + ":  " + quotedList(matches)).send();
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
	
	private String timeString(String timezone) {
		TimeZone tz = TimeZone.getTimeZone(timezone);
		GregorianCalendar cal = new GregorianCalendar(tz);
		cal.setTimeInMillis(System.currentTimeMillis());
		int hour = cal.get(GregorianCalendar.HOUR);
		if (hour == 0)
			hour = 12;
		String ret = hour + ":";
		ret += String.format("%02d", cal.get(GregorianCalendar.MINUTE));
		if (cal.get(GregorianCalendar.AM_PM) == GregorianCalendar.AM)
			ret += "am";
		else
			ret += "pm";
		ret += ", " + cal.get(GregorianCalendar.DAY_OF_MONTH) + "/";
		ret += (cal.get(GregorianCalendar.MONTH) + 1) + "/";
		ret += cal.get(GregorianCalendar.YEAR);
		ret += " (" + tz.getID() + " - " + tz.getDisplayName() + ")";
		return ret;
	}
	
	private ArrayList<String> timezoneSearch(String searchTerm) {
		String[] ids = TimeZone.getAvailableIDs() ;
		ArrayList<String> matches = new ArrayList<String>();
		for(int i=0; i < ids.length; i++) {
			if (ids[i].equalsIgnoreCase(searchTerm)) {
				matches = new ArrayList<String>();
				matches.add(ids[i]);
				break;
			} else if (ids[i].toLowerCase().contains(searchTerm.toLowerCase())) {
				matches.add(ids[i]);
			} else if (ids[i].toLowerCase().contains(searchTerm.toLowerCase().replaceAll("\\s+", "_"))) {
				matches.add(ids[i]);
			}
		}
		return matches;
	}
}
