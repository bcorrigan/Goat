package goat.core;

import java.util.TimeZone;
import java.util.Locale;
import java.util.HashMap;
import static goat.util.CurrencyConverter.*;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * A simple javabean to represent a user
 */
public class User {

	private String name = "";
	private String weatherStation = "";
	private String timeZoneString = "";
	private String currency = "";
	private Locale locale = null;
	private HashMap<String, Long> lastMessageTimestamps = new HashMap<String, Long>();
	private String lastMessage = null;
	private String lastChannel = null;
	private double longitude = -4.250132; //users default to George Square, Glasgow
	private double latitude = 55.861221;
	private int woeId = 21125; //default to Glasgow of course 
	
	public String getLastChannel() {
		return lastChannel;
	}

	public void setLastChannel(String lastChannel) {
		this.lastChannel = lastChannel;
	}

	private String lastfmName = "";
	
	private Users container = null;
	
	public User() {
	}
	
	protected User(String uname) {
		name = uname ;
	}

	/* possibly harmful
	public User(String name, String weatherStation) {
		this.name = name;
		this.weatherStation = weatherStation;
	}
	
	public User(String name, String weatherStation, String timezone) {
		this.name = name;
		this.weatherStation = weatherStation;
		this.timeZoneString = TimeZone.getTimeZone(timezone).getID();
	}
	*/

	public synchronized String getName() {
		return name;
	}

	public synchronized void setName(String userName) {
		if(null == container)
			name = userName;
		else
			synchronized (container.writeLock) {
				name = userName;
				save();
			}
	}

	public synchronized String getWeatherStation() {
		return weatherStation;
	}

	public synchronized void setWeatherStation(String station) {
		if(null == container)
			weatherStation = station;
		else
			synchronized (container.writeLock) {
				weatherStation = station;
				save();
			}
	}
	
	public synchronized String getTimeZoneString() {
		return timeZoneString ;
	}
	
	public synchronized TimeZone getTimeZone() {
		TimeZone ret = null;
		if (! timeZoneString.equals(""))
			ret = TimeZone.getTimeZone(timeZoneString);
		return ret;
	}
	
	public synchronized void setTimeZoneString(String tz) {
		if(null == container)
			reallySetTimeZoneString(tz);
		else
			synchronized (container.writeLock) {
				reallySetTimeZoneString(tz);
				save();
			}
	}
	
	private void reallySetTimeZoneString(String tz) {
		if (tz.equalsIgnoreCase("unset") || tz.equals("")) {
			this.timeZoneString = "" ;
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
				this.timeZoneString = newTZ.getID() ;
			}
		} else {
			this.timeZoneString = newTZ.getID();
		}
	}
	
	public synchronized void setTimeZone(TimeZone tz) {
		if(container == null)
			timeZoneString = tz.getID() ;
		else
			synchronized(container.writeLock) {
				timeZoneString = tz.getID() ;
				save();
			}
	}
	
	public synchronized String getCurrency() {
		return currency;
	}
	
	public synchronized void setCurrency(String newCurrency) throws IOException, ParserConfigurationException, SAXException {
		if(null == container)
			reallySetCurrency(newCurrency);
		else
			synchronized (container.writeLock) {
				reallySetCurrency(newCurrency);
				save();
			}
	}
	
	private void reallySetCurrency(String newCurrency) 
	throws IOException, ParserConfigurationException, SAXException {
		if (newCurrency.equalsIgnoreCase("unset")) {
			this.currency = "";
			return;
		} else {
			if (isRecognizedCurrency(newCurrency))
				this.currency = newCurrency.toUpperCase();
		}
	}
	
	public synchronized Locale getLocale() {
		return locale;
	}
	
	public synchronized void setLocale(Locale loc) {
		if(null == container)
			locale = loc;
		else
			synchronized(container.writeLock) {
				locale = loc;
				save();
			}
	}
	
	

	public synchronized void setLastMessage(Message m) {
		if(null == container) {
			lastMessageTimestamps.put(m.getChanname(), System.currentTimeMillis());
			lastMessage = m.getTrailing();
			lastChannel = m.getChanname();
		} else
			synchronized(container.writeLock) {
				lastMessageTimestamps.put(m.getChanname(), System.currentTimeMillis());
				lastMessage = m.getTrailing();
				lastChannel = m.getChanname();
				save();
			}
	}

	public synchronized Long getLastMessageTimestamp() {
		return lastMessageTimestamps.get(lastChannel);
	}

	public synchronized Long getLastMessageTimestamp(String channel) {
		return lastMessageTimestamps.get(channel);
	}
	
	
	public synchronized String getLastMessage() {
		return lastMessage;
	}
	
	
	// possibly harmful, but required for serialization
	public HashMap<String, Long> getLastMessageTimestamps() {
		return lastMessageTimestamps;
	}
	 

	public void setLastMessageTimestamps(HashMap<String, Long> lastMessageTimestamps) {
		this.lastMessageTimestamps = lastMessageTimestamps;
	}
	
	public synchronized String getLastfmname() {
		return lastfmName;
	}
	
	public synchronized void setLastfmname(String name) {
		if(container == null)
			lastfmName = name;
		else
			synchronized (container.writeLock) {
				lastfmName = name;
				save();
			}
	}
	
	public synchronized double getLongitude() {
		return longitude;
	}
	
	public synchronized void setLongitude(double longitude) {
		if(container == null)
			this.longitude = longitude;
		else
			synchronized (container.writeLock) {
				this.longitude = longitude;
				save();
			}
	}
	
	public synchronized double getLatitude() {
		return latitude;
	}
	
	public synchronized void setLatitude(double latitude) {
		if(container == null)
			this.latitude = latitude;
		else
			synchronized (container.writeLock) {
				this.latitude = latitude;
				save();
			}
	}
	
	public synchronized int getWoeId() {
		return woeId;
	}
	
	public synchronized void setWoeId(int woeId) {
		if(container == null)
			this.woeId = woeId;
		else
			synchronized (container.writeLock) {
				this.woeId = woeId;
				save();
			}
	}
	
	
	private synchronized void save() {
		if (!(null == container))
			synchronized (container.writeLock) {
				container.notifyUpdatesPending();
			}
	}
	
	protected void setContainer(Users bucket) {
		container = bucket;
	}
}
