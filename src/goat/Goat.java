package goat;

import goat.core.*;
import goat.module.Core;
import goat.module.ModuleCommands;

import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

public class Goat {
	private static boolean showhelp;
	public static MessageQueue inqueue = new MessageQueue();
	public static MessageQueue outqueue = new MessageQueue();
	public static ModuleController modController = new ModuleController() ;
    public static String[] argv = {""};
    public static ServerConnection sc;
    private static Users users = new Users() ;
    
    public static final String GOAT_PROPS_FILE = "config/goat.properties" ;
    public static final String GOAT_PASSWORDS_FILE = "config/passwords.properties" ;

	public static void main(String[] args) {
        argv=args;
        new Goat() ;
        /*
         *  The following was all used to test new modController stuff, it should be 
         *  refuctored into a junit thingy.  Some day.
         *
        ModuleController mc = modController ;
        try {
        	mc.load("Say") ;
        	System.out.println(Arrays.toString(mc.lsmod())) ;
        	mc.load("Core") ;
        	mc.load("Weather") ;
        	System.out.println(Arrays.toString(mc.lsmod())) ;
        	System.out.println(Arrays.toString(mc.getLoadedCommands())) ;
        	System.out.println() ;
        	System.out.println(Arrays.toString(mc.getAllModules())) ;
        	System.out.println(Arrays.toString(mc.getAllCommands())) ;
        } catch (Exception e) {
        	System.err.println("Oops.") ;
        	e.printStackTrace() ;
        }
        */
        System.exit(0) ;
	}

	public Goat() {
		Locale.setDefault(Locale.UK);  // goat is UKian, damn it.
      setDefaultStats();
		parseArgs(argv);
        if (showhelp) {
			showHelp();
            System.exit(0);
        }

		sc = new ServerConnection(BotStats.servername); //lets init the connection..
		loadDefaultModules(modController);
		try {
			Thread.sleep(100);   //lets give the logon a chance to progress before adding messages to queues
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		new MessageDispatcher(modController);
	}

	private static void parseArgs(String[] args) {
		int state = 0;

        for (String arg : args) {
            switch (state) {
                //start case
                case 0:
                    if (arg.equals("-name")) {
                        state = 1;
                    } else if (arg.equals("-channel")) {
                        state = 2;
                    } else if (arg.equals("-host")) {
                        state = 3;
                    } else if (arg.equals("-help")) {
                        showhelp = true;
                    } else {
                        System.out.println("Illegal argument.");
                        showhelp = true;
                    }
                    break;

                case 1:
                    BotStats.botname = arg;
                    state = 0;
                    break;

                case 2:
                    BotStats.addChannel(arg);
                    state = 0;
                    break;

                case 3:
                    BotStats.servername = arg;
                    state = 0;
                    break;
            }
        }

        //if we are still waiting for an argument
		if (state != 0) {
			System.out.println("Missing argument.");
			showhelp = true;
		}
	}

	private static void showHelp() {
		System.out.println("Usage: java Goat [-name <name>][-host <host>][-channel <channel>]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -name <name>         Changes the bot's default name [default: goat]");
		System.out.println("  -channel <#channel>  Changes the bot's default channel [default: #jism]");
		System.out.println("  -host <host>         Sets which host to connect to [default: irc.slashnet.org]");
	}

	private void loadDefaultModules(ModuleController modController) {
		try {
			modController.load("CTCP");
			modController.load("ModuleCommands");
            modController.load("NickServ");
			ModuleCommands moduleCommands = (ModuleCommands) modController.get(1);
			moduleCommands.modControl = modController;
			moduleCommands.inAllChannels = true;
			modController.load("Help");
			if (modController.get("Help") != null)
				modController.get("Help").inAllChannels = true ;
			modController.load("Auth");
			modController.load("Core");
			Core core = (Core) modController.get(5);
			core.inAllChannels = true;
			modController.load("Users");
			if (modController.get("Users") != null)
				modController.get("Users").inAllChannels = true ;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void setDefaultStats() {
		ClassLoader goatClassLoader = ClassLoader.getSystemClassLoader();
		URL goatRevisionResource = goatClassLoader.getResource("goatRevision");
		if (goatRevisionResource != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(goatClassLoader.getResourceAsStream("goatRevision")));
			String line;
			try {
				line = br.readLine();
				line = line.replaceAll("(?i)[a-z:]", "").trim();
				BotStats.version = "r" + line;				
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(BotStats.version==null)
					BotStats.version = "unknown";
			}
		} else {
			BotStats.version = "unknown" ;
		}

		BotStats.botname = "goat";
		BotStats.clientName = "goat";
		BotStats.owner = "rs";
		BotStats.servername = "irc.slashnet.org";

	}

	public static Properties getProps() {
		return getPropsFromFile(GOAT_PROPS_FILE) ;
	}
	
	public static Properties getPasswords() {
		return getPropsFromFile(GOAT_PASSWORDS_FILE) ;
	}
	
	private static Properties getPropsFromFile(String filename) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(filename));
		} catch (IOException e) {
			System.err.println("WARNING:  Could not load properties from file \"" + filename + "\"") ;
			e.printStackTrace() ;
		}
		return props;
	}
	
	public static Users getUsers() {
		if (null == users) {
			users = new Users() ;
			return users ;
		} else {
			return users;
		}
	}
}
