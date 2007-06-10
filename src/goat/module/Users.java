package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.core.User;

import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class Users extends Module {

	public static final String TIMEZONE_HELP_MESSAGE = 
		"To set your timezone, type 'timezone [code]', or 'timezone unset' to erase your setting.  A partial timezone code will work, if I can resolve it to a single timeszone.  To find your code directly instead of making me guess, try a web index, like this one: http://twiki.org/cgi-bin/xtra/tzdatepick.html";

	private static final int MAX_LISTINGS = 30;
	
	private static Random random = new Random();

	private goat.core.Users users ;

	public Users() {
		users = goat.Goat.getUsers() ;
	}

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public static String[] getCommands() {
		return new String[]{"timezone", "usertime", "localtime", "worldclock", "worldtime", "seen"};
	}

	public void processChannelMessage(Message m) {
		if (m.modCommand.equalsIgnoreCase("timezone")) 
			timezone(m) ;
		else if(m.modCommand.equalsIgnoreCase("usertime") || m.modCommand.equalsIgnoreCase("localtime"))
			usertime(m);
		else if(m.modCommand.equalsIgnoreCase("worldclock") || m.modCommand.equalsIgnoreCase("worldtime"))
			worldclock(m);
		else if(m.modCommand.equalsIgnoreCase("seen"))
			seen(m);
		
		recordSighting(m);
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

	private void seen(Message m) {
		String remaining = Message.removeFormattingAndColors(m.modTrailing).replaceAll("\\s+", " ").trim();
		remaining = remaining.replaceAll("\\?", "");
		String name = "";
		String channel = "";
		final String HERE = "here";
		if(remaining.contains(" ")) {
			name = remaining.substring(0, remaining.indexOf(" "));
			remaining = remaining.substring(remaining.indexOf(" ")).trim();
			if(remaining.matches("(?i)here.*") || remaining.matches("(?i)in here.*"))
				channel = HERE;
			else if(remaining.startsWith("in "))
				channel = remaining.substring(remaining.indexOf(" ")).trim();
			else if(remaining.equals("*"))
				channel = "";
			if (! channel.equals("") 
					&& !channel.equals(HERE)
					&& !channel.startsWith("#"))
				channel = "#" + channel;
		} else
			name = remaining;
		if (name.equalsIgnoreCase(m.sender)) {
			String snarkyReplies[] = new String[] {
					"Yes.",
					"No, have you tried checking under the sofa cushions?",
					"I think " + name + " might be sitting in your chair.",
					"I don't keep tabs on unimportant people.",
					"Oh, a comedian.  Great.",
					"What's it worth to you?",
					"Vanity doesn't look good on you, sweetie.",
					"What am I, a canyon?  If you want soulless echoes, go talk to zuul.",
					"Not if I can help it, no."
			};
			m.createReply(snarkyReplies[random.nextInt(snarkyReplies.length)]).send();
		} else if (! name.equals("")) {
			if (users.hasUser(name)) {
				User u = users.getUser(name);
				if(u.getLastMessageTimestamp() != 0) {
					SimpleDateFormat df = new SimpleDateFormat("EEEE, d MMMM yyyy, hh:mma z", Locale.UK);
					if(users.hasUser(m.sender) && users.getUser(m.sender).getTimeZone() != "")
						df.setTimeZone(TimeZone.getTimeZone(users.getUser(m.sender).getTimeZone()));
					else {
						df.setTimeZone(TimeZone.getTimeZone("Zulu"));
						// nag the user if they haven't got their time zone set
						Message.createPagedPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE).send();
					}
					
					Long lastSeen = u.getLastMessageTimestamp();
					String saying = " saying: " + u.getLastMessage().trailing;
				
					if(channel.equals(""))
						channel = u.getLastMessage().channame;
					else {
						String temp = channel;
						if(temp.equals(HERE))
							temp = m.channame;
						if(! u.getLastMessage().channame.equals(temp)) {
							saying = "";
							lastSeen = u.getLastMessageTimestamp(temp);
							if(lastSeen == null) {
								m.createReply("I've seen " + name + ", but never in " + channel + ".").send();
								return;
							}
						}
					}
					
					String stamp = df.format(new Date(lastSeen));
					if(u.getTimeZone() != "") {
						df = new SimpleDateFormat("d MMM hh:mma", Locale.UK);
						df.setTimeZone(TimeZone.getTimeZone(u.getTimeZone()));
						stamp += ", " + df.format(new Date(lastSeen)) + " " + u.getName() + " time";
					}
					stamp = stamp.replaceAll("AM", "am");  // SimpleDateFormat doesn't give us a lower-case am/pm marker
					stamp = stamp.replaceAll("PM", "pm");
					
					
					String durString = durationString(System.currentTimeMillis() - lastSeen);
					m.createReply(u.getName() + " was last seen in " + channel + " "
							+ durString + " ago" + saying + "    [" + stamp + "]").send();
				} else 
					m.createReply("Oddly, I know about " + name + ", but I've never heard it say anything.").send();
			} else {
				m.createReply("I have never seen " + name + ".").send();
			}
		} else
			m.createReply("I ain't seen nothin'").send();
		
	}

	private void recordSighting(Message m) {
		if (! m.isPrivate) {
			User u;
			if(users.hasUser(m.sender))
				u = users.getUser(m.sender);
			else {
				u = new User(m.sender);
				users.addUser(u);
			}
			u.setLastMessage(m);
			users.save();
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

	public final long SECOND = 1000;
	public final long MINUTE = SECOND * 60;
	public final long HOUR = MINUTE * 60;
	public final long DAY = HOUR * 24;
	public final long YEAR = (long) ((double) DAY * 365.25);
	public final long MONTH = YEAR / 12;

	public String durationString(long intervalInMillis) {
		String durString = "less than one second";
		String durparts[] = new String[] {
				intervalInMillis / YEAR + " year",
				(intervalInMillis / MONTH) % 12 + " month",
				(intervalInMillis / DAY) % (MONTH / DAY) + " day",
				(intervalInMillis / HOUR) % 24 + " hour",
				(intervalInMillis / MINUTE) % 60 + " minute",
				(intervalInMillis / SECOND) % 60 + " second"};
		int partsCount = 0;
		for(int i=0; i<durparts.length; i++) {
			if(Character.isDigit(durparts[i].charAt(0))) {
				int endNum = durparts[i].indexOf(" ");
				int num = new Integer(durparts[i].substring(0,endNum));
				if(num == 0)
					durparts[i] = null;
				else
					partsCount++;
				if (num > 1)
					durparts[i] += "s";
			}
			else
				durparts[i] = null;
		}
		if (partsCount > 0) {
			String temp[] = new String[partsCount];
			int tempIndex = 0;
			for(int i=0; i<durparts.length; i++)
				if(durparts[i] != null)
					temp[tempIndex++] = durparts[i];
			if(temp.length == 1) {
				durString = temp[0];
			} else {
				durString = "";
				for(int i=0; i<temp.length; i++) {
					durString += temp[i];
					if(i != temp.length - 1)
						if(i == temp.length - 2)
							durString += " and ";
						else
							durString += ", ";
				}
			}
		}
		return durString;
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
