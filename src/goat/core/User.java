package goat.core;

import java.util.TimeZone;
import java.util.Locale;
import java.util.HashMap;
import static goat.util.CurrencyConverter.*;
import org.jdom.JDOMException;
import java.io.IOException;

/**
 * A simple javabean to represent a user
 */
public class User {

	private String name = "";
	private String weatherStation = "";
	private String timeZone = "";
	private String currency = "";
	private Locale locale = null;
	private HashMap<String, Long> lastMessageTimestamps = new HashMap<String, Long>();
	private Message lastMessage = null;
	
	public User() {
	}
	
	public User(String uname) {
		name = uname ;
	}

	public User(String name, String weatherStation) {
		this.name = name;
		this.weatherStation = weatherStation;
	}
	
	public User(String name, String weatherStation, String timezone) {
		this.name = name;
		this.weatherStation = weatherStation;
		this.timeZone = TimeZone.getTimeZone(timezone).getID();
	}

	public String getName() {
		return name;
	}

	public void setName(String userName) {
		name = userName;
	}

	public String getWeatherStation() {
		return weatherStation;
	}

	public void setWeatherStation(String station) {
		this.weatherStation = station;
	}
	
	public String getTimeZoneString() {
		return timeZone ;
	}
	
	public TimeZone getTimeZone() {
		TimeZone ret = null;
		if (! timeZone.equals(""))
			ret = TimeZone.getTimeZone(timeZone);
		return ret;
	}
	
	public void setTimeZone(String tz) {
		if (tz.equalsIgnoreCase("unset") || tz.equals("")) {
			this.timeZone = "" ;
			return ;
		}
		// this is more complicated than it has to be thanks to 
		// java's TimeZone.getTimeZone() returning GMT if it can't
		// figure out the string it's given.
		TimeZone newTZ = TimeZone.getTimeZone(tz) ;
		TimeZone gmt = TimeZone.getTimeZone("GMT") ;
		if (newTZ.getID().equals(gmt.getID())) {
			if (tz.equalsIgnoreCase("GMT")
					|| tz.equalsIgnoreCase("zulu")
					|| tz.equalsIgnoreCase("UTC")
					|| tz.equalsIgnoreCase("UCT")
					|| tz.equalsIgnoreCase("Universal")) {
				this.timeZone = newTZ.getID() ;
			}
		} else {
			this.timeZone = newTZ.getID();
		}
				
	}
	
	public void setTimeZone(TimeZone tz) {
		this.timeZone = tz.getID() ;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public void setCurrency(String newCurrency) throws JDOMException, IOException {
		if (newCurrency.equalsIgnoreCase("unset")) {
			this.currency = "";
			return;
		} else {
			if (isRecognizedCurrency(newCurrency))
				this.currency = newCurrency.toUpperCase();
		}
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	/* ugly ugly, do later
	public void setLocale(String loc) {
		loc = loc.trim();
		String[] parts  = loc.split("\\s*,\\s*");
	}
	*/
	
	public void setLocale(Locale loc) {
		locale = loc;
	}
	
	public void setLastMessage(Message m) {
		lastMessageTimestamps.put(m.channame, System.currentTimeMillis());
		lastMessage = m;
	}

	public Long getLastMessageTimestamp() {
		return lastMessageTimestamps.get(lastMessage.channame);
	}

	public Long getLastMessageTimestamp(String channel) {
		return lastMessageTimestamps.get(channel);
	}
	
	public Message getLastMessage() {
		return lastMessage;
	}
	
	public HashMap<String, Long> getLastMessageTimestamps() {
		return lastMessageTimestamps;
	}

	public void setLastMessageTimestamps(HashMap<String, Long> lastMessageTimestamps) {
		this.lastMessageTimestamps = lastMessageTimestamps;
	}
}
