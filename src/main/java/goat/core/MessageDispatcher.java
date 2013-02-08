package goat.core;

import goat.Goat;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Takes Messages off the inqueue and dispatches them to appropriate Modules</p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class MessageDispatcher {
    private static LinkedBlockingQueue<Message> inqueue = Goat.inqueue;

	/**
	 *
	 * @param modController The modController containing modules to dispatch messages to.
	 */

    public MessageDispatcher(ModuleController modController) {
        monitor();
    }
    /**
	 * Endlessly monitors the inqueue, and dispatches messages as they appear.
	 */
	private void monitor() {
        while(true) {
            try {
                Message msg = inqueue.take();
                dispatchMessage(msg);
            } catch (InterruptedException ie) {}
		}

    }
	
	private void dispatchMessageToAll(Message msg) {
		List<Module> modules = BotStats.getInstance().getModules();
		for( Module mod : modules )
			sendMessage(msg, mod);
	}
	
    /*
     * 1) If the message is other type, just send it to all modules
	 * 2)Make a Collection of all Modules that are interested in definite commands,
	 *    and another of modules that are interested in msgs other modules aren't interested
	 *    in, and another of modules interested in *all* msgs
	 * 3)Find out what channel the Message is from
	 * 4)Get channels from the module
	 * 5)Check if message is private, if so..
	 * 6)	Check Message.modCommand, compare with module's commands..
	 * 7)		If a match, send to module, & save the fact that a module was "interested"
	 * 8)Check the message's channel, compare with module's
	 * 9)	do 4&5
	 * 10)If nothing was interested in msg, send to all modules that have wantsUnclaimedmessages set
	 * 11)Send msg to all modules interested in *everything*
	 *
	 */
    private void dispatchMessage(Message msg) {
    	//if the message is "other" we always want to send it nomatter what - to every module
    	if( msg.isCTCP()||!msg.getCommand().equals("PRIVMSG") ) {
    		dispatchMessageToAll(msg);
    		return;
    	}
    	
        List<Module> modules = BotStats.getInstance().getModules();
        ArrayList<Module> modulesWantingAll = new ArrayList<Module>(),
				  modulesWantingSome = new ArrayList<Module>(),
				  modulesWantingOne = new ArrayList<Module>();

		for(Module mod : modules) {
			if(mod.messageType() == Module.WANT_ALL_MESSAGES)
				modulesWantingAll.add(mod);
			else if(mod.messageType() == Module.WANT_UNCLAIMED_MESSAGES)
				modulesWantingSome.add(mod);
			else
				modulesWantingOne.add(mod);
		}

		for(Module mod : modulesWantingAll) 
			sendIfChannelsMatch(msg, mod);
		

		boolean used = false;
		for(Module mod : modulesWantingOne) {
			String [] commands = mod.getCommands();
            for (String command : commands)
                if (command.equalsIgnoreCase(msg.getModCommand())) {
                    sendIfChannelsMatch(msg, mod);
                    used = true;
                }
        }

		if (!used) 
			for(Module mod : modulesWantingSome) 
				sendIfChannelsMatch(msg, mod);		
    }

	private void sendIfChannelsMatch(Message msg, Module mod) {
		if (mod.inAllChannels || msg.isPrivate() & mod.wantsPrivate)
			sendMessage(msg, mod);
		else {
			ArrayList<String> channels = mod.getChannels();
			Iterator<String> it = channels.iterator();
			String chan;
			while (it.hasNext()) {
				chan = (String) it.next();
				if (chan.equals(msg.getChanname()))
					sendMessage(msg, mod);
			}
		}
    }
	
	private void sendMessage(Message msg, Module mod) {
		mod.dispatchMessage(msg);
	}

}
