package goat.core;

import goat.Goat;
import goat.util.Pager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class Message {
    protected static ConcurrentHashMap<String, Pager> pagerCache = new ConcurrentHashMap<String, Pager>();

    /**
     * The outqueue instance for sending messages
     */
    private static LinkedBlockingQueue<Message> outqueue = Goat.outqueue;

    /**
     * The hostmask of the sending prefix.
     * <p/>
     * This is the hostmask of the person who sent the message.  Not to be confused with the nick, which is the other part of the prefix.
     */
    protected String hostmask = "";
    protected ArrayList<String> words;
    /**
     * True if this message is sent by the owner.
     */
    private boolean isAuthorised;
    /**
     * The prefix of a message. Usually the hostmask who sent the message. The sender field is a substring of this.
     */
    private String prefix = "";
    /**
     * The content of a message. Often a line of chat.
     * <p/>
     * In the case of PRIVMSG, this is the chat line.
     */
    private String trailing = "";
    /**
     * The first word of the content of the message - eg the "command" that modules may be interested in
     */
    private String modCommand = "";
    /**
     * All the trailing part of the message beyond the first word which may be the module command
     */
    private String modTrailing = "";
    /**
     * The command embodied by a message. Typical commands include PRIVMSG (most user communication), NOTICE (communication
     * that must not be automatically responded to), JOIN (join a channel), MODE (change the status of something), and PART
     * (leave a channel).
     */
    private String command = "";
    /**
     * The parameters of the message command. Often the addressee of a communication. For PRIVMSG, this is either a channel
     * or the user receiving the message.
     */
    private String params = "";
    /**
     * The nickname part of the sending prefix.
     * <p/>
     * This is the nick of the person who sent the message. Not to be confused with the hostmask, which contains other
     * details.
     */
    private String sender = "";
    /**
     * Whether this message was sent one to one. False if sent to a channel, true if only to one user.
     */
    private boolean isPrivate;
    /**
     * The Channel name
     */
    private String channame = "";
    /**
     * Where replies to this message should be sent.  Should be equal to sender if isPrivate is true, otherwise equal to channame.  Changing this will change the behavior of createReply() and createPagedReply().
     */
    private String replyTo = "";
    /**
     * If goat has been directly addressed, this is true.
     * That is to say, if the user has said "goat, blah blah blah" it is true,
     * but if they have said merely "blah blah blah" it is false
     */
    private boolean directlyAddressed;

    public static boolean hasNextPage(String key) {
        boolean ret = false;
        synchronized (pagerCache) {
            if (pagerCache.containsKey(key)) {
                Pager pager = pagerCache.get(key);
                if (pager.isEmpty())
                    pagerCache.remove(key);
                else
                    ret = true;
            }
        }
        return ret;
    }

    public static String nextPage(String key, String messageCommand) {
        String ret = "";
        if (Message.hasNextPage(key))
            synchronized (pagerCache) {
                Pager pager = pagerCache.get(key);
                ret = pager.getNext(messageCommand, key);
                if (pager.isEmpty())
                    pagerCache.remove(key);
            }
        return ret;
    }

    /**
     * Sends a generic message using the current connection.
     */
    public static void send(Message m) {
        // Do a little sanity checking and final adjusting before we queue this outgoing message
        if (m.getCommand().equalsIgnoreCase("PRIVMSG") || m.getCommand().equalsIgnoreCase("NOTICE")) {
            if (0 == m.getParams().indexOf("#"))
                m.setChanname(m.getParams());
            else if (m.getParams().equals("")) {
                System.err.println(m.getCommand() + " message has no valid recipient; not sending:");
                System.err.println("   " + m.getCommand() + " " + m.getParams() + " | " + m.getTrailing());
                return;
            }
        }
        if (m.getSender().equals("")) {
            m.setSender(BotStats.getInstance().getBotname());
        }
        Message.outqueue.add(m);
    }

    byte[] toByteArray() {
        String message = (getPrefix().length() > 0 ? ':' + getPrefix() + ' ' : "") + getCommand() + (getParams().length() > 0 ? " " : "") + getParams() + (getTrailing().length() > 0 ? " :" + getTrailing() : "");

        char[] chars = message.toCharArray();

        byte[] bytes = new byte[chars.length + 2];

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != '\n')
                bytes[i] = (byte) chars[i];
            else
                bytes[i] = 0x20;
        }

        bytes[chars.length] = 0x0D;
        bytes[chars.length + 1] = 0x0A;

        return bytes;
    }

    public String toString() {
        return ((getPrefix().length() > 0 ? ':' + getPrefix() + ' ' : "") + getCommand() + (getParams().length() > 0 ? " " : "") + getParams() + (getTrailing().length() > 0 ? " :" + getTrailing() : ""));
    }

    /**
     * <P>Gets the word at the given position from the decoded message.</P>
     * <p/>
     * <P>The parsing is simplistic, and disregards punctuation, except apostrophes.</P>
     * <p/>
     * <P>eg. <CODE>Hello! Aren't you Dave?</CODE> would give the words:</P>
     * <p/>
     * <OL><LI>Hello<LI>Aren't<LI>you<LI>Dave</OL>
     *
     * @param index The index of the word to get, starting at 0.
     * @return The word at the specified position, if one, or an empty string if none.
     */
    public String getWord(int index) {
        if (words == null || index > words.size() - 1)
            return "";

        return (String) words.get(index);
    }

    /**
     * Creates a reply to a PRIVMSG with a NOTICE. <P>Using this is recommended for three reasons - <OL><LI>REplied with a
     * NOTICE (recommended) <LI>Makes sure that you can't reply to a NOTICE <LI>Always replies to the correct place - the
     * channel if the request came from the channel or the user if it was a private message </OL></P>
     *
     * @param trailing The message to send to the person.
     */
    public Message createReply(String trailing) {
        IrcMessage ret;
        if (getCommand().equals("PRIVMSG"))
            ret = Message.createPrivmsg(getReplyTo(), trailing);
        else
            ret = new IrcMessage("", "", "", ""); //hopefully this will be accepted and ignored
        return ret;
    }

    /**
     * Just reply directly, lets not muck about with this m.createReply("blah").send() business
     *
     * @param trailing
     */
    public void reply(String trailing) {
        createPagedReply(trailing).send();
    }

    /**
     * obsolete, use reply()
     * Just do a paged reply directly, lets not muck about with this m.createPagedReply("blah").send() business
     *
     * @param trailing
     */
    public void pagedReply(String trailing) {
        createPagedReply(trailing).send();
    }

    /**
     * Creates a new paged reply, using createReply(), and initializes the pager cache with the supplied string
     *
     * @param trailing The text to be paged and sent
     * @return a message containing the first chunk of paged text, which the caller will most likely want to send()
     */
    public Message createPagedReply(String trailing) {
        IrcMessage ret;
        if (getCommand().equals("PRIVMSG"))
            ret = Message.createPagedPrivmsg(getReplyTo(), trailing);
        else
            ret = new IrcMessage("", "", "", ""); //hopefully this will be accepted and ignored
        return ret;
    }

    /**
     * Is there more text in the pager buffer for the current channel/nick?
     *
     * @return true if there's more text to be had.
     */
    public boolean hasNextPage() {
        return Message.hasNextPage(getReplyTo());
    }

    /**
     * returns a reply message via createReply containing the next page of text, if any, from the pager cache for the current channel/nick (ie, "params")
     *
     * @return aforesaid message, if there is more text in the buffer, else an empty message.
     */
    public Message createNextPage() {
        IrcMessage ret = new IrcMessage("", "", "", "");
        synchronized (pagerCache) {
            if (hasNextPage())
                ret = createReply(Message.nextPage(getReplyTo(), getCommand()));
        }
        return ret;
    }

    /**
     * Creates a reply to a PRIVMSG with any message type. <P>Using this is recommended for two reasons - <OL><LI>Makes
     * sure that you can't reply to a NOTICE <LI>Always replies to the correct place - the channel if the request came from
     * the channel or the user if it was a private message </OL></P>
     *
     * @param command  The type of message to send (usually PRIVMSG or NOTICE)
     * @param trailing The message to send.
     */
    public Message createReply(String command, String trailing) {
        if (!this.getCommand().equals("PRIVMSG")) {
            return new IrcMessage("", "", "", ""); //hopefully this will be accepted and ignored
        }
        return new IrcMessage("", command, getReplyTo(), trailing);
    }

    /**
     * Sends this message
     */
    public void send() {
        // copy to console
        // System.out.println("(goat): " + this.trailing) ;
        Message.send(this);
    }

    public String getChanname() {
        return channame;
    }

    protected void setChanname(String channame) {
        this.channame = channame;
    }

    public String getCommand() {
        return command;
    }

    protected void setCommand(String command) {
        this.command = command;
    }

    public boolean isDirectlyAddressed() {
        return directlyAddressed;
    }

    protected void setDirectlyAddressed(boolean directlyAddressed) {
        this.directlyAddressed = directlyAddressed;
    }

    public String getModCommand() {
        return modCommand;
    }

    protected void setModCommand(String modCommand) {
        this.modCommand = modCommand;
    }

    public String getParams() {
        return params;
    }

    protected void setParams(String params) {
        this.params = params;
    }

    public String getPrefix() {
        return prefix;
    }

    protected void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSender() {
        return sender;
    }

    protected void setSender(String sender) {
        this.sender = sender;
    }

    public String getTrailing() {
        return trailing;
    }

    protected void setTrailing(String trailing) {
        this.trailing = trailing;
    }

    public boolean isAuthorised() {
        return isAuthorised;
    }

    protected void setAuthorised(boolean isAuthorised) {
        this.isAuthorised = isAuthorised;
    }

    public String getModTrailing() {
        return modTrailing;
    }

    protected void setModTrailing(String modTrailing) {
        this.modTrailing = modTrailing;
    }

    public String getReplyTo() {
        return replyTo;
    }

    protected void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public ArrayList<String> getWords() {
        return words;
    }

    protected void setWords(ArrayList<String> words) {
        this.words = words;
    }

    protected void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String dataDump() {
        String ret = toString();
        if (!"".equals(channame))
            ret += "\n   channame:           " + channame;
        if (!"".equals(command))
            ret += "\n   command:            " + command;
        if (directlyAddressed)
            ret += "\n   directlyAddressed.";
        if (!"".equals(hostmask))
            ret += "\n   hostmask            " + hostmask;
        if (isAuthorised)
            ret += "\n   isAuthorised.";
        if (isPrivate)
            ret += "\n   isPrivate.";
        if (!"".equals(modCommand))
            ret += "\n   modCommand:          " + modCommand;
        if (!"".equals(modTrailing))
            ret += "\n   modTrailing:         " + modTrailing;
        if (!"".equals(params))
            ret += "\n   params:              " + params;
        if (!"".equals(prefix))
            ret += "\n   prefix:              " + prefix;
        if (!"".equals(replyTo))
            ret += "\n   replyTo:             " + replyTo;
        if (!"".equals(sender))
            ret += "\n   sender:              " + sender;
        if (!"".equals(trailing))
            ret += "\n   trailing:            " + trailing;
        if (words != null && !words.isEmpty()) {
            ret += "\n   words:              ";
            for (String w : words)
                ret += " " + w;
        }
        return ret;
    }
}
