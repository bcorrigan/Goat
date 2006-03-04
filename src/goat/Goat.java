package goat;

import goat.core.*;
import goat.module.Core;
import goat.module.ModuleCommands;
import java.io.*;

public class Goat {
	private static boolean showhelp;
	public static MessageQueue inqueue = new MessageQueue();
	public static MessageQueue outqueue = new MessageQueue();
	public static ModuleController modController = new ModuleController() ;
    public static String[] argv = {""};
    public static ServerConnection sc;

	public static void main(String[] args) {
        argv=args;
		new Goat();
	}

	public Goat() {
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

		for (int i = 0; i < args.length; i++) {
			switch (state) {
				//start case
				case 0:
					if (args[i].equals("-name")) {
						state = 1;
					} else if (args[i].equals("-channel")) {
						state = 2;
					} else if (args[i].equals("-host")) {
						state = 3;
					} else if (args[i].equals("-help")) {
						showhelp = true;
					} else {
						System.out.println("Illegal argument.");
						showhelp = true;
					}
					break;

				case 1:
					BotStats.botname = args[i];
					state = 0;
					break;

				case 2:
					BotStats.addChannel(args[i]);
					state = 0;
					break;

				case 3:
					BotStats.servername = args[i];
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
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void setDefaultStats() {
		if (getClass().getClassLoader().getResource("goatRevision") != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("goatRevision")));
			String line;
			try {
				line = br.readLine();
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
}
