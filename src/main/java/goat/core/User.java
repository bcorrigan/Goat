package goat.core;

import java.util.Map;
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

    KVStore<String> strStore;
    KVStore<Object> objStore;
	
	public String getLastChannel() {
		return strStore.get("lastChannel");
	}

	public void setLastChannel(String lastChannel) {
		strStore.save("lastChannel", lastChannel);
	}
	
	protected User(String uname) {
		strStore = new KVStore<String>("user."+uname+".");
		objStore = new KVStore<Object>("user."+uname+".");
		
		if(!strStore.has("name")) {
		    //some backwards-compatible init required for a new user
		    setName(uname);
		    setWeatherStation("");
		    setTimeZoneString("");
		    strStore.save("currency","GBP");
		    setLongitude(-4.250132); //George square, glasgow
		    setLatitude(55.861221);
		    setWoeId(21125); //also glasgow
		}
	}

	public String getName() {
	    return strStore.get("name");
	}

	public void setName(String userName) {
	    strStore.save("name", userName);
	}

	public String getWeatherStation() {
	    return strStore.get("weatherStation");
	}

	public void setWeatherStation(String station) {
	    strStore.save("weatherStation", station);
	}
	
	public String getTimeZoneString() {
	    return strStore.get("timeZone");
	}
	
	public TimeZone getTimeZone() {
		TimeZone ret = null;
		if (strStore.has("timeZone"))
			ret = TimeZone.getTimeZone(strStore.get("timeZone"));
		return ret;
	}
	
	public void setTimeZoneString(String tz) {
		if (tz.equalsIgnoreCase("unset") || tz.equals("")) {
		    strStore.save("timeZone","");
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
			    strStore.save("timeZone",newTZ.getID());
			}
		} else {
		    strStore.save("timeZone",newTZ.getID());
		}
	}
	
	public void setTimeZone(TimeZone tz) {
	    strStore.save("timeZone",tz.getID());
	}
	
	public String getCurrency() {
		return strStore.get("currency");
	}
	
	public void setCurrency(String newCurrency) 
	        throws IOException, ParserConfigurationException, SAXException {
		if (newCurrency.equalsIgnoreCase("unset")) {
		    strStore.save("currency","");
			return;
		} else {
			if (isRecognizedCurrency(newCurrency))
			    strStore.save("currency",newCurrency.toUpperCase());
		}
	}
	
	public Locale getLocale() {
		return (Locale) objStore.get("locale");
	}
	
	public void setLocale(Locale loc) {
		objStore.save("locale",loc);
	}
	

	public void setLastMessage(Message m) {
	    //note: lastMessageTimestamps put() here is committed by the subsequent strStore.save()
	    getLastMessageTimestamps().put(m.getChanname(), System.currentTimeMillis());
		strStore.save("lastMessage",m.getTrailing());
		strStore.save("lastChannel",m.getChanname());
	}

	public boolean isActiveWithin(long duration) {
	    long now = System.currentTimeMillis();
	    Long seen = getLastMessageTimestamps().get(strStore.get("lastChannel"));
	    if(seen==null)
	        return false;
	    else 
	        return (now-seen)<duration;
	}
	
	public Long getLastMessageTimestamp() {
		return getLastMessageTimestamps().get(strStore.get("lastChannel"));
	}

	public Long getLastMessageTimestamp(String channel) {
		return getLastMessageTimestamps().get(channel);
	}
	
	
	public String getLastMessage() {
		return strStore.get("lastMessage");
	}
	
	
	// possibly harmful, but required for serialization
	public Map<String, Long> getLastMessageTimestamps() {
	    return new KVStore<Long>("user."+getName()+".lastMessageTimestamps");
	}
	 

	public void setLastMessageTimestamps(Map<String, Long> lastMessageTimestamps) {
	    KVStore<Long> tStore = new KVStore<Long>("user."+getName()+".lastMessageTimestamps");
	    tStore.putAll(lastMessageTimestamps);
	    tStore.save();
	}
	
	public String getLastfmname() {
		return strStore.get("lastfmName");
	}
	
	public void setLastfmname(String name) {
	    strStore.save("lastfmName", name);
	}
	
	public double getLongitude() {
	    return (double) objStore.get("longitude");
	}
	
	public void setLongitude(double longitude) {
	    objStore.save("longitude", longitude);
	}
	
	public double getLatitude() {
	    return (double) objStore.get("latitude");
	}
	
	public void setLatitude(double latitude) {
	    objStore.save("latitude", latitude);
	}
	
	public int getWoeId() {
	    return (int) objStore.get("woeId");
	}
	
	public void setWoeId(int woeId) {
	    objStore.save("woeId", woeId);
	}
}
