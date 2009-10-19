/*
 * Created on 14-Aug-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.core.User;
import goat.core.Users;
import goat.util.PhaseOfMoon;

import java.util.Calendar;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.regex.* ;
import java.util.Date ;
import java.text.SimpleDateFormat ;
import com.web_tomorrow.utils.suntimes.*;
import java.util.GregorianCalendar ;
import java.util.TimeZone ;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author bc
 *         <p/>
 *         Module that allows users to ask for weather reports (METAR data) by supplying a four letter ICAO code
 */
public class Weather extends Module {

	private static Users users;	//all the users of this weather module
	private String codes_url = "https://pilotweb.nas.faa.gov/qryhtml/icao/" ;

	public Weather() {
		users = goat.Goat.getUsers() ;
	}

	
	public boolean isThreadSafe() {
		return false;
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
		//User user ;
		//if (users.hasUser(m.sender)) {
		//	user = users.getUser(m.sender) ;
		//} else {
		//	user = new User(m.sender) ;
		//}
		if (m.getModTrailing().matches("\\s*")) {     //if just whitespace
			
			if (users.hasUser(m.getSender()) && ! users.getUser(m.getSender()).getWeatherStation().equals("")) {
				m.pagedReply(getReport(m.getSender(), m.getModCommand(), 
						users.getUser(m.getSender()).getWeatherStation()));
				return;
			}
			
			if(m.getPrefix().trim().matches(".*\\.nyc\\.res\\.rr\\.com$")) {
				m.reply("I'm sorry, qpt, but I can't help you until you start to help yourself.");
			} else {
				m.reply("I don't know where you are, " + m.getSender() + ", perhaps you should tell me " +
					"by looking at" + Constants.BLUE + " " + codes_url + " " + Constants.NORMAL +
					"and telling me where you are.");
			}
		} else if (m.getModTrailing().matches("\\s*[a-zA-Z0-9]{4}\\s*")) { //if 4 letter code is supplied
			String station = m.getModTrailing().trim().toUpperCase() ;
			//boolean user_found = false ;
			//User user = new User(m.sender.toLowerCase(), station) ;
			//Iterator it = users.iterator();
			/*while (it.hasNext()) {
				User temp = (User) it.next();
				if (temp.getName().equals(m.sender.toLowerCase())) {
					user = temp ;
					user_found = true ;
					break ;
				}
			} */
			String report = getReport(m.getSender(), m.getModCommand(), station);
			//debug
            //System.out.println("report:" + report + ":");
            if (report.matches(".*[ (]" + station + "[).].*")) {
            	
            	User user = users.getOrCreateUser(m.getSender());
				if (! user.getWeatherStation().equals(station)) {
					user.setWeatherStation(station);
				}
			}
			m.pagedReply(report);
		}
	}

