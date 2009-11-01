package goat.core;

import goat.Goat;

import goat.util.StringUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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

	private static BotStats instance;
	
	private BotStats() {
		
	}
	
	public static BotStats getInstance() {
		if(instance==null)
			instance = new BotStats();
		return instance;
	}
	
	/*
	 * Where to find the config file.
	 */
	private String CONFIG_FILE = "config/goatrc" ;
	
	/**
	 * The bot's name.
	 */
	private String botname;
	/**
	 * The channels we are in.
	 */
	private ArrayList<String> channels = new ArrayList<String>();
	/**
	 * The authenticated owner of the bot.
	 */
	private String owner;
	/**
	 * Client version.
	 */
	private String version;
	/**
	 * The name of the server we are connected to.
	 */
	private String servername;
	private String clientName;

	private Set<String> commands = new HashSet<String>();
	
	private List<Module> modules = new ArrayList<Module>();
	
    /**
     * Set to true in unit test context
     */
    private boolean testing = false;

    /**
	 * The charset the bot is currently using
	 */
	private Charset charset = Charset.forName("UTF-8");
	
	public String getBotname() {
		return botname;
	}

	public void setBotname(String botname) {
		this.botname = botname;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getServername() {
		return servername;
	}

	public void setServername(String servername) {
		this.servername = servername;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public Set<String> getCommands() {
		return commands;
	}

	public void setCommands(Set<String> commands) {
		this.commands = commands;
	}

	public List<Module> getModules() {
		return modules;
	}
	
	public void rebuildCommands() {
		BotStats.getInstance().setCommands( new HashSet<String>() );
		for(Module mod:getModules()) {
			addCommands(mod.getCommands());
		}
	}
	
	public boolean isLoadedCommand(String s) {
		return StringUtil.stringInArray(s, getCommands().toArray(new String[0])) ;
	}
	
	public void addCommands(String[] commands) {
		BotStats.getInstance().getCommands().addAll(Arrays.asList( commands ));
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
	}
	
	public void addModule(Module module) {
		modules.add(module);
	}
	
	public void removeModule(Module module) {
		modules.remove(module);
		
		rebuildCommands();
	}
	
	/**
	 * @return All the module names of all loaded modules
	 */
	public String[] getModuleNames() {
		Module mod;
		String[] modNames = new String[modules.size()];
		for(int i=0;i<modules.size();i++) {
			mod = modules.get(i);
			modNames[i] = mod.getClass().getName();
		}
		return modNames;
	}

	public boolean isTesting() {
		return testing;
	}

	public void setTesting(boolean testing) {
		this.testing = testing;
	}

	public String getCONFIG_FILE() {
		return CONFIG_FILE;
	}

	public synchronized String[] getChannels() {
		Object[] ob = channels.toArray();
		String[] chans = new String[ob.length];
		for (int i = 0; i < ob.length; i++) {
			chans[i] = (String) ob[i];
		}
		return chans;
	}

	public synchronized void addChannel(String chan) {
		channels.add(chan);
	}

	public synchronized void removeChannel(String chan) {
		channels.remove(chan);
	}

	public synchronized boolean containsChannel(String chan) {
        return channels.contains(chan);
		}

	public boolean isValidChannelName(String chan) {
        return (chan.startsWith("#") || chan.startsWith("+") || chan.startsWith("&")) && !(chan.matches(":") || chan.matches(",") || chan.matches("\u0007") || chan.matches(" "));
		}

	/**
	 * Call to set the charset in use by goat
	 * @param charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
		goat.Goat.sc.setCharset(charset);
	}
	
	/**
	 * Get the currently used charset from here
	 * @return
	 */
	public Charset getCharset() {
		return charset;
	}
	
	
	public void readConfFile() {
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
