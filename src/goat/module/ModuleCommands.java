package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.ModuleController;
import goat.core.BotStats;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @version <p>Date: 18-Dec-2003</p>
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
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
		}

	}

	private void parse() {
		String[] args = m.modTrailing.split(" ");
		ArrayList chans = new ArrayList();
		String moduleName;
		if (args.length == 0) {
			sendMessage(m.createReply("You must specify some arguments.\n " +
										"Format: " + m.modCommand + " <moduleName> <channel1> <channel2> .. [ALL].\n " +
										"ALL means the module will be active for all channels."));
			return;
		}
		moduleName = args[0];
		for (int i = 1; i < args.length; i++) {
			if (args[i].startsWith("#") & args[i].length() > 1 || args[i].toLowerCase().equals("all"))
				chans.add(args[i]);
			else {
				sendMessage(m.createReply("You have not specified proper channels in the arguments."));
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
			sendMessage(m.createReply("Modified registered channels of " + mod.getClass().getName()));
		} else {
			sendMessage(m.createReply("Could not modify registered channels of '" + moduleName + "'. That module does not exist or is not loaded."));
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
		try {
			Module mod = modControl.load(moduleName);
			if(mod!=null) {
				setChans(chans, mod);
				sendMessage(m.createReply("Module '" + mod.getClass().getName() + "' successfully loaded."));
			} else
				sendMessage(m.createReply("Module '" + moduleName + "' is already loaded!"));
		} catch (IllegalAccessException e) {
			sendMessage(m.createReply("IllegalAccessException: Module " + moduleName + " could not be loaded."));
		} catch (InstantiationException e) {
			sendMessage(m.createReply("InstantiationException: Module " + moduleName + " could not be instantiated."));
		} catch (ClassNotFoundException e) {
			sendMessage(m.createReply("ClassNotFoundException: Module " + moduleName + " could not be found."));
		} catch (ClassCastException e) {
			sendMessage(m.createReply("ClassCastException: Module " + moduleName + " is not an instance of Module and is thereforenot a viable module for " + BotStats.botname + "."));
		} catch (NoClassDefFoundError e) {
			sendMessage(m.createReply("NoClassDefFoundError: Module " + moduleName + " not found."));
		}
	}

	private void rmmod(Message m) {
		if (modControl.unload(m.modTrailing.trim()))
			sendMessage(m.createReply("Successfully removed module '" + m.modTrailing.trim() + "'!"));
		else
			sendMessage(m.createReply("Module not found: '" + m.modTrailing.trim() + "'"));
	}

	private void lsmod(Message m) {
		String[] mods = modControl.lsmod();
		String modLine = "Loaded modules: " + mods[0];
		for (int i = 1; i < mods.length; i++)
			modLine += ", " + mods[i];
		modLine += ".";
		sendMessage(m.createReply(modLine));
	}



	public String[] getCommands() {
		return new String[]{"lsmod", "rmmod", "insmod", "chans"};
	}


}
