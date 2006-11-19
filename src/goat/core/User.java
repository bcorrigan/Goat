package goat.core;

import java.util.TimeZone;

/**
 * A simple javabean to represent a user
 */
public class User {

	private String name;
	private String weatherStation;
	private String timezone;

	public User() {
		name = "";
		weatherStation = "";
		timezone = "" ;
	}
	
	public User(String uname) {
		name = uname ;
		weatherStation = "" ;
		timezone = "" ;
	}

	public User(String name, String weatherStation) {
		this.name = name;
		this.weatherStation = weatherStation;
		this.timezone = "";
	}
	
	public User(String name, String location, String tz) {
		this.name = name;
		this.weatherStation = location;
		this.timezone = TimeZone.getTimeZone(tz).getID();
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
	
	public String getTimezone() {
		return timezone ;
	}
	
	public void setTimeZone(String tz) {
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
				this.timezone = newTZ.getID() ;
			}
		} else {
			this.timezone = newTZ.getID();
		}
				
	}
	
	public void setTimeZone(TimeZone tz) {
		this.timezone = tz.getID() ;
	}
}
