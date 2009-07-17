package goat.core;

import goat.Goat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Contains lots of info about the bot and its environment.
 *
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 18-Dec-2003</p>
 */
public class BotStats {

	/*
	 * Where to find the config file.
	 */
	public static final String CONFIG_FILE = "config/goatrc" ;
	
	/**
	 * The bot's name.
	 */
	public static String botname;
	/**
	 * The channels we are in.
	 */
	private static ArrayList<String> channels = new ArrayList<String>();
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

	public static Set<String> commands = new HashSet<String>();
	
	public static List<Class<? extends Module>> modules;
	
    /**
     * Set to true in unit test context
     */
    public static boolean testing = false;

    /**
	 * The charset the bot is currently using
	 */
	private static Charset charset = Charset.forName("UTF-8");
	
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
        return channels.contains(chan);
		}

	public static boolean isValidChannelName(String chan) {
        return (chan.startsWith("#") || chan.startsWith("+") || chan.startsWith("&")) && !(chan.matches(":") || chan.matches(",") || chan.matches("\u0007") || chan.matches(" "));
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
	
	
	public static void readConfFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(CONFIG_FILE));
			String lineIn;

			while ((lineIn = in.readLine()) != null) {
				Message m = new Message("", "", "", "");
				m.setAuthorised(true);
				m.setPrivate(true);
				if (lineIn.startsWith("#")) {
					continue;		//so the file can be commented :-)
				}
				String[] words = lineIn.split(" ");
				m.setModCommand(words[0]);
				for (int i = 1; i < words.length; i++) {
					m.setModTrailing(m.getModTrailing()
							+ (words[i] + ' '));
				}
				m.setCommand("PRIVMSG");
				Goat.inqueue.add(m);
			}
			in.close();

		} catch (FileNotFoundException e) {
			System.out.println("goatrc not found, starting anyway..");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
}
