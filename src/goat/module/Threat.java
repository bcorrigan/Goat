package goat.module;

import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class keeps us informed of changes in the current US Threat Advisory Level.
 * <p/>
 * Class gets the latest info on it here: http://www.dhs.gov/dhspublic/getAdvisoryCondition
 * <p/>
 * The levels are:
 * <p/>
 * <ul> <li>Green (low danger) <li>Blue (be guarded) <li>Yellow (elevated) <li>Orange (high) <li>Red (shit jeeesus I'm
 * coming home!) </ul>
 *
 * @author Barry Corrigan
 */
public class Threat extends Module implements Runnable {

	private static final int UNKNOWN = 0;
	private static final int GREEN = 1;
	private static final int BLUE = 2;
	private static final int YELLOW = 3;
	private static final int ORANGE = 4;
	private static final int RED = 5;

	private static final String[] THREATS = new String[6];

	static {
		THREATS[UNKNOWN] = "UNKNOWN";
		THREATS[GREEN] = Constants.GREEN + "GREEN" + Constants.NORMAL + " (low danger)";
		THREATS[BLUE] = Constants.BLUE + "BLUE" + Constants.NORMAL + " (be guarded)";
		THREATS[YELLOW] = Constants.YELLOW + "YELLOW" + Constants.NORMAL + " (elevated danger level)";
		THREATS[ORANGE] = Constants.BROWN + "ORANGE" + Constants.NORMAL + " (high danger level)";
		THREATS[RED] = Constants.RED + "RED" + Constants.NORMAL + " (severe danger level)";
	}

	private int threatLevel;

	private boolean firstTime = true;	//true until commitThreat is run at least once, so that channels aren't spammed when module is first loaded

	public Threat() {
		Thread t = new Thread(this);
		t.start();
	}

	public boolean isThreadSafe() {
		return false;
	}
	
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		m.createReply("The current Department of Homeland Security terror threat level is " + THREATS[threatLevel]).send();
	}

	public static String[] getCommands() {
		return new String[]{"threat"};
	}

	/**
	 * A wee thread that gets the latest threat elvel every ten minutes and sends out messages informing folk of changes in
	 * the terror level.
	 */
	public void run() {
		while (true) {
			commitThreat(getLatestThreatLevel());
			try {
				Thread.sleep(600000);		//update the threat level every ten minutes
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Contacts http://www.dhs.gov/dhspublic/getAdvisoryCondition yo get the latest threat level.
	 *
	 * @return The latest threat level
	 */
	private int getLatestThreatLevel() {
        HttpURLConnection connection = null;
        BufferedReader in = null;
		try {
			URL threatURL = new URL("http://www.dhs.gov/dhspublic/getAdvisoryCondition");
			connection = (HttpURLConnection) threatURL.openConnection();
			// connection.setConnectTimeout(3000);  //jdk5 only
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				System.out.println("Fuck up at dhs.gov, HTTP Response code: " + connection.getResponseCode());
				return UNKNOWN;
			}
			in = new BufferedReader(new InputStreamReader(threatURL.openStream()));
			in.readLine();	//we discard the first line, the content we want is on the second
			String threatLevelString = in.readLine();
			if (threatLevelString.matches(".*LOW.*"))
				return GREEN;
			else if (threatLevelString.matches(".*GUARDED.*"))
				return BLUE;
			else if (threatLevelString.matches(".*ELEVATED.*"))
				return YELLOW;
			else if (threatLevelString.matches(".*HIGH.*"))
				return ORANGE;
			else if (threatLevelString.matches(".*SEVERE.*"))
				return RED;
			connection.disconnect();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if(in!=null) in.close();
            } catch(IOException ioe) {
                System.out.println("Error closing stream");
                ioe.printStackTrace();
            }
        }
		return UNKNOWN;
	}

	/**
	 * Takes a new threat level from the source website and compares to last one, decides on any actions to be taken, etc
	 *
	 * @param newThreat The very latest threat level
	 */
	private void commitThreat(int newThreat) {
		if (newThreat == UNKNOWN)
			return;
		else if (firstTime) {
			firstTime = false;
			threatLevel = newThreat;
			return;
		} else if (newThreat == threatLevel)
			return;
		else if (newThreat > threatLevel) {
			threatLevel = newThreat;
			switch (threatLevel) {
				case BLUE:
					broadcast(Constants.BOLD + "Caution: " + Constants.BOLD + "The Department of Homeland Security has upgraded the threat level to " + Constants.BLUE + "Blue" + Constants.NORMAL + ", this is not dangerous, but please be guarded.");
					break;
				case YELLOW:
					broadcast(Constants.BOLD + "Caution: " + Constants.BOLD + "The Department of Homeland Security has upgraded the threat level to " + Constants.YELLOW + "Yellow" + Constants.NORMAL + ", there is now a significant risk of terrorist attacks. Please take appropriate measures to safeguard yourself and your loved ones.");
					break;
				case ORANGE:
					broadcast(Constants.BOLD + "Caution: " + Constants.BOLD + "The Department of Homeland Security has upgraded the threat level to " + Constants.BROWN + "Orange" + Constants.NORMAL + ", there is now a " + Constants.BOLD + "very high" + Constants.BOLD + " risk of terrorist attack! Please stay away from urban areas and consider stockpiling essential supplies.");
					break;
				case RED:
					broadcast(Constants.BOLD + "Caution: " + Constants.BOLD + "The Department of Homeland Security has upgraded the threat level to " + Constants.RED + "Red" + Constants.NORMAL + ", the risk of terrorist attack is now " + Constants.BOLD + "severe" + Constants.BOLD + ", you should leave the country if possible (not by plane) and convert your assets into gold.");
			}
		} else if (newThreat < threatLevel) {
			threatLevel = newThreat;
			switch (threatLevel) {
				case GREEN:
					broadcast(Constants.BOLD + "Update: " + Constants.BOLD + "The Department of Homeland Security has downgraded the threat level to " + Constants.GREEN + "Green" + Constants.NORMAL + ", the risk of terrorist attack is low, but don't take anything for granted.");
					break;
				case BLUE:
					broadcast(Constants.BOLD + "Update: " + Constants.BOLD + "The Department of Homeland Security has downgraded the threat level to " + Constants.BLUE + "Blue" + Constants.NORMAL + ", this is still dangerous, so please be guarded.");
					break;
				case YELLOW:
					broadcast(Constants.BOLD + "Update: " + Constants.BOLD + "The Department of Homeland Security has downgraded the threat level to " + Constants.YELLOW + "Yellow" + Constants.NORMAL + ", there is still a significant risk of terrorist attacks. Continue take appropriate measures to safeguard yourself and your loved ones.");
					break;
				case ORANGE:
					broadcast(Constants.BOLD + "Update: " + Constants.BOLD + "The Department of Homeland Security has downgraded the threat level to " + Constants.BROWN + "Orange" + Constants.NORMAL + ", there is still a " + Constants.BOLD + "very high" + Constants.BOLD + " risk of terrorist attack! You can relax a little, but times are still grave.");
					break;

			}
		}
	}

	/**
	 * Pump out message to all channels goat is on.
	 *
	 * @param msg The message to get sent.
	 */
	private void broadcast(String msg) {         //TODO: This should really only broadcast to the channels the module is on, that means an accessor and some modifications through goat.core.* though
		String[] chans = BotStats.getChannels();
        for (String chan : chans) new Message("", "PRIVMSG", chan, msg).send();
    }
}
