package goat.core;

import java.util.TimeZone;
import java.util.HashMap;

/**
 * A simple javabean to represent a user
 */
public class User {

	private String name = "";
	private String weatherStation = "";
	private String timeZone = "";
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
	
	public String getTimeZone() {
		return timeZone ;
	}
	
	public void setTimeZone(String tz) {
		if (tz.equalsIgnoreCase("unset")) {
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
}
