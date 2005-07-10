package goat.core;

import goat.Goat;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * <p>Takes Messages off the inqueue and dispatches them to appropriate Modules</p>
 * @version <p>Date: 17-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 *
 */
public class MessageDispatcher {
    private static MessageQueue inqueue=Goat.inqueue;
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
            if(Goat.inqueue.hasNext()) {
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
        //if(msg.isAuthorised)
			//System.out.println("Inbuffer: prefix: " + msg.prefix + " params: " + msg.params + " trailing:" + msg.trailing + " command:" + msg.command + " sender: " + msg.sender +
			//					           "\n    " + "isCTCP:" + msg.isCTCP + " isPrivate:" + msg.isPrivate + " CTCPCommand:" + msg.CTCPCommand + " CTCPMessage:" + msg.CTCPMessage);

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
				if (commands[j].equalsIgnoreCase(msg.modCommand)) {
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
		if (mod.inAllChannels || msg.isPrivate & mod.wantsPrivate)
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
        try {
            if (msg.isCTCP||!msg.command.equals("PRIVMSG")) {
                mod.processOtherMessage(msg);
            } else if (msg.isPrivate) {
                mod.processPrivateMessage(msg);
            } else {
                mod.processChannelMessage(msg);
            }
        } catch(Exception e) {
            msg.createReply( mod.getClass().getName() + " caused an exception: " 
                    + e.getMessage() + ". You will probably want to fix this. Saving stacktrace to a bugfix file.").send();
        }
	}

}
