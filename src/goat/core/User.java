package goat.core;

import java.util.TimeZone;

/**
 * A simple javabean to represent a user
 */
public class User {

	private String name;
	private String weatherStation;
	private String timeZone;

	public User() {
		name = "";
		weatherStation = "";
		timeZone = "" ;
	}
	
	public User(String uname) {
		name = uname ;
		weatherStation = "" ;
		timeZone = "" ;
	}

	public User(String name, String weatherStation) {
		this.name = name;
		this.weatherStation = weatherStation;
		this.timeZone = "";
	}
	
	public User(String name, String location, String tz) {
		this.name = name;
		this.weatherStation = location;
		this.timeZone = TimeZone.getTimeZone(tz).getID();
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
}