	private String getReport(String username, String command, String station) {
        HttpURLConnection connection = null;
        BufferedReader in = null;
		  station = station.toUpperCase() ;
		try {
			URL url = new URL("http://weather.noaa.gov/pub/data/observations/metar/decoded/" + station + ".TXT");
			connection = (HttpURLConnection) url.openConnection();
			// incompatible with 1.4
			connection.setConnectTimeout(5000);  //just five seconds, we can't hang around
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				return "That doesn't seem to be a valid location, " + username + ", sorry.  See " + codes_url ;
			}
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return "Hmmmn. " + username + ", the NOAA weather server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.";
			}
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine = null ; 
			String response = "" ;
			String wind_direction = "";
			String wind_mph = "";
            //String precipitation = "";
            String wind_gust = "";
			String temp_f = "";
			String temp_c = "";
			String sky_conditions = "";
			String weather_type = "";
			String precipitation = "none";
			String humidity = "";
			long minutes_since_report = 0 ;
			// String report_timezone = "" ;
			int report_year = 0;
			int report_month = 0;
			int report_day = 0;
			double longitude = 0;
			double latitude = 0;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.startsWith("ob") || inputLine.startsWith("cycle"))
					continue;
				inputLine = inputLine.replaceAll(":0", "");
				if (inputLine.matches(": ") && inputLine.substring(0, 1).matches("[A-Z]")) {
					inputLine = inputLine.replaceAll(": ", ':' + Constants.BOLD + ' ');
					inputLine = Constants.BOLD + inputLine;
				}
				response += inputLine;
				
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
				m = Pattern.compile(".* (\\d+)-(\\d+)(?:-\\d+)*([NS]) (\\d+)-(\\d+)(?:-\\d+)*([EW]).*").matcher(inputLine) ;
				if (m.matches()) {
					//System.out.println("matched: " + m.group()) ;
					latitude = Double.parseDouble(m.group(1)) + Double.parseDouble(m.group(2)) / 60L ;
					if ((m.group(3) != null) && (m.group(3).equals("S"))) {
						latitude = - latitude ;
					}
					longitude = Double.parseDouble(m.group(4)) + Double.parseDouble(m.group(5)) / 60L ;
					if ((m.group(6) != null) && (m.group(6).equals("W"))) {
						longitude = - longitude ;
					}
					//System.out.println("coordinates (lat/long): " + latitude + "/" + longitude) ;
				}

				// Sky conditions
				m = Pattern.compile("^Sky conditions: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					sky_conditions = m.group(1) ;
				}
				
				// Weather type
				m = Pattern.compile("^Weather: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					weather_type = m.group(1) ;
				}

				// Time
				m = Pattern.compile(".* ([A-Z]{3}) / ((\\d+)\\.(\\d+)\\.(\\d+) \\d+ UTC).*").matcher(inputLine) ;
				if (m.matches()) {
					// By way of explanation:  the regexp should yield groups:
					//  (1) local time zone as "ZZZ"
					//		note: as far as I've seen, this is always EST --rs
					//report_timezone = m.group(1) ;  //unused
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
					//uncomment if we start using this again
					precipitation = m.group(1) ;
				}
				// Humidity
				m = Pattern.compile("^Relative Humidity: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					humidity = m.group(1) ;
				}
			}
			double windchill = 666.0;
			if (!"".equals(temp_f)) {
				windchill = Double.parseDouble(temp_f) ;
				if (!"".equals(wind_mph))
					windchill = windchill(Double.parseDouble(temp_f),Double.parseDouble(wind_mph)) ;
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
			TimeZone tz = null ;
			if (users.hasUser(username) && ! users.getUser(username).getTimeZoneString().equals("")) {
				tz = TimeZone.getTimeZone(users.getUser(username).getTimeZoneString()) ;
			}
			sunrise_string = sunString(sunrise_UTC, longitude, tz) ;
			sunset_string = sunString(sunset_UTC, longitude, tz) ;
			String sun_report = "Sunrise " + sunrise_string + ", sunset " + sunset_string;

            double score = getScore(wind_mph, wind_gust,temp_c,sky_conditions,weather_type,humidity,sunrise_UTC,sunset_UTC);
            double scoreRounded = Math.round(score*100)/100d;
            String short_response = temp_f + "F/" + temp_c + "C";
			if (! sky_conditions.equals("")) {
				short_response += ", " + sky_conditions ;
			}
			if (! weather_type.equals("")) {
				short_response += ", " + weather_type ;
			}
			if (! wind_direction.equals("")) {
				short_response += ".  Wind " + wind_direction + " " + wind_mph + "mph" ;
				if (! wind_gust.equals("")) {
					short_response += " gusting to " + wind_gust + "mph";
				}
			} else {
				short_response += ".  No wind" ;
			}
			short_response += ".  Humidity " + humidity ;
			//if (! precipitation.equals("none")) {
			//	short_response += ".  Precip last hour: " + precipitation ;
			//}
			String windchillString = "" ;
			if (windchill != 666.0) {
				windchillString = ".  Windchill " + String.format("%2.1fF/%2.1fC", new Object[] {windchill, fToC(windchill)}) ;
				response += windchillString ;
				if ((Double.parseDouble(temp_f) - windchill) > 5)
					short_response += windchillString ;
			}
			if (! (sunrise_string.equals("") || sunset_string.equals(""))) {
				short_response +=  ".  " + sun_report ;
				response += ".  " + sun_report ;
			}
			Date now = new Date();
			short_response += ".  Moon " + PhaseOfMoon.phaseAsShortString(now.getTime()) ;
			response += ".  Moon: " + PhaseOfMoon.phaseAsString(now.getTime()) ;
			if (0 != minutes_since_report) {
				short_response += ".  Reported " + minutes_since_report + " minutes ago at " + station ;
			}
			response += ".  Score " + scoreRounded + ".";
            short_response += ".  Score " + scoreRounded + ".";               
            if (command.equalsIgnoreCase("fullweather")) {
				return response;
			} else {
				return short_response ;
			}
		} catch  (SocketTimeoutException e) {
			return "I got bored waiting for the weather report for " + station ;
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

    private double getScore(String wind_mph, String wind_gust,String temp_c,String sky_conditions, String weather_type,String humidity,Time sunrise_UTC,Time sunset_UTC) {
        double wind_mph_d=0, temp_c_d=0, wind_gust_d=0,humidity_d=50,bonus=0,sunHours=0;
        try {
            if(!wind_mph.equals(""))
                wind_mph_d = Integer.parseInt(wind_mph);
            if(!wind_gust.equals(""))
                wind_gust_d = Integer.parseInt(wind_gust);
            temp_c_d = Double.parseDouble(temp_c);
            if(!humidity.equals(""))
                humidity_d = Integer.parseInt(humidity.substring(0, humidity.length()-1));
            if( sunset_UTC.getFractionalHours()>sunrise_UTC.getFractionalHours() )
                sunHours = sunset_UTC.getFractionalHours() - sunrise_UTC.getFractionalHours();
            else
                sunHours = sunset_UTC.getFractionalHours() + 24 - sunrise_UTC.getFractionalHours();
            // debug
				// System.out.println("sunHours:" + sunHours);
            if( sky_conditions.contains("overcast") )
                bonus+=5;

            if( sky_conditions.contains("partly cloudy") )
                bonus+=1.5;
            else if( sky_conditions.contains("mostly cloudy") )
                bonus+=3;
            else if( sky_conditions.contains("cloudy") )
                bonus+=5;

            if( weather_type.contains("light rain"))
                bonus+=5;
            else if( weather_type.contains("heavy rain"))
                bonus+=15;
            else if( weather_type.contains("rain"))
                bonus+=10;

            if( weather_type.contains("haze"))
                bonus+=5;

            if( weather_type.contains("drizzle"))
                bonus+=5;
            if( weather_type.contains("freezing drizzle"))
                bonus+=10;
            if( weather_type.contains("ice pellets"))
                bonus+=15;
            if( weather_type.contains("blowing dust"))
                bonus+=10;

            if( weather_type.contains("light fog"))
                bonus+=5;
            else if( weather_type.contains("heavy fog"))
                bonus+=15;
            else if( weather_type.contains("ice fog"))
                bonus+=20;
            else if( weather_type.contains("ground fog"))
                bonus+=15;
            else if( weather_type.contains("fog"))
                bonus+=10;

            if(weather_type.contains("freezing spray"))
                bonus+=20;
            else if(weather_type.contains("freezing"))
                bonus+=15;

            if(weather_type.contains("tornado"))
                bonus+=100;
            if(weather_type.contains("volcanic ash"))
                bonus+=100;
            if(weather_type.contains("water spouts"))
                bonus+=50;
            if(weather_type.contains("blowing sand"))
                bonus+=50;
            if(weather_type.contains("frost"))
                bonus+=15;

            if(weather_type.contains("lightning"))
                bonus+=30;

            if(weather_type.contains("thunder"))
                bonus+=30;

            if( weather_type.contains("light ice pellets"))
                bonus+=10;
            else if( weather_type.contains("heavy ice pellets"))
                bonus+=20;
            else if( weather_type.contains("ice pellets"))
                bonus+=15;

            if( weather_type.contains("light ice crystals"))
                bonus+=20;
            else if( weather_type.contains("heavy ice crystals"))
                bonus+=30;
            else if( weather_type.contains("ice crystals"))
                bonus+=25;

            if( weather_type.contains("light sleet"))
                bonus+=10;
            else if( weather_type.contains("heavy sleet"))
                bonus+=20;
            else if( weather_type.contains("sleet"))
                bonus+=15;

            if( weather_type.contains("light snow"))
                bonus+=15;
            else if( weather_type.contains("heavy snow"))
                bonus+=25;
            else if( weather_type.contains("snow"))
                bonus+=20;
            
            if( weather_type.contains("smoke"))
            	bonus+=75;
            
            //towering cumulonimbus is definitely threatening
            if( weather_type.contains("towering"))
            	bonus += 7.5;
            
        } catch(NumberFormatException nfe) {
            System.out.println("oh no!");
            return 0;
        }
       if( humidity_d<50 )
            humidity_d += 2*(50-humidity_d);
		  humidity_d = humidity_d/100;
        humidity_d -= 0.5;
        return (wind_mph_d/2) + Math.abs(15-temp_c_d) + ((wind_gust_d-wind_mph_d)/3) + humidity_d*Math.abs(temp_c_d)/2 + Math.abs(12-sunHours) + bonus;
    }

	private String sunString(Time t, double longitude, TimeZone tz) {
		String ret = "" ;
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC")) ;
		cal.set(Calendar.HOUR_OF_DAY, t.getHour()) ;
		cal.set(Calendar.MINUTE, t.getMinute()) ;
		if (null == tz) {
			// Fake time zone conversion
			// cal.add(Calendar.HOUR_OF_DAY, (int) longitude / 15) ;
		} else {
 	   	// Real time zone conversion
			long tempdate = cal.getTimeInMillis() ;
			cal = new GregorianCalendar(tz) ;
			cal.setTimeInMillis(tempdate) ;
		}
		ret = ret + cal.get(Calendar.HOUR) + ":" ;
		if (10 > cal.get(Calendar.MINUTE)) {
			ret = ret + "0" + cal.get(Calendar.MINUTE) ;
		} else {
			ret = ret + cal.get(Calendar.MINUTE) ;
		}
		if ( cal.get(Calendar.AM_PM) == Calendar.AM ) {
			ret += "am" ;
		} else {
			ret += "pm" ;
		}
		if (null == tz) {
			ret += " GMT" ;
		} 
		return ret ;
	}
	
	public double windchill(double t, double v) {
		return 35.74 + 0.6215*t - 35.75*(Math.pow(v, 0.16)) + 0.4275*t*(Math.pow(v,0.16)) ;
	}
	
	public double fToC (double f) {
		return (f - 32)*5/9;
	}
		
	public String[] getCommands() {
		return new String[]{"weather", "fullweather"};
	}
	
	public static void main(String[] args) {
	}

}
