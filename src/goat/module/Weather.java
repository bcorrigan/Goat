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
			String inputLine, response = in.readLine() + ' ';
			String wind_direction = "";
			String wind_mph = "";
			String wind_gust = "";
			String temp_f = "";
			String temp_c = "";
			String sky_conditions = "";
			String precipitation = "none";
			String humidity = "";
			long minutes_since_report = 0 ;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.startsWith("ob") || inputLine.startsWith("cycle"))
					continue;
				inputLine = inputLine.replaceAll(":0", "");
				if (inputLine.matches(": ") && inputLine.substring(0, 1).matches("[A-Z]")) {
					inputLine = inputLine.replaceAll(": ", ':' + Message.BOLD + ' ');
					inputLine = Message.BOLD + inputLine;
				}
				response += inputLine + ' ';
				
				// Might want to move these pattern compiles out of the while loop
				Matcher m = Pattern.compile("^Wind: from the ([NSEW]+) \\(.*\\) at (\\d+) MPH \\(\\d+ KT\\)(?: gusting to (\\d+) MPH \\(\\d+ KT\\))*.*").matcher(inputLine) ;
				if (m.matches()) {
					wind_direction = m.group(1) ;
					wind_mph = m.group(2) ;
					if (!(null ==  m.group(3))) {
						wind_gust = m.group(3) ;
					}
				}
				m = Pattern.compile("^Sky conditions: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					sky_conditions = m.group(1) ;
				}
				m = Pattern.compile(".*/ (\\d+\\.\\d+\\.\\d+ \\d+ UTC).*").matcher(inputLine) ;
				if (m.matches()) {
					Date now = new Date() ;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HHmm zzz") ;
					try { 
						long diff = now.getTime() - sdf.parse(m.group(1)).getTime() ;
						minutes_since_report = diff / (1000L*60L) ;
					} catch(java.text.ParseException e) {
						System.out.println("Date parse exception.") ;
						e.printStackTrace() ;
					}
						
				}
				m = Pattern.compile("^Temperature: ([\\d.-]+) F \\(([\\d.-]+) C\\)").matcher(inputLine) ;
				if (m.matches()) {
					temp_f = m.group(1) ;
					temp_c = m.group(2) ;
				}
				m = Pattern.compile("^Precipitation last hour: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					precipitation = m.group(1) ;
				}
				m = Pattern.compile("^Relative Humidity: (.*)").matcher(inputLine) ;
				if (m.matches()) {
					humidity = m.group(1) ;
				}
			}
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

	public String[] getCommands() {
		return new String[]{"weather", "fullweather"};
	}

	public static void main(String[] args) {
	}

}
