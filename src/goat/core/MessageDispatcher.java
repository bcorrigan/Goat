package goat.core;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * <p>Takes Messages off the inqueue and dispatches them to appropriate Modules</p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class MessageDispatcher {
    private MessageQueue inqueue;
    private ModuleController modController;

	/**
	 *
	 * @param inqueue The inqueue to monitor
	 * @param modController The modController containing modules to dispatch messages to.
	 */

    public MessageDispatcher(MessageQueue inqueue, ModuleController modController) {
        this.inqueue = inqueue;
        this.modController = modController;
        monitor();
    }
    /**
	 * Endlessly monitors the inqueue, and dispatches messages as they appear.
	 */
	private void monitor() {
        while(true) {
            if(inqueue.hasNext()) {
                Message msg = inqueue.dequeue();
                processMessage(msg);
            }
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
    private void processMessage(Message msg) {
        Iterator it = modController.iterator();
        ArrayList modulesWantingAll = new ArrayList(),
				  modulesWantingSome = new ArrayList(),
				  modulesWantingOne = new ArrayList();
		Module mod;
		while(it.hasNext()) {
			mod = (Module) it.next();
			if(mod.messageType() == Module.WANT_ALL_MESSAGES)
				modulesWantingAll.add(mod);
			else if(mod.messageType() == Module.WANT_UNCLAIMED_MESSAGES)
				modulesWantingSome.add(mod);
			else
				modulesWantingOne.add(mod);
		}

		it = modulesWantingAll.iterator();
		while(it.hasNext()) {
			mod = (Module) it.next();
			sendIfChannelsMatch(msg, mod);
		}

		it = modulesWantingOne.iterator();
		boolean used = false;
		while(it.hasNext()) {
			mod = (Module) it.next();
			String[] commands = mod.getCommands();
			for (int j = 0; j < commands.length; j++)
				if (commands[j].equals(msg.modCommand)) {
					sendIfChannelsMatch(msg, mod);
					used = true;
				}
		}

		if (!used) {
			it = modulesWantingSome.iterator();
			while(it.hasNext()) {
				mod = (Module) it.next();
				sendIfChannelsMatch(msg, mod);
			}
		}
    }

	private void sendIfChannelsMatch(Message msg, Module mod) {
		if (mod.inAllChannels == true || (msg.isPrivate & mod.wantsPrivate))
			sendMessage(msg, mod);
		else {
			ArrayList channels = mod.getChannels();
			Iterator it = channels.iterator();
			String chan;
			while (it.hasNext()) {
				chan = (String) it.next();
				if (chan.equals(msg.channame))
					sendMessage(msg, mod);
			}
		}

    }

	private void sendMessage(Message msg, Module mod) {
		if (msg.isPrivate) {
			mod.processPrivateMessage(msg);
		} else if (msg.isCTCP) {
			mod.processOtherMessage(msg);
		} else {
			mod.processChannelMessage(msg);
		}
	}

}
