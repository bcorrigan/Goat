package goat.core;

import java.util.ArrayList;

/**
 * <p>Contains lots of info about the bot and its environment.
 * @version <p>Date: 18-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class BotStats {
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
		for(int i=0;i<ob.length;i++) {
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
		if(channels.contains(chan))
			return true;
		return false;
	}

	public static boolean isValidChannelName(String chan) {
		if((chan.startsWith("#")||chan.startsWith("+")||chan.startsWith("&"))&&!(chan.matches(":")||chan.matches(",")||chan.matches("\u0007")||chan.matches(" "))) {
			return true;
		}
		return false;
	}
}
