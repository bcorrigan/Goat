package goat.core;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * <p>Contains lots of info about the bot and its environment.
 *
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 18-Dec-2003</p>
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
	
	/**
	 * The charset the bot is currently using
	 */
	private static Charset charset = Charset.forName("ISO-8859-1");
	
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

	/**
	 * Call to set the charset in use by goat
	 * @param charset
	 */
	public static void setCharset(Charset charset) {
		BotStats.charset = charset;
		goat.Goat.sc.setCharset(charset);
	}
	
	/**
	 * Get the currently used charset from here
	 * @return
	 */
	public static Charset getCharset() {
		return charset;
	}
}
