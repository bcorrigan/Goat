package goat.core;

import goat.util.Pager;
import goat.util.StringUtil;

import java.util.ArrayList;
import java.util.StringTokenizer;

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

public class IrcMessage extends Message {

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


	public IrcMessage() {
		// so's we can serialize
	}

	/**
	 * The most low level way of creating a new message, this requires some knowledge of the IRC RFC. prefix can be left
	 * empty (but not null) with most outgoing messages.
	 */
	public IrcMessage(String prefix, String command, String params, String trailing) {
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
		return new IrcMessage("", "NOTICE", to, message);
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
		return new IrcMessage("", "PRIVMSG", to, message);
	}

	public static synchronized Message createPagedPrivmsg(String to, String message) {
	    IrcMessage ret;
	    synchronized (pagerCache) {
	        if(Pager.shouldPaginate(message)) {
	            Pager pager = new Pager(message) ;
	            pagerCache.put(to, pager) ;
	            ret = new IrcMessage("", "PRIVMSG", to, pager.getNext("PRIVMSG", to)) ;
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
		IrcMessage m = new IrcMessage("", command, to, (char) 0x01 + payload + (char) 0x01);
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
	public IrcMessage(String messagestring) {
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

	/*
	public boolean isPrivate() {
		return isPrivateMessage();
	}

	public void setPrivate(boolean isPrivate) {
		this.setPrivateMessage(isPrivate);
	}
*/

	public String dataDump() {
		String ret = super.dataDump();
		if (!"".equals(CTCPCommand))
			ret += "\n   CTCPCommand:        " + CTCPCommand;
		if (!"".equals(CTCPMessage))
			ret += "\n   CTCPMessage:        " + CTCPMessage;
		if (isCTCP)
			ret += "\n   isCTCP.";
		return ret;
	}

}
