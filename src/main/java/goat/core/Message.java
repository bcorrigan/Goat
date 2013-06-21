package goat.core;

import goat.Goat;
import goat.util.Pager;
import goat.util.StringUtil;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <P>This class encapsulates an IRC Message. An IRC message is one line of communication, either from the server to the
 * client, or from the client to the server, or between servers.</P>
 * <p/>
 * <P>A message always has the following format</P>
 * <p/>
 * <P><CODE>[:&lt;prefix&gt;] &lt;command&gt; &lt;params&gt; [:&lt;trailing&gt;]</CODE></P>
 * <p/>
 * <P>and the fields in the class represent these parts.</P>
 * <p/>
 * <P>IRC is defined by RFC 1459 (simply search for RFC 1459 to find copies of it), and I will omit a rigourous
 * treatment here (except to say that this class handles converting Java Unicode Strings to the (unspecified but 8-bit)
 * character set for IRC.</P>
 *
 * @author bc & much filched from Daniel Pope's bot & bits from pircbot
 * @version 1.0
 */

public class Message {

    /**
	 * The outqueue instance for sending messages
	 */
	private static LinkedBlockingQueue<Message> outqueue = Goat.outqueue;
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
	 * The hostmask of the sending prefix.
	 * <p/>
	 * This is the hostmask of the person who sent the message.  Not to be confused with the nick, which is the other part of the prefix.
	 */
	private String hostmask = "";

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
	private String replyTo = "" ;

    /**
     * If goat has been directly addressed, this is true.
     * That is to say, if the user has said "goat, blah blah blah" it is true,
     * but if they have said merely "blah blah blah" it is false
     */
    private boolean directlyAddressed;

    /**
	 * CTCP is a slight addition to the protocol. Effectively, this allows extension commands to be supplied, which can be
	 * ignored by the client if not understood. CTCP is usually a PRIVMSG, where the trailing is encapulated by ASCII 0x01,
	 * and the first full word of which is the command.
	 * <p/>
	 * The most common example of CTCP is the CTCP ACTION, which is used by typing /me in most clients.
	 */
	private boolean isCTCP;

	/**
	 * The CTCP command (if isCTCP is true). eg. 'ACTION' for an action/emote
	 */
	private String CTCPCommand = "";

	/**
	 * The CTCP message (if isCTCP is true). eg. 'giggles' in '/me giggles'
	 */
	private String CTCPMessage = "";

	private ArrayList<String> words;

	private static ConcurrentHashMap<String, Pager> pagerCache = new ConcurrentHashMap<String, Pager>() ;


	public Message() {
		// so's we can serialize
	}

	/**
	 * The most low level way of creating a new message, this requires some knowledge of the IRC RFC. prefix can be left
	 * empty (but not null) with most outgoing messages.
	 */
	public Message(String prefix, String command, String params, String trailing) {
		this.setPrefix(prefix);
		this.setCommand(command);
		this.setParams(params);
		this.setTrailing(trailing);
	}

	/**
	 * Creates a new outgoing NOTICE.
	 * <p/>
	 * <P>This type of message should not automatically be replied to.</P>
	 *
	 * @param to      The nick of the person to send to.
	 * @param message The message to send.
	 */
	public static Message createNotice(String to, String message) {
		return new Message("", "NOTICE", to, message);
	}

	/**
	 * Creates a new outgoing PRIVMSG.
	 * <p/>
	 * This message can automatically be replied to. Personally, I'd suggest using this only for socials or to build an IRC
	 * client.
	 *
	 * @param to      The nick of the person to send to.
	 * @param message The message to send.
	 */
	public static Message createPrivmsg(String to, String message) {
		return new Message("", "PRIVMSG", to, message);
	}

	public static synchronized Message createPagedPrivmsg(String to, String message) {
	    Message ret;
	    synchronized (pagerCache) {
	        if(Pager.shouldPaginate(message)) {
	            Pager pager = new Pager(message) ;
	            pagerCache.put(to, pager) ;
	            ret = new Message("", "PRIVMSG", to, pager.getNext("PRIVMSG", to)) ;
	        } else {
	            ret = createPrivmsg(to, message);
	        }
	    }
	    return ret;
	}

	/**
	 * Creates a new outgoing CTCP message.
	 *
	 * @param to          The nick of the person to send to.
	 * @param command     The IRC command (PRIVMSG or NOTICE)
	 * @param CTCPcommand The CTCP command to send. The most common is ACTION.
	 * @param CTCPparams  The parameters of the CTCP command. With an ACTION, this is the action.
	 */
	public static Message createCTCP(String to, String command, String CTCPcommand, String CTCPparams) {
		String payload = "" ;
		if ((null != CTCPparams) && (! CTCPparams.equals("")))
			payload = CTCPcommand + ' ' + CTCPparams ;
		Message m = new Message("", command, to, (char) 0x01 + payload + (char) 0x01);
		m.setCTCPCommand(CTCPcommand);
		if (null != CTCPparams)
			m.setCTCPMessage(CTCPparams);
		else
			m.setCTCPMessage("");
		m.setCTCP(true);
		return m;
	}

	/**
	 * This is the preferred form of parsing a message from the network.
	 */
	public Message(String messagestring) {
		int i, j, k;

		//Perform a preliminary parse to decode the IRC protocol
		if (messagestring.charAt(0) == ':') //prefix form - prefix starts with :
		{
			k = messagestring.indexOf(' ');
			if (k > 0)
				setPrefix(messagestring.substring(1, k));
			i = messagestring.indexOf(' ', k + 1);
			setCommand(messagestring.substring(k + 1, i));
			j = messagestring.indexOf(" :", i);

			if (j == -1)
				setParams(messagestring.substring(i + 1));
			else {
				if (j != i)
					setParams(messagestring.substring(i + 1, j));
				setTrailing(messagestring.substring(j + 2));
			}
		} else    //non prefix form - this doesn't happen in client communications
		{
			i = messagestring.indexOf(' ');
			setCommand(messagestring.substring(0, i));
			j = messagestring.indexOf(" :", i);

			if (j < 0)
				setParams(messagestring.substring(i + 1));
			else {
				if (j != i)
					setParams(messagestring.substring(i + 1, j));
				setTrailing(messagestring.substring(j + 2));
			}
		}

		//Extended parsing decodes the message further.
		String CTCP;

		if (getTrailing() != null && getTrailing().length() > 1 && getTrailing().charAt(0) == 0x01 && getTrailing().charAt(getTrailing().length() - 1) == 0x01) {
			CTCP = getTrailing().substring(1, getTrailing().length() - 1);

			if ((i = CTCP.indexOf(' ')) > -1) {
				setCTCPCommand(CTCP.substring(0, i));
				setCTCPMessage(CTCP.substring(i));
			} else {
				setCTCPCommand(CTCP);
			}

			setCTCP(true);
		} else if (getTrailing() != null) {
			words = new ArrayList<String>(30);

			int state = 0;

			char ch;
			String currentword = "";

			for (i = 0; i < getTrailing().length(); i++) {
				ch = getTrailing().charAt(i);

				switch (Character.getType(ch)) {
					case Character.DECIMAL_DIGIT_NUMBER:
					case Character.LOWERCASE_LETTER:
					case Character.UPPERCASE_LETTER:
						currentword += ch;
						state = 1;
						break;

					default:
						if (ch == '\'') {
							//this case is an exception so we can count apostrophes, etc within the one word.
							currentword += ch;
							state = 1;
							break;
						} else {
							if (state == 1) {
								words.add(currentword);
								currentword = "";
							}
							state = 0;
							break;
						}
				}
			}

			if (state == 1) {
				words.add(currentword);
			}

			words.trimToSize();
		}
		if (getPrefix() != null) {
			j = getPrefix().indexOf('!');
			if (j > -1) {
				setSender(getPrefix().substring(0, j));
				setHostmask(getPrefix().substring(j + 1));
			}
		}
		if (getCommand().equals("PRIVMSG")) {
			if (getParams().equals(BotStats.getInstance().getBotname())) {
				setPrivate(true);
				setReplyTo(getSender());
			} else {
				setChanname(getParams());
				setReplyTo(getChanname());
			}
		} else if(getCommand().equals("PART")) {
			setChanname(getParams());
		} else if (getCommand().equals("JOIN")) {
			setChanname(getTrailing());
		} else if(getCommand().equals("NOTICE")) {
			if (getParams().equals(BotStats.getInstance().getBotname())) {
				setPrivate(true);
				setReplyTo(""); // never reply to a NOTICE
			} else
				setChanname(getParams());
				setReplyTo(""); // never reply to a NOTICE
		}

		/* this ain't right.  --rs.
		if (isPrivate)
			replyTo = sender ;
		else
			replyTo = channame ;
		*/
		if (isPrivate()) {  //if private, set modTrailing (everything after command), modCommand (first word) and ignore "goat"
			String words = getTrailing();
			StringTokenizer st = new StringTokenizer(words);
			String firstWord = "";
			if (st.hasMoreTokens()) {
				firstWord = st.nextToken() ;
			}
			if ((!firstWord.toLowerCase().matches(BotStats.getInstance().getBotname().toLowerCase() + "\\w+"))
                    && firstWord.toLowerCase().matches(BotStats.getInstance().getBotname().toLowerCase() + "\\W*")) {
                setDirectlyAddressed(true);
                if (st.hasMoreTokens()) {
					setModCommand(st.nextToken());
					while (st.hasMoreTokens())
						setModTrailing(getModTrailing() + (st.nextToken() + ' '));         //TODO all this String concatenation in loops is nae use, need to replace with StringBuffer. But StringBuilder comes with jdk1.5, so will just wait till it is widespread
				}
			} else {
                setDirectlyAddressed(false);
                setModCommand(StringUtil.removeFormattingAndColors(firstWord));
				while (st.hasMoreTokens())
					setModTrailing(getModTrailing() + (st.nextToken() + ' '));
			}
		} else if (!isCTCP()) {
			String words = getTrailing();
			StringTokenizer st = new StringTokenizer(words);
			String firstWord = "";
			if (st.hasMoreTokens()) {
				firstWord = st.nextToken();
			}
			if ((!firstWord.toLowerCase().matches(BotStats.getInstance().getBotname().toLowerCase() + "\\w+"))
                    && firstWord.toLowerCase().matches(BotStats.getInstance().getBotname().toLowerCase() + "\\W*")) {
                setDirectlyAddressed(true);
                if (st.hasMoreTokens())
					setModCommand(st.nextToken());
			} else {
                setDirectlyAddressed(false);
                setModCommand(StringUtil.removeFormattingAndColors(firstWord));
			}

			while (st.hasMoreTokens()) {
				setModTrailing(getModTrailing() + (st.nextToken() + ' '));
			}
		}

		if (getPrefix().equals(BotStats.getInstance().getOwner()))
			setAuthorised(true);
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
		return ( (getPrefix().length() > 0 ? ':' + getPrefix() + ' ' : "") + getCommand() + (getParams().length() > 0 ? " " : "") + getParams() + (getTrailing().length() > 0 ? " :" + getTrailing() : "") );
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
		Message ret;
		if (getCommand().equals("PRIVMSG"))
			ret = createPrivmsg(getReplyTo(), trailing);
		else
			ret = new Message("", "", "", ""); //hopefully this will be accepted and ignored
		return ret;
	}

	/**
	 * Just reply directly, lets not muck about with this m.createReply("blah").send() business
	 * @param trailing
	 */
	public void reply(String trailing) {
		createPagedReply(trailing).send();
	}

	/**
         * obsolete, use reply()
	 * Just do a paged reply directly, lets not muck about with this m.createPagedReply("blah").send() business
	 * @param trailing
	 */
	public void pagedReply(String trailing) {
		createPagedReply(trailing).send();
	}

	/**
	 * Creates a new paged reply, using createReply(), and initializes the pager cache with the supplied string
	 *
	 * @param trailing The text to be paged and sent
	 *
	 * @return a message containing the first chunk of paged text, which the caller will most likely want to send()
	 */
	public Message createPagedReply(String trailing) {
		Message ret;
		if(getCommand().equals("PRIVMSG"))
			ret = createPagedPrivmsg(getReplyTo(), trailing);
		else
			ret = new Message("","","",""); //hopefully this will be accepted and ignored
		return ret;
	}

	/**
	 * Is there more text in the pager buffer for the current channel/nick?
	 *
	 * @return true if there's more text to be had.
	 */
	public boolean hasNextPage() {
		return hasNextPage(getReplyTo());
	}

	public static boolean hasNextPage(String key) {
		boolean ret = false;
		synchronized (pagerCache) {
			if (pagerCache.containsKey(key) ) {
				Pager pager = pagerCache.get(key) ;
				if (pager.isEmpty())
					pagerCache.remove(key);
				else
					ret = true;
			}
		}
		return ret ;
	}

	/**
	 * returns a reply message via createReply containing the next page of text, if any, from the pager cache for the current channel/nick (ie, "params")
	 *
	 * @return aforesaid message, if there is more text in the buffer, else an empty message.
	 */
	public Message createNextPage() {
		Message ret = new Message("", "", "", "") ;
		synchronized (pagerCache) {
			if (hasNextPage() )
                            ret = createReply(nextPage(getReplyTo(), getCommand())) ;
		}
		return ret;
	}

        public static String nextPage(String key, String messageCommand) {
		String ret = "";
		if (hasNextPage(key) )
			synchronized (pagerCache) {
				Pager pager = pagerCache.get(key);
				ret = pager.getNext(messageCommand, key);
				if(pager.isEmpty())
					pagerCache.remove(key);
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
			return new Message("", "", "", ""); //hopefully this will be accepted and ignored
		}
		return new Message("", command, getReplyTo(), trailing);
	}



//	/**
//	 * Removes all colours from this message (from trailing and modTrailing fields)
//	 */
//	public void removeColors() {
//		setTrailing(Constants.removeColors(getTrailing()));
//		setModTrailing(Constants.removeColors(getModTrailing()));
//	}
//
//	/**
//	 * Removes all formatting from this message. So removes all bold, underlining, reverse etc from the fields trailing & modTrailing.
//	 */
//	public void removeFormatting() {
//		setTrailing(Constants.removeFormatting(getTrailing()));
//		setModTrailing(Constants.removeFormatting(getModTrailing()));
//	}
//
//	/**
//	 * Removes all formatting and colors from this message.
//	 */
//	public void removeFormattingAndColors() {
//		removeFormatting();
//		removeColors();
//	}


	/**
	 * Sends a generic message using the current connection.
	 */
	public static void send(Message m) {
		// Do a little sanity checking and final adjusting before we queue this outgoing message
		if (m.getCommand().equalsIgnoreCase("PRIVMSG") || m.getCommand().equalsIgnoreCase("NOTICE")) {
			if (0 == m.getParams().indexOf("#"))
				m.setChanname(m.getParams());
			else if (m.getParams().equals("")) {
				System.err.println(m.getCommand() + " message has no valid recipient; not sending:") ;
				System.err.println("   " + m.getCommand() + " " + m.getParams() + " | " + m.getTrailing()) ;
				return ;
			}
		}
		if (m.getSender().equals("")) {
			m.setSender(BotStats.getInstance().getBotname());
		}
		outqueue.add(m);
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

	public String getCTCPCommand() {
		return CTCPCommand;
	}

	protected void setCTCPCommand(String command) {
		CTCPCommand = command;
	}

	public String getCTCPMessage() {
		return CTCPMessage;
	}

	protected void setCTCPMessage(String message) {
		CTCPMessage = message;
	}

	public boolean isDirectlyAddressed() {
		return directlyAddressed;
	}

	protected void setDirectlyAddressed(boolean directlyAddressed) {
		this.directlyAddressed = directlyAddressed;
	}

	public String getHostmask() {
		return hostmask;
	}

	protected void setHostmask(String hostmask) {
		this.hostmask = hostmask;
	}

	public boolean isCTCP() {
		return isCTCP;
	}

	protected void setCTCP(boolean isCTCP) {
		this.isCTCP = isCTCP;
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

	/*
	public boolean isPrivate() {
		return isPrivateMessage();
	}

	public void setPrivate(boolean isPrivate) {
		this.setPrivateMessage(isPrivate);
	}
*/

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
		if(!"".equals(channame))
			ret += "\n   channame:           " + channame;
		if(!"".equals(command))
			ret += "\n   command:            " + command;
		if (!"".equals(CTCPCommand))
			ret += "\n   CTCPCommand:        " + CTCPCommand;
		if(!"".equals(CTCPMessage))
			ret += "\n   CTCPMessage:        " + CTCPMessage;
		if(directlyAddressed)
			ret += "\n   directlyAddressed.";
		if(!"".equals(hostmask))
			ret += "\n   hostmask            " + hostmask;
		if(isAuthorised)
			ret += "\n   isAuthorised.";
		if(isCTCP)
			ret += "\n   isCTCP.";
		if(isPrivate)
			ret += "\n   isPrivate.";
		if(!"".equals(modCommand))
			ret += "\n   modCommand:          " + modCommand;
		if(!"".equals(modTrailing))
			ret += "\n   modTrailing:         " + modTrailing;
		if(!"".equals(params))
			ret += "\n   params:              " + params;
		if(!"".equals(prefix))
			ret += "\n   prefix:              " + prefix;
		if(!"".equals(replyTo))
			ret += "\n   replyTo:             " + replyTo;
		if(!"".equals(sender))
			ret += "\n   sender:              " + sender;
		if(!"".equals(trailing))
			ret += "\n   trailing:            " + trailing;
		if(words != null && !words.isEmpty()) {
			ret += "\n   words:              ";
			for(String w: words)
				ret += " " + w;
		}
		return ret;
	}
}
