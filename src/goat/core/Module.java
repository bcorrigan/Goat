package goat.core;

import goat.Goat;

import java.util.ArrayList;

/**
 * The superclass of all Goat modules.
 * @author &copy; Barry Corrigan 2003 All Rights Reserved.
 * @version 1.0
 */

public abstract class Module {

	private ArrayList channels = new ArrayList();

	public boolean inAllChannels = false;
	public boolean wantsPrivate = true;

	public static final int WANT_ALL_MESSAGES = 0;
	public static final int WANT_UNCLAIMED_MESSAGES = 1;
	public static final int WANT_COMMAND_MESSAGES = 2;

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
		channels = new ArrayList();
		for(int i=0; i<channelsArray.length; i++)
			channels.add(channelsArray[i]);
	}

	/**
	 * <p>Gets all the channel names the module receives messages from.</p>
	 * @return An array of the channel names the module receives messages from.
	 */
	public final ArrayList getChannels() {
		return channels;
	}

    /**
     * <p>Returns an array of commands that the module is interested in. </p>
	 *
	 * <p>This is only ever checked if <code>messageType()</code> returns <code>WANT_COMMAND_MESSAGES</code>,
	 * which it does by default. Implementing modules just need to define here what commands their module
	 * is interested in. That is, if someone in an irc channel says..</p>
	 *
	 * <p><code>&lt;qpt&gt; goat, eat it.</code><p>
	 *
	 * <p>..then the modCommand (Message.modCommand) would be "eat", and any module that had <code>getCommands()</code>
	 * return "eat" would receive this line in <code>processChannelMessage()</code> or <code>processPublicMessage()</code>
	 * but not otherwise.</p>
	 *
	 * <p>If "goat, eat it" is privately messaged to the bot, then Message.modCommand will be "eat" as well. However, if
	 * "eat it" is sent as a private message, then Message.modCommand will also be "eat". That is, in the case of private
	 * messages if the bot's name is at the start it is ignored, along with any additional punctuation.</p>
	 *
     * @return An array of commands that the module wants to know about.
     */
    public String[] getCommands() {
		return new String[0];
	}

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
}