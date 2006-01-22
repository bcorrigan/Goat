/*
 * Created on 14-Aug-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.weather.User;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.regex.* ;
import java.util.Date ;
import java.text.SimpleDateFormat ;
import com.web_tomorrow.utils.suntimes.*;
import java.util.GregorianCalendar ;
import java.util.TimeZone ;

/**
 * @author bc
 *         <p/>
 *         Module that allows users to ask for weather reports (METAR data) by supplying a four letter ICAO code
 */
public class Weather extends Module {

	ArrayList users;	//all the users of this weather module
	private String codes_url = "https://pilotweb.nas.faa.gov/qryhtml/icao/" ;

	public Weather() {
        XMLDecoder XMLdec = null;
		try {
			XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/weatherUsers.xml")));
			users = (ArrayList) XMLdec.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			users = new ArrayList();
		} catch (NoSuchElementException e) {
			users = new ArrayList();
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		} finally {
            if(XMLdec!=null) XMLdec.close();
        }
	}

	/* (non-Javadoc)
	 * @see goat.core.Module#processPrivateMessage(goat.core.Message)
	 */
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
}

	/* (non-Javadoc)
	 * @see goat.core.Module#processChannelMessage(goat.core.Message)
	 */
	public void processChannelMessage(Message m) {
		if (m.modTrailing.matches("\\s*")) {     //if just whitespace
			Iterator it = users.iterator();
			while (it.hasNext()) {
				User user = (User) it.next();
				if (user.getName().equals(m.sender.toLowerCase())) {
					m.createReply(getReport(user, m.modCommand, user.getLocation())).send();
					return;
				}
			}
			m.createReply("I don't know where you are, " + m.sender + ", perhaps you should tell me " +
					"by looking at" + Message.BLUE + " " + codes_url + " " + Message.NORMAL +
					"and telling me where you are.").send();
		} else if (m.modTrailing.matches("\\s*[a-zA-Z]{4}\\s*")) { //if 4 letter code is supplied
			String location = m.modTrailing.trim().toUpperCase() ;
			boolean user_found = false ;
			User user = new User(m.sender.toLowerCase(), location) ;
			Iterator it = users.iterator();
			while (it.hasNext()) {
				User temp = (User) it.next();
				if (temp.getName().equals(m.sender.toLowerCase())) {
					user = temp ;
					user_found = true ;
					break ;
				}
			}
			String report = getReport(user, m.modCommand, location) ;
			if (report.matches(".*[ (]" + location + "[).].*")) {
				if (user_found) {
					user.setLocation(location);
				} else {
					users.add(user);
				}
				commit();
			}
			m.createReply(report).send();
		}
	}

	private String getReport(User user, String command, String station) {
        HttpURLConnection connection = null;
        BufferedReader in = null;
		  station = station.toUpperCase() ;
		try {
			URL url = new URL("http://weather.noaa.gov/pub/data/observations/metar/decoded/" + station + ".TXT");
			connection = (HttpURLConnection) url.openConnection();
			// incompatible with 1.4
			connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				return "That doesn't seem to be a valid location, " + user.getName() + ", sorry.  See " + codes_url ;
			}
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return "Hmmmn, " + user.getName() + ", the NOAA weather server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.";
			}
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine = null ; 
			String response = "" ;
			String wind_direction = "";
			String wind_mph = "";
			String wind_gust = "";
			String temp_f = "";
			String temp_c = "";
			String sky_conditions = "";
			String precipitation = "none";
			String humidity = "";
			long minutes_since_report = 0 ;
			String report_timezone = "" ;
			int report_year = 0;
			int report_month = 0;
			int report_day = 0;
			int longitude = 0;
			int latitude = 0;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.startsWith("ob") || inputLine.startsWith("cycle"))
					continue;
				inputLine = inputLine.replaceAll(":0", "");
				if (inputLine.matches(": ") && inputLine.substring(0, 1).matches("[A-Z]")) {
					inputLine = inputLine.replaceAll(": ", ':' + Message.BOLD + ' ');
					inputLine = Message.BOLD + inputLine;
				}
				response += inputLine + ' ';
				
				// Might want to move these pattern compiles out of the while loop...

				// Wind
				Matcher m = Pattern.compile("^Wind: from the ([NSEW]+) \\(.*\\) at (\\d+) MPH \\(\\d+ KT\\)(?: gusting to (\\d+) MPH \\(\\d+ KT\\))*.*").matcher(inputLine) ;
				if (m.matches()) {
					wind_direction = m.group(1) ;
					wind_mph = m.group(2) ;
					if (!(null ==  m.group(3))) {
						wind_gust = m.group(3) ;
					}
				}

				// Coordinates
				m = Pattern.compile(".* (\\d+)-\\d+(?:-\\d+)*([NS]) (\\d+)-\\d+(?:-\\d+)*([EW]).*").matcher(inputLine) ;
				if (m.matches()) {
					System.out.println("matched: " + m.group()) ;
					latitude = Integer.parseInt(m.group(1)) ;
					if ((m.group(2) != null) && (m.group(2).equals("S"))) {
						latitude = - latitude ;
					}
					longitude = Integer.parseInt(m.group(3)) ;
					if ((m.group(4) != null) && (m.group(4).equals("W"))) {
						longitude = - longitude ;
					}
				}

				// Sky conditions
				m = Pattern.compile("^Sky conditions: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					sky_conditions = m.group(1) ;
				}

				// Time
				m = Pattern.compile(".* ([A-Z]{3}) / ((\\d+)\\.(\\d+)\\.(\\d+) \\d+ UTC).*").matcher(inputLine) ;
				if (m.matches()) {
					// By way of explanation:  the regexp should yield groups:
					//  (1) local time zone as "ZZZ"
					report_timezone = m.group(1) ;
					//  (2) UTC date and time as "yyyy.MM.dd HHmm UTC"
					//  (3) year as "yyyy"
					report_year = Integer.parseInt(m.group(3)) ;
					//  (4) month as "MM"
					report_month = Integer.parseInt(m.group(4)) ;
					//  (5) day as "dd"
					report_day = Integer.parseInt(m.group(5)) ;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HHmm zzz") ;
					try { 
						Date now = new Date() ;
						long diff = now.getTime() - sdf.parse(m.group(2)).getTime() ;
						minutes_since_report = diff / (1000L*60L) ;
					} catch(java.text.ParseException e) {
						System.out.println("Date parse exception.") ;
						e.printStackTrace() ;
					}
						
				}

				// Temperature
				m = Pattern.compile("^Temperature: ([\\d.-]+) F \\(([\\d.-]+) C\\)").matcher(inputLine) ;
				if (m.matches()) {
					temp_f = m.group(1) ;
					temp_c = m.group(2) ;
				}

				// Precipitation
				m = Pattern.compile("^Precipitation last hour: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					precipitation = m.group(1) ;
				}
				// Humidity
				m = Pattern.compile("^Relative Humidity: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					humidity = m.group(1) ;
				}
			}
			String sunrise_string = "" ;
			String sunset_string = "" ;
			//  Note: crappily named class Time is bundled in with the suntimes lib
			Time sunrise_UTC = new Time(0) ;
			Time sunset_UTC = new Time(0) ;
			try {
				sunrise_UTC = SunTimes.getSunriseTimeUTC(report_year, report_month, report_day, longitude, latitude, SunTimes.ZENITH) ;
				sunset_UTC = SunTimes.getSunsetTimeUTC(report_year, report_month, report_day, longitude, latitude, SunTimes.ZENITH) ;
			} catch (SunTimesException e) {
				e.printStackTrace() ;
			}
			sunrise_string = sunString(sunrise_UTC, "sunrise", longitude, null) ;
			sunset_string = sunString(sunset_UTC, "sunset", longitude, null) ;
			String short_response = temp_f + "F/" + temp_c + "C, " 
				+ sky_conditions + ".  " + "Wind " + wind_direction + " " 
				+ wind_mph + "mph" ;
			if (! wind_gust.equals("")) {
				short_response += " gusting to " + wind_gust + "mph";
			}
			short_response += ".  Humidity " + humidity ;
			if (! precipitation.equals("none")) {
				short_response += ".  Precipitation: " + precipitation ;
			}
			if (! (sunrise_string.equals("") || sunset_string.equals(""))) {
				short_response += ".  " + sunrise_string + ", " + sunset_string ;
				response += " " + sunrise_string + ", " + sunset_string ;
			}
			if (0 != minutes_since_report) {
				short_response += ".  Reported " + minutes_since_report + " minutes ago at " + station + "." ;
			}
			if (command.equalsIgnoreCase("fullweather")) {
				return response;
			} else {
				return short_response ;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.out.println("Cannot close input stream");
                ioe.printStackTrace();
            }
      }
		return null;
	}

	private void commit() {
        XMLEncoder XMLenc = null;
		try {
			XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("resources/weatherUsers.xml")));
			XMLenc.writeObject(users);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
            if(XMLenc!=null) XMLenc.close();
        }
	}

	private String sunString(Time t, String type, int longitude, TimeZone tz) {
		if (type.equalsIgnoreCase("sunrise")) {
			type = "Sunrise" ;
		} else if (type.equalsIgnoreCase("sunset")) {
			type = "Sunset" ;
		} else {
			return "" ;
		}
		String ret = type + ": " ;
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC")) ;
		cal.set(cal.HOUR_OF_DAY, t.getHour()) ;
		cal.set(cal.MINUTE, t.getMinute()) ;
		if (null == tz) {
			// Fake time zone conversion
			cal.add(cal.HOUR_OF_DAY, longitude / 15) ;
		} else {
 	   	// Real time zone conversion
			long tempdate = cal.getTimeInMillis() ;
			cal = new GregorianCalendar(tz) ;
			cal.setTimeInMillis(tempdate) ;
		}
		ret = ret + cal.get(cal.HOUR) + ":" ;
		if (10 > cal.get(cal.MINUTE)) {
			ret = ret + "0" + cal.get(cal.MINUTE) ;
		} else {
			ret = ret + cal.get(cal.MINUTE) ;
		}
		if ( cal.get(cal.AM_PM) == cal.AM ) {
			ret += "am" ;
		} else {
			ret += "pm" ;
		}
		return ret ;
	}
		

	public String[] getCommands() {
		return new String[]{"weather", "fullweather"};
	}

	public static void main(String[] args) {
	}

}
