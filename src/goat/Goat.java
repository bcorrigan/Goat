package goat;

import goat.core.*;
import goat.module.ModuleCommands;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Goat {
	private static boolean showhelp;
    public static MessageQueue inqueue = new MessageQueue();
    public static  MessageQueue outqueue = new MessageQueue();

	public static void main(String[] args) {
		setDefaultStats();
        parseArgs(args);
        if (showhelp)
            showHelp();
        else {
            Goat goat = new Goat();
        }
	}

    public Goat() {
        ServerConnection sc = new ServerConnection(BotStats.servername); //lets init the connection..
        ModuleController modController = new ModuleController();
        loadDefaultModules(modController);
		try {
			Thread.sleep(100);   //lets give the logon a chance to progress before adding messages to queues
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		loadInitScript();
		MessageDispatcher msgDispatch = new MessageDispatcher(modController);
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
		System.out.println("  -host <host>         Sets which host to connect to [default: coruscant.slashnet.org]");
	}

    private void loadDefaultModules(ModuleController modController) {
		try {
			modController.load("CTCP");
			modController.load("ModuleCommands");
			ModuleCommands moduleCommands = (ModuleCommands) modController.get(1);
			moduleCommands.modControl = modController;
			moduleCommands.inAllChannels = true;
			modController.load("Auth");
			modController.load("Help");
			modController.load("Core");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void loadInitScript() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("goatrc"));
			String lineIn;

			while((lineIn=in.readLine())!=null) {
				Message m = new Message("", "", "", "");
				m.isAuthorised = true;
				m.isPrivate = true;
				if(lineIn.startsWith("#")) {
					continue;		//so the file can be commented :-)
				}
				String[] words = lineIn.split(" ");
				m.modCommand = words[0];
				for(int i=1;i<words.length;i++) {
					m.modTrailing += words[i] + ' ';
				}
				m.command="PRIVMSG";
				inqueue.enqueue(m);
			}
			in.close();

		} catch (FileNotFoundException e) {
			System.out.println("goatrc not found, starting anyway..");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	private static void setDefaultStats() {
		BotStats.botname="goat";
 		BotStats.addChannel("#gayness");
		BotStats.clientName = "goat";
		BotStats.owner = "bc";
		BotStats.servername = "coruscant.slashnet.org";
		BotStats.version = "Goat 2.0 Alpha Enterprise Edition";
	}
}
