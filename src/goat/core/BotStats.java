package goat.core;

import java.util.ArrayList;

/**
 * <p>Contains lots of info about the bot and its environment.
 * @version <p>Date: 18-Dec-2003</p>
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
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
    public static ArrayList channels = new ArrayList();
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

}
