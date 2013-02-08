package goat;

import goat.core.*;
import goat.module.Core;
import goat.module.ModuleCommands;
import static goat.util.Passwords.*;

import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class Goat {
    private static boolean showhelp;
    public static LinkedBlockingQueue<Message> inqueue = new LinkedBlockingQueue<Message>();
    public static LinkedBlockingQueue<Message> outqueue = new LinkedBlockingQueue<Message>();
    public static ModuleController modController = new ModuleController() ;
    public static String[] argv = {""};
    public static ServerConnection sc;

    private static Users users = new Users() ;

    public static void main(String[] args) {
        argv=args;
        new Goat() ;

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

        System.out.print("Connecting to " + BotStats.getInstance().getServername() + " ... ");
	sc = new ServerConnection(BotStats.getInstance().getServername()); //lets init the connection..
	System.out.println("connected.\n");
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
		BotStats.getInstance().setBotname(arg);
		state = 0;
		break;

	    case 2:
		BotStats.getInstance().addChannel(arg);
		state = 0;
		break;

	    case 3:
		BotStats.getInstance().setServername(arg);
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
	Class<?>[] defaultModules = {
	    goat.module.CTCP.class,
	    goat.module.ModuleCommands.class,
	    goat.module.NickServ.class,
	    goat.module.Help.class,
	    goat.module.Auth.class,
	    goat.module.Core.class,
	    goat.module.Users.class,
	    goat.module.ServerCommands.class
	} ;
	try {
	    for(int i=0; i<defaultModules.length; i++)
		modController.loadInAllChannels(defaultModules[i]);
	    ModuleCommands moduleCommands = (ModuleCommands) modController.getLoaded("ModuleCommands");
	    moduleCommands.modControl = modController;
	    moduleCommands.inAllChannels = true;
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	} catch (InstantiationException e) {
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
		BotStats.getInstance().setVersion( "r" + line );
		br.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		if(BotStats.getInstance().getVersion()==null)
		    BotStats.getInstance().setVersion("unknown");
	    }
	} else {
	    BotStats.getInstance().setVersion("unknown") ;
	}

	String nick = getPassword("irc.nick");
	BotStats.getInstance().setBotname(nick);
	BotStats.getInstance().setClientName(nick);
	BotStats.getInstance().setOwner(nick);
	BotStats.getInstance().setServername(getPassword("irc.server"));
    }

    public static Properties getProps() {
	return getPropsFromFile(GOAT_PROPS_FILE) ;
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
