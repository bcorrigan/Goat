package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.ModuleController;
import goat.core.BotStats;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @version <p>Date: 18-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */
public class ModuleCommands extends Module {

	public ModuleController modControl;
	Message m;

	public void processPrivateMessage(Message msg) {
		m = msg;
		if (m.isAuthorised) {
			if (m.modCommand.equals("insmod")) {
				parse();
			} else if (m.modCommand.equals("rmmod")) {
				rmmod(m);
			} else if (m.modCommand.equals("chans")) {
				parse();
			}
		}
		if (m.modCommand.equals("lsmod")) {
			lsmod(m);
		} else if( m.modCommand.equals("showcommands")) {
			String listC = "";
			for( int i=0; i<BotStats.commands.length-1; i++ )
				listC += BotStats.commands[i] + ", ";
			listC += BotStats.commands[ BotStats.commands.length-1 ] + ".";
			m.createPagedReply(listC).send();
		}
	}

	private void parse() {
		String[] args = m.modTrailing.split(" ");
		ArrayList chans = new ArrayList();
		String moduleName;
		if (args.length == 0) {
			m.createReply("You must specify some arguments.\n " +
										"Format: " + m.modCommand + " <moduleName> <channel1> <channel2> .. [ALL].\n " +
										"ALL means the module will be active for all channels.").send();
			return;
		}
		moduleName = args[0];
		for (int i = 1; i < args.length; i++) {
			if (args[i].startsWith("#") & args[i].length() > 1 || args[i].toLowerCase().equals("all"))
				chans.add(args[i]);
			else {
				m.createReply("You have not specified proper channels in the arguments.").send();
				return;
			}
		}
		if(m.modCommand.equals("insmod"))
			insmod(moduleName, chans);
		else {
			chans(moduleName, chans);
		}
	}

	private void chans(String moduleName, ArrayList chans) {
		Module mod = modControl.get(moduleName);
		if(mod!=null) {
			setChans(chans, mod);
			m.createReply("Modified registered channels of " + mod.getClass().getName()).send();
		} else {
			m.createReply("Could not modify registered channels of '" + moduleName + "'. That module does not exist or is not loaded.").send();
		}
	}

	private void setChans(ArrayList chans, Module mod) {
		Iterator it = chans.iterator();
		String chan;
		mod.inAllChannels = false;
		while (it.hasNext()) {
			chan = (String) it.next();
			if (chan.toLowerCase().equals("all"))
				mod.inAllChannels = true;
			else {
				mod.addChannel(chan);
			}
		}
	}

	public void processChannelMessage(Message m) {
		if (m.modCommand.equals("lsmod")) {
			lsmod(m);
		}
	}

	private void insmod(String moduleName, ArrayList chans) {
		String response = "" ;
		try {
			Module mod = modControl.load(moduleName);
			if(mod!=null) {
				setChans(chans, mod);
				response = "Module '" + mod.getClass().getName() + "' successfully loaded.";
			} else
				response = "Module '" + moduleName + "' is already loaded!";
		} catch (IllegalAccessException e) {
			response = "IllegalAccessException: Module " + moduleName + " could not be loaded.";
		} catch (InstantiationException e) {
			response = "InstantiationException: Module " + moduleName + " could not be instantiated.";
		} catch (ClassNotFoundException e) {
			response = "ClassNotFoundException: Module " + moduleName + " could not be found.";
		} catch (ClassCastException e) {
			response = "ClassCastException: Module " + moduleName + " is not an instance of Module and is thereforenot a viable module for " + BotStats.botname + '.';
		} catch (NoClassDefFoundError e) {
			response = "NoClassDefFoundError: Module " + moduleName + " not found.";
		}
		
		/*We check the message before sending a response because insmod() is called many times
		 *  from within the goat when goat starts up, and those calls are via a Message object
		 *  that does not have a valid replyTo field.  So goat was sending messages to the IRC
		 *  server addressed to "", and the server was sending back error messages, wasting 
		 *  everyone's time and precious computational fluids.
		 */  
		if (m.replyTo.equals(""))
			System.out.println(response) ;
		else
			m.createReply(response).send() ;
	}

	private void rmmod(Message m) {
		if (m.modTrailing.trim().equalsIgnoreCase("ModCommands")) {
			m.createReply("ModuleCommands says:  I won't remove myself!").send() ;
			return ;
		}
		if (modControl.unload(m.modTrailing.trim()))
			m.createReply("Successfully removed module '" + m.modTrailing.trim() + "'!").send();
		else
			m.createReply("Module not found: '" + m.modTrailing.trim() + '\'').send();
	}

	private void lsmod(Message m) {
		String[] mods = modControl.lsmod();
		String modLine = "Loaded modules: " + mods[0];
		for (int i = 1; i < mods.length; i++)
			modLine += ", " + mods[i];
		modLine = modLine.replaceAll("goat.module.", "") ;
		modLine += ".";
		m.createPagedReply(modLine).send();
	}



	public static String[] getCommands() {
		return new String[]{"lsmod", "rmmod", "insmod", "chans", "showcommands"};
	}


}
