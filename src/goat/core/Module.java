package goat.core;


import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.reflect.*;

/**
 * The superclass of all Goat modules.
 * @author &copy; Barry Corrigan 2003 All Rights Reserved.
 * @version 1.0
 */

public abstract class Module implements Runnable {

	private ArrayList<String> channels = new ArrayList<String>();

	public boolean inAllChannels;
	public boolean wantsPrivate = true;

	//todo have "other" type always hit processOtherMessage
	public static final int WANT_ALL_MESSAGES = 0;
	public static final int WANT_UNCLAIMED_MESSAGES = 1;
	public static final int WANT_COMMAND_MESSAGES = 2;
	
	/**
	 * An enum representing the scope or context of an instance of a Module.
	 * 
	 * MESSAGE   - a new instance is created for each message handled
	 * 
	 * CHANNEL   - one instance is created for each channel the Module
	 *             is active in; that instance handles all messages in
	 *             its channel
	 *           
	 * SERVER    - One instance is created for each server the bot is connected to;
	 *             that instance handles all messages dispatched from the given
	 *             server to the Module.  (the bot is not currently multi-server
	 *             capable; this makes SERVER equivalent to GLOBAL)
	 *           
	 * GLOBAL    - One instance is created for each running bot; this instance handles
	 * 			   all messages dispatched to the Module from any server.
	 *   
	 * IRC_USER  - (NOT IMPLEMENTED) One instance is created for each IrcUser
	 * 
	 * BOT_USER  - (NOT IMPLEMENTED) One instance is created for each User
	 * 
	 * @author Ed
	 */
	public enum Scope {
		MESSAGE,
		CHANNEL,
		SERVER,
		USER,
		GLOBAL
	}
	
	/**
	 * The scope of your Module instance.  
	 * 
	 * Default is Module.Scope.SERVER
	 * 
	 * @see Scope
	 */
	protected Scope scope = Scope.SERVER;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	/**
	 * <p>Adds a channel to the list of channels the module receives messages from..</p>
	 * @param channelname The name of the channel to be added.
	 */
	public final void addChannel(String channelname) {
		channels.add(channelname);
	}

	/**
	 * <p>Removes a channel from the list of channels the module receives messages from.</p>
	 * @param channelname The name of the channel to be removed.
	 */
	public final void removeChannel(String channelname) {
		channels.remove(channelname);
	}

	/**
	 * <p>Sets the channels the module receives messages from.</p>
	 * @param channelsArray An array of channel names the module receives messages from.
	 */
	public final void setChannels(String[] channelsArray) {
		channels = new ArrayList<String>();
        for (String aChannelsArray : channelsArray) channels.add(aChannelsArray);
    }

	/**
	 * <p>Gets all the channel names the module receives messages from.</p>
	 * @return An array of the channel names the module receives messages from.
	 */
	public final ArrayList<String> getChannels() {
		return channels;
	}
	
	public final boolean inChannel(String channelname) {
		return channels.contains(channelname) ;
	}

    /**
	 * <p>
	 * Returns an array of commands that the module is interested in.
	 * </p>
	 * 
	 * <p>
	 * This is only ever checked if <code>messageType()</code> returns
	 * <code>WANT_COMMAND_MESSAGES</code>, which it does by default.
	 * Implementing modules just need to define here what commands their module
	 * is interested in. That is, if someone in an irc channel says..
	 * </p>
	 * 
	 * <p>
	 * <code>&lt;qpt&gt; goat, eat it.</code>
	 * <p>
	 * 
	 * <p>
	 * ..then the modCommand (Message.modCommand) would be "eat", and any module
	 * that had <code>getCommands()</code> return "eat" would receive this
	 * line in <code>processChannelMessage()</code> or
	 * <code>processPublicMessage()</code> but not otherwise.
	 * </p>
	 * 
	 * <p>
	 * If "goat, eat it" is privately messaged to the bot, then
	 * Message.modCommand will be "eat" as well. However, if "eat it" is sent as
	 * a private message, then Message.modCommand will also be "eat". That is,
	 * in the case of private messages if the bot's name is at the start it is
	 * ignored, along with any additional punctuation.
	 * </p>
	 * 
	 * <p>
	 * NOTE NOTE NOTE: This is almost certainly *not* the method you want. Since
	 * we changed it to a static method, java's (crackheaded) early binding of
	 * static methods mean that if you try to call this in a non-static way on a subclass
	 * of goat.core.Module, <i>you will get the superclass's version</i>.  Use
	 * goat.core.Module.getCommands(Class) instead, e.g., for some module myModule do:
	 * <code>String [] myCommands = Module.getCommands(myModule.getClass()) ;</code>
	 * instead of <code>myCommands = myModule.getCommands() ; </code>
	 * </p>
	 * 
	 * @return An array of commands that the module wants to know about.
	 * @see goat.core.Module.getCommands(Class)
	 */
    public abstract String[] getCommands();

	/**
	 * Called when the bot receives a message which is not a PRIVMSG. Many modules may not need to override the default
	 * implementation, which does nothing.
	 */
	public void processOtherMessage(Message m) {
	}

	/**
	 * Called when the bot receives a Private message. This only a PRIVMSG sent privately (1 to 1) by a user will trigger
	 * this action. You should typically respond directly to the user, if at all.
	 */
	public abstract void processPrivateMessage(Message m);
    
	/**	Called when the bot receives a message which is a Channel PRIVMSG
	 * (ie. a message sent to everyone in a channel by a user).
	 */
	public abstract void processChannelMessage(Message m);
	
	public void dispatchMessage(Message m) {
		if(running) {
			queueIncomingMessage(m);
		} else
			processMessage(m);
	}
	
	public void processMessage(Message m) {
	       try {
	            if (m.isCTCP()||!m.getCommand().equals("PRIVMSG")) {
	                processOtherMessage(m);
	            } else if (m.isPrivate()) {
	                processPrivateMessage(m);
	            } else {
	                processChannelMessage(m);
	            }
	        } catch(Exception e) {
	            e.printStackTrace();
	            m.createReply( this.getClass().getSimpleName() + " caused an exception: " 
	                    + e.getClass().getSimpleName() + ". You will probably want to fix this.").send();
	        }
		
	}
	
	 
	
	protected LinkedBlockingQueue<Message> incomingQueue = new LinkedBlockingQueue<Message>();
	
	public void queueIncomingMessage(Message m) {
		incomingQueue.add(m);
	}
	
	protected boolean stop = false;
	protected boolean running = false;
	
	public boolean isRunning() {
		return running;
	}
	
	public synchronized void stopDispatcher() {
		stop = true;
		
		// send a dummy message through the queue to make sure run() doesn't get stuck
		// waiting on a take() for a message that will never come
		incomingQueue.add(new Message());
	}
	
	public final void run() {
		running = true;
		while (!stop) {
			try {
				Message m = incomingQueue.take();
				if(!stop)  // we do it this in addition to while(!stop) because we might have been stopped while take() was waiting for something new to enter the queue  
					processMessage(m);
			} catch (InterruptedException ie) {}
		}
		running = false;
	}
}
