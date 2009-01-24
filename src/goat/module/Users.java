package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.core.User;
import static goat.util.CurrencyConverter.*;

import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.util.Random;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


public class Users extends Module {

	public static final String TIMEZONE_HELP_MESSAGE = 
		"To set your timezone, type 'timezone [code]', or 'timezone unset' to erase your setting.  A partial timezone code will work, if I can resolve it to a single timeszone.  To find your code directly instead of making me guess, try a web index, like this one: http://twiki.org/cgi-bin/xtra/tzdatepick.html";

	public static String CURRENCY_HELP_MESSAGE = "Type \"currency xxx\" to set your currency, where xxx is a known three-letter currency code.  Type \"currency list\" to list all available codes";

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
		return new String[]{"timezone", "usertime", "localtime", "worldclock", "worldtime", "seen", "currency"};
	}

	public void processChannelMessage(Message m) {
		if (m.getModCommand().equalsIgnoreCase("timezone")) 
			timezone(m) ;
		else if(m.getModCommand().equalsIgnoreCase("currency"))
			currency(m) ;
		else if(m.getModCommand().equalsIgnoreCase("usertime") || m.getModCommand().equalsIgnoreCase("localtime"))
			usertime(m);
		else if(m.getModCommand().equalsIgnoreCase("worldclock") || m.getModCommand().equalsIgnoreCase("worldtime"))
			worldclock(m);
		else if(m.getModCommand().equalsIgnoreCase("seen"))
			seen(m);

		recordSighting(m);
	}

	private void timezone(Message m) {
		String tz = Constants.removeFormattingAndColors(m.getModTrailing()).trim();
		if (tz.matches("")) { // no input
			String tzString = "";
			if(users.hasUser(m.getSender()))
				tzString = users.getUser(m.getSender()).getTimeZoneString();
			if (tzString.equals("")) {
				m.createReply(m.getSender() + ", your time zone is not set.  Instructions in /msg.").send();
			} else {
				m.createReply(m.getSender() + ", your time zone is \"" + tzString + "\" (" + TimeZone.getTimeZone(tzString).getDisplayName() + ").  To change it, see instructions in /msg").send();
			}
			Message.createPagedPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE).send();
		} else if (tz.equalsIgnoreCase("unset")) {
			if(users.hasUser(m.getSender()))
				users.getUser(m.getSender()).setTimeZoneString(tz) ;
			m.createReply("Time zone unset for user " + m.getSender()).send() ;
		} else {
			ArrayList<String> matches = timezoneSearch(tz);
			if (matches.size() == 1) {
				User u = users.getOrCreateUser(m.getSender());
				u.setTimeZoneString(matches.get(0));
				m.createReply(u.getName() + "'s time zone set to \"" + u.getTimeZoneString() + "\"  Current time is: " + timeString(u.getTimeZoneString())).send();
			} else if(matches.size() == 0) {
				m.createReply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.").send();
				Message.createPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE).send();
			} else if(matches.size() > MAX_LISTINGS) {
				m.createReply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.").send();
			} else {
				m.createReply("Time zones matching " + Constants.BOLD + tz + Constants.NORMAL + ":  " + quotedList(matches)).send();
			}
		}
	}

	private void currency(Message m) {
		String newCurrency = Constants.removeFormattingAndColors(m.getModTrailing());
		newCurrency = translateCurrencyAliases(newCurrency);
		newCurrency = newCurrency.trim();
		try {
			if (newCurrency.equals("")) { // no input
				if(users.hasUser(m.getSender()) && !users.getUser(m.getSender()).equals(""))
					m.createReply(m.getSender() + ", your currency is " + users.getUser(m.getSender()).getCurrency() + ".").send();
				else
					m.createReply(m.getSender() + ", your currency is not set.  Instructions in /msg").send();
				Message.createPagedPrivmsg(m.getSender(), CURRENCY_HELP_MESSAGE).send();
			} else if(newCurrency.equalsIgnoreCase("unset")) {
				if(users.hasUser(m.getSender()) && ! users.getUser(m.getSender()).getCurrency().equals("")) {
					users.getUser(m.getSender()).setCurrency(newCurrency);
					m.createReply("Currency unset for user " + m.getSender()).send();
				} else
					m.createReply("eh, your currency wasn't set to begin with, " + m.getSender() + ".");
			} else if(newCurrency.matches("[a-zA-Z]{3}")) {
				if(isRecognizedCurrency(newCurrency)) {
					users.getOrCreateUser(m.getSender()).setCurrency(newCurrency);
					m.createReply(m.getSender() + "'s currency set to " + users.getUser(m.getSender()).getCurrency()).send();
				} else {
					m.createReply("\"" + newCurrency + "\" is not a currency code I'm familiar with.");
				}
			} else if(newCurrency.equalsIgnoreCase("list")) {
				m.createPagedReply("Current known currency codes:  " + exchangeRates.keySet().toString()).send();
			} else {
				m.createReply("I'm expecting a three-letter currency code.  Type \"currency list\", and I'll tell you all the codes I know at the moment." ).send();
			}
		} catch (ParserConfigurationException pse) {
			m.createReply("I'm sorry, I can't set your currency until someone fixes my xml parser.").send();
		} catch (SAXException se) {
			m.createReply("I ran into trouble trying to parse the exchange rates table").send();
			se.printStackTrace();
		} catch (IOException ioe) {
			m.createReply("I couldn't retrieve the exchange rates table from the internets").send();
			ioe.printStackTrace();
		}
	}

	private void usertime(Message m) {
		String uname = m.getModTrailing().trim();
		uname = Constants.removeFormattingAndColors(uname);
		String reply = "";
		if (m.getSender().equalsIgnoreCase(uname) || uname.equals("")) {
			if(! users.hasUser(m.getSender())) {
				reply="I don't know anything about you, " + m.getSender() + ".";
				Message.createPrivmsg(m.getSender(), "Try setting your timezone with the command \"timezone [your time zone]\"").send();
			} else if(users.getUser(m.getSender()).getTimeZoneString().equals("")) {
				reply="I don't know your time zone, " + m.getSender() + ".";
				Message.createPrivmsg(m.getSender(), "Try setting your timezone with the command \"timezone [your time zone]\"").send();
			}
			else 
				reply="Your current time is " + timeString(users.getUser(m.getSender()).getTimeZoneString()); 
		} else if (! uname.matches("[a-zA-Z0-9^{}\\[\\]`\\\\^_-|]+"))
			reply = "You're being difficult";
		else if(! users.hasUser(uname))
			reply="I don't know anything about user \"" + uname + "\"";
		else if(users.getUser(uname).getTimeZoneString().equals(""))
			reply="I don't know " + uname + "'s time zone.";
		else 
			reply="Current time for " + uname + " is " + timeString(users.getUser(uname).getTimeZoneString()); 
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
		String tz = m.getModTrailing();
		tz = Constants.removeFormattingAndColors(m.getModTrailing()).trim();
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
			m.createReply("Time zones matching " + Constants.BOLD + tz + Constants.NORMAL + ":  " + quotedList(matches)).send();
		}		
	}

	private void seen(Message m) {
		String remaining = Constants.removeFormattingAndColors(m.getModTrailing()).replaceAll("\\s+", " ").trim();
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
		if (name.equalsIgnoreCase(m.getSender())) {
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
					if(users.hasUser(m.getSender()) && users.getUser(m.getSender()).getTimeZoneString() != "")
						df.setTimeZone(TimeZone.getTimeZone(users.getUser(m.getSender()).getTimeZoneString()));
					else {
						df.setTimeZone(TimeZone.getTimeZone("Zulu"));
						// nag the user if they haven't got their time zone set
						Message.createPagedPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE).send();
					}

					Long lastSeen = u.getLastMessageTimestamp();
					String saying = " saying: " + u.getLastMessage();

					if(channel.equals(""))
						channel = u.getLastChannel();
					else {
						String temp = channel;
						if(temp.equals(HERE))
							temp = m.getChanname();
						if(! u.getLastChannel().equals(temp)) {
							saying = "";
							lastSeen = u.getLastMessageTimestamp(temp);
							if(lastSeen == null) {
								m.createReply("I've seen " + name + ", but never in " + channel + ".").send();
								return;
							}
						}
					}

					String stamp = df.format(new Date(lastSeen));
					if(u.getTimeZoneString() != "") {
						df = new SimpleDateFormat("d MMM hh:mma", Locale.UK);
						df.setTimeZone(TimeZone.getTimeZone(u.getTimeZoneString()));
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
		if (! m.isPrivate()) {
			users.getOrCreateUser(m.getSender()).setLastMessage(m);
		}
	}

	private String quotedList(ArrayList<String> strings) {
		String ret = "" ;
		Iterator<String> i = strings.iterator();
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
		switch (cal.get(GregorianCalendar.DAY_OF_WEEK)) {
		case GregorianCalendar.SUNDAY : ret += ", Sunday"; break;
		case GregorianCalendar.MONDAY : ret += ", Monday"; break;
		case GregorianCalendar.TUESDAY : ret += ", Tuesday"; break;
		case GregorianCalendar.WEDNESDAY : ret += ", Wednesday"; break;
		case GregorianCalendar.THURSDAY : ret += ", Thursday"; break;
		case GregorianCalendar.FRIDAY : ret += ", Friday"; break;
		case GregorianCalendar.SATURDAY : ret += ", Saturday"; break;
		}
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
		TimeZone tz = null;
		for(int i=0; i < ids.length; i++) {
			tz = TimeZone.getTimeZone(ids[i]);
			if (ids[i].equalsIgnoreCase(searchTerm) || tz.getDisplayName().equalsIgnoreCase(searchTerm)) {
				matches = new ArrayList<String>();
				matches.add(ids[i]);
				break;
			} else if (ids[i].toLowerCase().contains(searchTerm.toLowerCase()) || tz.getDisplayName().toLowerCase().contains(searchTerm.toLowerCase())) {
				matches.add(ids[i]);
			} else if (ids[i].toLowerCase().contains(searchTerm.toLowerCase().replaceAll("\\s+", "_"))) {
				matches.add(ids[i]);
			}
		}
		return matches;
	}
}
