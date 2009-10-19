package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.core.User;
import goat.util.StringUtil;
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

	public String[] getCommands() {
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
		String tz = StringUtil.removeFormattingAndColors(m.getModTrailing()).trim();
		if (tz.matches("")) { // no input
			String tzString = "";
			if(users.hasUser(m.getSender()))
				tzString = users.getUser(m.getSender()).getTimeZoneString();
			if (tzString.equals("")) {
				m.reply(m.getSender() + ", your time zone is not set.  Instructions in /msg.");
			} else {
				m.reply(m.getSender() + ", your time zone is \"" + tzString + "\" (" + TimeZone.getTimeZone(tzString).getDisplayName() + ").  To change it, see instructions in /msg");
			}
			Message.createPagedPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE);
		} else if (tz.equalsIgnoreCase("unset")) {
			if(users.hasUser(m.getSender()))
				users.getUser(m.getSender()).setTimeZoneString(tz) ;
			m.reply("Time zone unset for user " + m.getSender()) ;
		} else {
			ArrayList<String> matches = timezoneSearch(tz);
			if (matches.size() == 1) {
				User u = users.getOrCreateUser(m.getSender());
				u.setTimeZoneString(matches.get(0));
				m.reply(u.getName() + "'s time zone set to \"" + u.getTimeZoneString() + "\"  Current time is: " + StringUtil.timeString(u.getTimeZoneString()));
			} else if(matches.size() == 0) {
				m.reply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.");
				Message.createPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE);
			} else if(matches.size() > MAX_LISTINGS) {
				m.reply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.");
			} else {
				m.reply("Time zones matching " + Constants.BOLD + tz + Constants.NORMAL + ":  " + StringUtil.quotedList(matches));
			}
		}
	}

	private void currency(Message m) {
		String newCurrency = StringUtil.removeFormattingAndColors(m.getModTrailing());
		newCurrency = translateCurrencyAliases(newCurrency);
		newCurrency = newCurrency.trim();
		try {
			if (newCurrency.equals("")) { // no input
				if(users.hasUser(m.getSender()) && !users.getUser(m.getSender()).equals(""))
					m.reply(m.getSender() + ", your currency is " + users.getUser(m.getSender()).getCurrency() + ".");
				else
					m.reply(m.getSender() + ", your currency is not set.  Instructions in /msg");
				Message.createPagedPrivmsg(m.getSender(), CURRENCY_HELP_MESSAGE);
			} else if(newCurrency.equalsIgnoreCase("unset")) {
				if(users.hasUser(m.getSender()) && ! users.getUser(m.getSender()).getCurrency().equals("")) {
					users.getUser(m.getSender()).setCurrency(newCurrency);
					m.reply("Currency unset for user " + m.getSender());
				} else
					m.reply("eh, your currency wasn't set to begin with, " + m.getSender() + ".");
			} else if(newCurrency.matches("[a-zA-Z]{3}")) {
				if(isRecognizedCurrency(newCurrency)) {
					users.getOrCreateUser(m.getSender()).setCurrency(newCurrency);
					m.reply(m.getSender() + "'s currency set to " + users.getUser(m.getSender()).getCurrency());
				} else {
					m.reply("\"" + newCurrency + "\" is not a currency code I'm familiar with.");
				}
			} else if(newCurrency.equalsIgnoreCase("list")) {
				m.pagedReply("Current known currency codes:  " + exchangeRates.keySet().toString());
			} else {
				m.reply("I'm expecting a three-letter currency code.  Type \"currency list\", and I'll tell you all the codes I know at the moment." );
			}
		} catch (ParserConfigurationException pse) {
			m.reply("I'm sorry, I can't set your currency until someone fixes my xml parser.");
		} catch (SAXException se) {
			m.reply("I ran into trouble trying to parse the exchange rates table");
			se.printStackTrace();
		} catch (IOException ioe) {
			m.reply("I couldn't retrieve the exchange rates table from the internets");
			ioe.printStackTrace();
		}
	}

	private void usertime(Message m) {
		String uname = m.getModTrailing().trim();
		uname = StringUtil.removeFormattingAndColors(uname);
		String reply = "";
		if (m.getSender().equalsIgnoreCase(uname) || uname.equals("")) {
			if(! users.hasUser(m.getSender())) {
				reply="I don't know anything about you, " + m.getSender() + ".";
				Message.createPrivmsg(m.getSender(), "Try setting your timezone with the command \"timezone [your time zone]\"");
			} else if(users.getUser(m.getSender()).getTimeZoneString().equals("")) {
				reply="I don't know your time zone, " + m.getSender() + ".";
				Message.createPrivmsg(m.getSender(), "Try setting your timezone with the command \"timezone [your time zone]\"");
			}
			else 
				reply="Your current time is " + StringUtil.timeString(users.getUser(m.getSender()).getTimeZoneString()); 
		} else if (! uname.matches("[a-zA-Z0-9^{}\\[\\]`\\\\^_-|]+"))
			reply = "You're being difficult";
		else if(! users.hasUser(uname))
			reply="I don't know anything about user \"" + uname + "\"";
		else if(users.getUser(uname).getTimeZoneString().equals(""))
			reply="I don't know " + uname + "'s time zone.";
		else 
			reply="Current time for " + uname + " is " + StringUtil.timeString(users.getUser(uname).getTimeZoneString()); 
		m.reply(reply);
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
		tz = StringUtil.removeFormattingAndColors(m.getModTrailing()).trim();
		if (tz.equals("")) {
			m.reply("You need to give me a time zone.");
			return;
		}
		ArrayList<String> matches = timezoneSearch(tz);
		if (matches.size() == 1) {
			m.reply("Time in zone \"" + matches.get(0) + "\" is " + StringUtil.timeString(matches.get(0))) ;
		} else if(matches.size() == 0) {
			m.reply("I couldn't find any time zones matching \"" + tz + "\".  Sorry.");
			// Message.createPrivmsg(m.sender, TIMEZONE_HELP_MESSAGE);
		} else if(matches.size() > MAX_LISTINGS) {
			m.reply("I found " + matches.size() + " time zones matching \"" + tz + "\".  Listing all of them would be boring.");
		} else {
			m.reply("Time zones matching " + Constants.BOLD + tz + Constants.NORMAL + ":  " + StringUtil.quotedList(matches));
		}		
	}

	private void seen(Message m) {
		String remaining = StringUtil.removeFormattingAndColors(m.getModTrailing()).replaceAll("\\s+", " ").trim();
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
			m.reply(snarkyReplies[random.nextInt(snarkyReplies.length)]);
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
						Message.createPagedPrivmsg(m.getSender(), TIMEZONE_HELP_MESSAGE);
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
								m.reply("I've seen " + name + ", but never in " + channel + ".");
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


					String durString = StringUtil.durationString(System.currentTimeMillis() - lastSeen);
					m.reply(u.getName() + " was last seen in " + channel + " "
							+ durString + " ago" + saying + "    [" + stamp + "]");
				} else 
					m.reply("Oddly, I know about " + name + ", but I've never heard it say anything.");
			} else {
				m.reply("I have never seen " + name + ".");
			}
		} else
			m.reply("I ain't seen nothin'");

	}

	private void recordSighting(Message m) {
		if (! m.isPrivate()) {
			users.getOrCreateUser(m.getSender()).setLastMessage(m);
		}
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
