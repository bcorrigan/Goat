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

/**
 * @author bc
 *         <p/>
 *         Module that allows users to ask for weather reports (METAR data) by supplying a four letter ICAO code
 */
public class Weather extends Module {

	ArrayList users;	//all the users of this weather module

	public Weather() {
		try {
			XMLDecoder XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/weatherUsers.xml")));
			users = (ArrayList) XMLdec.readObject();
			XMLdec.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			users = new ArrayList();
		} catch (NoSuchElementException e) {
			users = new ArrayList();
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
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
					m.createReply(getReport(user)).send();
					return;
				}
			}
			m.createReply("I don't know where you are, " + m.sender + ", perhaps you should tell me " +
					"by looking at" + Message.BLUE + " https://pilotweb.nas.faa.gov/qryhtml/icao/ " + Message.NORMAL +
					"and telling me where you are.").send();
		} else if (m.modTrailing.matches("\\s*\\w\\w\\w\\w\\s*")) { //if 4 letter code is supplied
			Iterator it = users.iterator();
			while (it.hasNext()) {
				User user = (User) it.next();
				if (user.getName().equals(m.sender.toLowerCase())) {
					user.setLocation(m.modTrailing.split(" ")[0].toUpperCase());
					commit();
					m.createReply(getReport(user)).send();
					return;
				}
			}
			User user = new User(m.sender.toLowerCase(), m.modTrailing.split(" ")[0].toUpperCase());
			users.add(user);
			commit();
			m.createReply(getReport(user)).send();
		}

	}

	private String getReport(User user) {
		try {
			URL url = new URL("http://weather.noaa.gov/pub/data/observations/metar/decoded/" + user.getLocation() + ".TXT");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			// incompatible with 1.4
			// connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				return "That doesn't seem to be a valid location, " + user.getName() + ", sorry.";
			}
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return "Hmmmn, " + user.getName() + ", the server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.";
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine, response = in.readLine() + " ";
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.startsWith("ob") || inputLine.startsWith("cycle"))
					continue;
				inputLine = inputLine.replaceAll(":0", "");
				if (inputLine.matches(": ") && inputLine.substring(0, 1).matches("[A-Z]")) {
					inputLine = inputLine.replaceAll(": ", ":" + Message.BOLD + " ");
					inputLine = Message.BOLD + inputLine;
				}
				response += inputLine + " ";
				
			}
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void commit() {
		try {
			XMLEncoder XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("resources/weatherUsers.xml")));
			XMLenc.writeObject(users);
			XMLenc.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
	}

	public String[] getCommands() {
		return new String[]{"weather"};
	}

	public static void main(String[] args) {
	}

}
