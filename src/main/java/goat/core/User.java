package goat.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Locale;
import java.util.HashMap;

import static goat.util.CurrencyConverter.*;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.ArrayUtils;
import org.xml.sax.SAXException;

/**
 * A simple javabean to represent a user
 */
public class User {


    public static final String NAME="name";
    public static final String SCREENNAME="screenName";
    public static final String WOEID="woeId";
    public static final String LATITUDE="latitude";
    public static final String LONGITUDE="longitude";
    public static final String LASTFMNAME="lastfmName";
    public static final String CURRENCY="currency";
    public static final String TIMEZONE="timeZone";
    public static final String WEATHERSTATION="weatherStation";
    public static final String LASTCHANNEL="lastChannel";
    public static final String LASTMESSAGE="lastMessage";
    public static final String LASTMESSAGETIMESTAMPS="lastMessageTimestamps";
    public static final String LOCALE="locale";
    public static final String TWEETBUDGET="tweetBudget";

    private KVStore<String> strStore;
    private KVStore<Object> objStore;
    private KVStore<Long> lastTimestamps;


    public String getLastChannel() {
	return strStore.get(LASTCHANNEL);
    }

    public void setLastChannel(String lastChannel) {
	strStore.save(LASTCHANNEL, lastChannel);
    }

    protected User(String uname) {
	strStore = new KVStore<String>("userstore."+uname+".");
	objStore = new KVStore<Object>("userstore."+uname+".");
        lastTimestamps = new KVStore<Long>("userstore."+uname+"."+LASTMESSAGETIMESTAMPS);

	if(!strStore.has("name")) {
	    //some backwards-compatible init required for a new user
	    setName(uname);
	    setWeatherStation("");
	    setTimeZoneString("");
	    strStore.save(CURRENCY,"GBP");
	    setLongitude(-4.250132); //George square, glasgow
	    setLatitude(55.861221);
	    setWoeId(21125); //also glasgow
	    //max of 10 tweets per hour default
	    setTweetBudget(10);
	}
    }


    public String getName() {
	return strStore.get(NAME);
    }

    public void setName(String userName) {
	strStore.save(NAME, userName);
    }

    public String getWeatherStation() {
	return strStore.get(WEATHERSTATION);
    }

    public void setWeatherStation(String station) {
	strStore.save(WEATHERSTATION, station);
    }

    public String getTimeZoneString() {
	return strStore.get(TIMEZONE);
    }

    public TimeZone getTimeZone() {
	TimeZone ret = null;
	if (strStore.has(TIMEZONE))
	    ret = TimeZone.getTimeZone(strStore.get(TIMEZONE));
	return ret;
    }

    public void setTimeZoneString(String tz) {
	if (tz.equalsIgnoreCase("unset") || tz.equals("")) {
	    strStore.save(TIMEZONE,"");
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
		strStore.save(TIMEZONE,newTZ.getID());
	    }
	} else {
	    strStore.save(TIMEZONE,newTZ.getID());
	}
    }

    public void setTimeZone(TimeZone tz) {
	strStore.save(TIMEZONE,tz.getID());
    }

    public String getCurrency() {
	return strStore.get(CURRENCY);
    }

    public void setCurrency(String newCurrency)
	throws IOException, ParserConfigurationException, SAXException {
	if (newCurrency.equalsIgnoreCase("unset")) {
	    strStore.save(CURRENCY,"");
	    return;
	} else {
	    if (isRecognizedCurrency(newCurrency))
		strStore.save(CURRENCY,newCurrency.toUpperCase());
	}
    }

    public Locale getLocale() {
	return (Locale) objStore.get(LOCALE);
    }

    public void setLocale(Locale loc) {
	objStore.save(LOCALE,loc);
    }

    public synchronized void setLastMessage(Message m) {
	lastTimestamps.save(m.getChanname(), System.currentTimeMillis());
	strStore.save(LASTMESSAGE,m.getTrailing());
	strStore.save(LASTCHANNEL,m.getChanname());
    }

    public boolean isActiveWithin(long duration) {
	long now = System.currentTimeMillis();
	Long seen = lastTimestamps.get(strStore.get(LASTCHANNEL));
	if(seen==null)
	    return false;
	else
	    return (now-seen)<duration;
    }

    public Long getLastMessageTimestamp() {
	return lastTimestamps.get(strStore.get(LASTCHANNEL));
    }

    public Long getLastMessageTimestamp(String channel) {
	return lastTimestamps.get(channel);
    }

    public String getLastMessage() {
	return strStore.get(LASTMESSAGE);
    }

    public String getLastfmname() {
	return strStore.get(LASTFMNAME);
    }

    public void setLastfmname(String name) {
	strStore.save(LASTFMNAME, name);
    }

    public double getLongitude() {
	return (double) objStore.get(LONGITUDE);
    }

    public void setLongitude(double longitude) {
	objStore.save(LONGITUDE, longitude);
    }

    public double getLatitude() {
	return (double) objStore.get(LATITUDE);
    }

    public void setLatitude(double latitude) {
	objStore.save(LATITUDE, latitude);
    }

    public int getWoeId() {
	return (int) objStore.get(WOEID);
    }

    public void setWoeId(int woeId) {
	objStore.save(WOEID, woeId);
    }

    public int getTweetBudget() {
        return (int) objStore.get(TWEETBUDGET);
    }

    public void setTweetBudget(int budget) {
        objStore.save(TWEETBUDGET, budget);
    }

    public boolean has(String property) {
	return objStore.has(property);
    }

    //get all tweeter screennames this user is following
    public List<String> getFollowing() {
	KVStore<Object> following = objStore.subStore(SCREENNAME);
	List<String> followList = new ArrayList<String>(following.size());
	for(String screenName : following.keySet()) {
	    if((Boolean) following.get(screenName))
		followList.add(screenName);
	}
	return followList;
    }

    //zap a particular screenname as being followed by this user
    public void rmFollowing(String screenName) {
	String sn = screenName.toLowerCase();
	if(objStore.has(SCREENNAME+"."+sn)) {
	    objStore.remove(SCREENNAME+"."+sn);
	    objStore.save();
	}
    }

    //add a screenname as being followed by this user
    public void addFollowing(String screenName) {
	String sn = screenName.toLowerCase();
	objStore.save(SCREENNAME+"."+sn,true);
    }

    public boolean equals(User user) {
	return user.getName().equals(getName());
    }
}
