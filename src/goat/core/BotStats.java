package goat.core;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * <p>Contains lots of info about the bot and its environment.
 *
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 18-Dec-2003</p>
 */
public class BotStats {

	static {
		BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")));
		String line;
		try {
			while ((line = br.readLine()) != null)
				if (line.startsWith("Implementation-Version")) {
					version = line.replaceFirst("Implementation-Version: ", "r");
					break;
				}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The bot's name.
	 */
	public static String botname;
	/**
	 * The channels we are in.
	 */
	private static ArrayList channels = new ArrayList();
	/**
	 * The authenticated owner of the bot.
	 */
	public static String owner;
	/**
	 * Client version.
	 */
	public static String version;
	/**
	 * The name of the server we are connected to.
	 */
	public static String servername;
	public static String clientName;

	public static synchronized String[] getChannels() {
		Object[] ob = channels.toArray();
		String[] chans = new String[ob.length];
		for (int i = 0; i < ob.length; i++) {
			chans[i] = (String) ob[i];
		}
		return chans;
	}

	public static synchronized void addChannel(String chan) {
		channels.add(chan);
	}

	public static synchronized void removeChannel(String chan) {
		channels.remove(chan);
	}

	public static synchronized boolean containsChannel(String chan) {
		if (channels.contains(chan))
			return true;
		return false;
	}

	public static boolean isValidChannelName(String chan) {
		if ((chan.startsWith("#") || chan.startsWith("+") || chan.startsWith("&")) && !(chan.matches(":") || chan.matches(",") || chan.matches("\u0007") || chan.matches(" "))) {
			return true;
		}
		return false;
	}
}
