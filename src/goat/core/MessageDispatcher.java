package goat.core;

import goat.Goat;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Takes Messages off the inqueue and dispatches them to appropriate Modules</p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class MessageDispatcher {
    private static LinkedBlockingQueue<Message> inqueue = Goat.inqueue;
    private ModuleController modController;

	/**
	 *
	 * @param modController The modController containing modules to dispatch messages to.
	 */

    public MessageDispatcher(ModuleController modController) {
        this.modController = modController;
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
    /*
	 * 0)Make a Collection of all Modules that are interested in definite commands,
	 *    and another of modules that are interested in msgs other modules aren't interested
	 *    in, and another of modules interested in *all* msgs
	 * 1)Find out what channel the Message is from
	 * 2)Get channels from the module
	 * 3)Check if message is private, if so..
	 * 4)	Check Message.modCommand, compare with module's commands..
	 * 5)		If a match, send to module, & save the fact that a module was "interested"
	 * 6)Check the message's channel, compare with module's
	 * 7)	do 4&5
	 * 8)If nothing was interested in msg, send to all modules that have wantsUnclaimedmessages set
	 * 9)Send msg to all modules interested in *everything*
	 *
	 */
    private void dispatchMessage(Message msg) {
        Iterator<Module> it = modController.iterator();
        ArrayList<Module> modulesWantingAll = new ArrayList<Module>(),
				  modulesWantingSome = new ArrayList<Module>(),
				  modulesWantingOne = new ArrayList<Module>();
		Module mod;
        //if(msg.isAuthorised)
			//System.out.println("Inbuffer: prefix: " + msg.prefix + " params: " + msg.params + " trailing:" + msg.trailing + " command:" + msg.command + " sender: " + msg.sender +
			//					           "\n    " + "isCTCP:" + msg.isCTCP + " isPrivate:" + msg.isPrivate + " CTCPCommand:" + msg.CTCPCommand + " CTCPMessage:" + msg.CTCPMessage);

		while(it.hasNext()) {
			mod = it.next();
			if(mod.messageType() == Module.WANT_ALL_MESSAGES)
				modulesWantingAll.add(mod);
			else if(mod.messageType() == Module.WANT_UNCLAIMED_MESSAGES)
				modulesWantingSome.add(mod);
			else
				modulesWantingOne.add(mod);
		}

		it = modulesWantingAll.iterator();
		while(it.hasNext()) {
			mod = it.next();
			sendIfChannelsMatch(msg, mod);
		}

		it = modulesWantingOne.iterator();
		boolean used = false;
		while(it.hasNext()) {
			mod = it.next();
			String [] commands = mod.getCommands();
            for (String command : commands)
                if (command.equalsIgnoreCase(msg.getModCommand())) {
                    sendIfChannelsMatch(msg, mod);
                    used = true;
                }
        }

		if (!used) {
			it = modulesWantingSome.iterator();
			while(it.hasNext()) {
				mod = it.next();
				sendIfChannelsMatch(msg, mod);
			}
		}
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
