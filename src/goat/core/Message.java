package goat.core;

import java.util.*;

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
 * @author Daniel Pope
 * @version 1.0
 */

public class Message {

	//public static String server;
	//public static String nickuserhost;

	/**
	 * True if this message is sent by the owner.
	 */
	public boolean isAuthorised = false;
	/**
	 * The prefix of a message. Usually the hostmask who sent the message. The sender field is a substring of this.
	 */
	public String prefix = "";

	/**
	 * The content of a message. Often a line of chat.
	 * <p/>
	 * In the case of PRIVMSG, this is the chat line.
	 */
	public String trailing = "";

	/**
	 * The first word of the content of the message - eg the "command" that modules may be interested in
	 */
	public String modCommand = "";

	/**
	 * All the trailing part of the message beyond the first word which may be the module command
	 */
	public String modTrailing = "";

	/**
	 * The command embodied by a message. Typical commands include PRIVMSG (most user communication), NOTICE (communication
	 * that must not be automatically responded to), JOIN (join a channel), MODE (change the status of something), and PART
	 * (leave a channel).
	 */
	public String command = "";

	/**
	 * The parameters of the message command. Often the addressee of a communication. For PRIVMSG, this is either a channel
	 * or the user receiving the message.
	 */
	public String params = "";

	/**
	 * The nickname part of the sending hostmask.
	 * <p/>
	 * This is the nick of the person who sent the message. Not to be confused with the hostname, which contains further
	 * details.
	 */
	public String sender = "";

	/**
	 * Whether this message was sent one to one. False if sent to a channel, true if only to one user.
	 */
	public boolean isPrivate = false;

	/**
	 * The Channel name
	 */
	public String channame = "";

	/**
	 * CTCP is a slight addition to the protocol. Effectively, this allows extension commands to be supplied, which can be
	 * ignored by the client if not understood. CTCP is usually a PRIVMSG, where the trailing is encapulated by ASCII 0x01,
	 * and the first full word of which is the command.
	 * <p/>
	 * The most common example of CTCP is the CTCP ACTION, which is used by typing /me in most clients.
	 */
	public boolean isCTCP = false;

	/**
	 * The CTCP command (if isCTCP is true). eg. 'ACTION' for an action/emote
	 */
	public String CTCPCommand = "";

	/**
	 * The CTCP message (if isCTCP is true). eg. 'giggles' in '/me giggles'
	 */
	public String CTCPMessage = "";

	private ArrayList words;

	/**
	 * The most low level way of creating a new message, this requires some knowledge of the IRC RFC. prefix can be left
	 * empty (but not null) with most outgoing messages.
	 */
	public Message(String prefix, String command, String params, String trailing) {
		this.prefix = prefix;
		this.command = command;
		this.params = params;
		this.trailing = trailing;
	}

	/**
	 * Removes all previously applied color and formatting attributes.
	 */
	public static final String NORMAL = "\u000f";


	/**
	 * Bold text.
	 */
	public static final String BOLD = "\u0002";


	/**
	 * Underlined text.
	 */
	public static final String UNDERLINE = "\u001f";


	/**
	 * Reversed text (may be rendered as italic text in some clients).
	 */
	public static final String REVERSE = "\u0016";


	/**
	 * White coloured text.
	 */
	public static final String WHITE = "\u000300";


	/**
	 * Black coloured text.
	 */
	public static final String BLACK = "\u000301";


	/**
	 * Dark blue coloured text.
	 */
	public static final String DARK_BLUE = "\u000302";


	/**
	 * Dark green coloured text.
	 */
	public static final String DARK_GREEN = "\u000303";


	/**
	 * Red coloured text.
	 */
	public static final String RED = "\u000304";


	/**
	 * Brown coloured text.
	 */
	public static final String BROWN = "\u000305";


	/**
	 * Purple coloured text.
	 */
	public static final String PURPLE = "\u000306";


	/**
	 * Olive coloured text.
	 */
	public static final String OLIVE = "\u000307";


	/**
	 * Yellow coloured text.
	 */
	public static final String YELLOW = "\u000308";


	/**
	 * Green coloured text.
	 */
	public static final String GREEN = "\u000309";


	/**
	 * Teal coloured text.
	 */
	public static final String TEAL = "\u000310";


	/**
	 * Cyan coloured text.
	 */
	public static final String CYAN = "\u000311";


	/**
	 * Blue coloured text.
	 */
	public static final String BLUE = "\u000312";


	/**
	 * Magenta coloured text.
	 */
	public static final String MAGENTA = "\u000313";


	/**
	 * Dark gray coloured text.
	 */
	public static final String DARK_GRAY = "\u000314";


	/**
	 * Light gray coloured text.
	 */
	public static final String LIGHT_GRAY = "\u000315";


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

	/**
	 * Creates a new outgoing CTCP message.
	 * 
	 * @param to          The nick of the person to send to.
	 * @param command     The IRC command (PRIVMSG or NOTICE)
	 * @param CTCPcommand The CTCP command to send. The most common is ACTION.
	 * @param CTCPparams  The parameters of the CTCP command. With an ACTION, this is the action.
	 */
	public static Message createCTCP(String to, String command, String CTCPcommand, String CTCPparams) {
		return new Message("", command, to, (char) 0x01 + CTCPcommand + " " + CTCPparams + (char) 0x01);
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
				prefix = messagestring.substring(1, k);
			i = messagestring.indexOf(' ', k + 1);
			command = messagestring.substring(k + 1, i);
			j = messagestring.indexOf(" :", i);

			if (j == -1)
				params = messagestring.substring(i + 1);
			else {
				if (j != i)
					params = messagestring.substring(i + 1, j);
				trailing = messagestring.substring(j + 2);
			}
		} else    //non prefix form - this doesn't happen in client communications
		{
			i = messagestring.indexOf(' ');
			command = messagestring.substring(0, i);
			j = messagestring.indexOf(" :", i);

			if (j < 0)
				params = messagestring.substring(i + 1);
			else {
				if (j != i)
					params = messagestring.substring(i + 1, j);
				trailing = messagestring.substring(j + 2);
			}
		}

		//Extended parsing decodes the message further.
		String CTCP;

		if (trailing != null && trailing.length() > 1 && trailing.charAt(0) == 0x01 && trailing.charAt(trailing.length() - 1) == 0x01) {
			CTCP = trailing.substring(1, trailing.length() - 1);

			if ((i = CTCP.indexOf(' ')) > -1) {
				CTCPCommand = CTCP.substring(0, i);
				CTCPMessage = CTCP.substring(i);
			} else {
				CTCPCommand = CTCP;
			}

			isCTCP = true;
		} else if (trailing != null) {
			words = new ArrayList();

			int state = 0;

			char ch;
			String currentword = "";

			for (i = 0; i < trailing.length(); i++) {
				ch = trailing.charAt(i);

				switch (Character.getType(ch)) {
					case Character.DECIMAL_DIGIT_NUMBER:
					case Character.LOWERCASE_LETTER:
					case Character.UPPERCASE_LETTER:
						currentword += ch;
						state = 1;
						break;

					default:
						if (ch == '\'') {
							//this case is an exception so we can count apostrophe's, etc within the one word.
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

		if (command.equals("PRIVMSG")) {
			if (params.equals(BotStats.botname)) {
				isPrivate = true;
			} else
				channame = params;

		}

		if (prefix != null) {
			j = prefix.indexOf('!');

			if (j > -1) {
				sender = prefix.substring(0, j);
			}
		}

		if (isPrivate) {  //if private, set modTrailing (everything after command), modCommand (first word) and ignore "goat"
			String words = trailing;
			StringTokenizer st = new StringTokenizer(words);
			String firstWord = "";
			if (st.hasMoreTokens()) {
				firstWord = st.nextToken();
				firstWord = firstWord.replaceAll("\\W", "");
			}
			if (firstWord.toLowerCase().startsWith(BotStats.botname)) {
				if (st.hasMoreTokens()) {
					modCommand = st.nextToken();
					modCommand = modCommand.replaceAll("\\W", "");  //zap nonword characters
					while (st.hasMoreTokens())
						modTrailing += st.nextToken() + " ";
				}
			} else {
				modCommand = firstWord;
				while (st.hasMoreTokens())
					modTrailing += st.nextToken() + " ";
			}
		} else if (!isCTCP) {
			String words = trailing;
			StringTokenizer st = new StringTokenizer(words);
			String firstWord = "";
			if (st.hasMoreTokens())
				firstWord = st.nextToken();
			if (firstWord.toLowerCase().startsWith(BotStats.botname)) {
				if (st.hasMoreTokens())
					modCommand = st.nextToken();
				modCommand = modCommand.replaceAll("\\W", "");  //zap nonword characters
			} else {
				modCommand = firstWord;
			}

			while (st.hasMoreTokens()) {
				modTrailing += st.nextToken() + " ";
			}
		}

		if (isPrivate && prefix.equals(BotStats.owner))
			isAuthorised = true;
	}

	byte[] toByteArray() {
		String message = ((prefix.length() > 0) ? (":" + prefix + " ") : "") + command + ((params.length() > 0) ? " " : "") + params + ((trailing.length() > 0) ? " :" + trailing : "");

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
		if (!command.equals("PRIVMSG")) {
			return new Message("", "", "", ""); //hopefully this will be accepted and ignored
		}

		if (!params.equals(BotStats.botname))    //if this is a private message
		{
			return new Message("", "PRIVMSG", params, trailing);
		} else {
			return new Message("", "PRIVMSG", sender, trailing);
		}
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
		if (!this.command.equals("PRIVMSG")) {
			return new Message("", "", "", ""); //hopefully this will be accepted and ignored
		}

		if (!params.equals(BotStats.botname))    //if this is a private message
		{
			return new Message("", command, params, trailing);
		} else {
			return new Message("", command, sender, trailing);
		}
	}

	/**
	 * Removes all colours from a line of text.
	 */
	private static String removeColors(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		while (i < length) {
			char ch = line.charAt(i);
			if (ch == '\u0003') {
				i++;
				// Skip "x" or "xy" (foreground color).
				if (i < length) {
					ch = line.charAt(i);
					if (Character.isDigit(ch)) {
						i++;
						if (i < length) {
							ch = line.charAt(i);
							if (Character.isDigit(ch)) {
								i++;
							}
						}
						// Now skip ",x" or ",xy" (background color).
						if (i < length) {
							ch = line.charAt(i);
							if (ch == ',') {
								i++;
								if (i < length) {
									ch = line.charAt(i);
									if (Character.isDigit(ch)) {
										i++;
										if (i < length) {
											ch = line.charAt(i);
											if (Character.isDigit(ch)) {
												i++;
											}
										}
									} else {
										// Keep the comma.
										i--;
									}
								} else {
									// Keep the comma.
									i--;
								}
							}
						}
					}
				}
			} else if (ch == '\u000f') {
				i++;
			} else {
				buffer.append(ch);
				i++;
			}
		}
		return buffer.toString();
	}

	/**
	 * Removes all colours from this message (from trailing and modTrailing fields)
	 */
	public void removeColors() {
		trailing = removeColors(trailing);
		modTrailing = removeColors(modTrailing);
	}

	/**
	 * Remove formatting from a line of IRC text.
	 *
	 * @param line the input text.
	 * @return the same text, but without any bold, underlining, reverse, etc.
	 */
	private static String removeFormatting(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < length; i++) {
			char ch = line.charAt(i);
			if (ch == '\u000f' || ch == '\u0002' || ch == '\u001f' || ch == '\u0016') {
				// Don't add this character.
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	/**
	 * Removes all formatting from this message. So removes all bold, underlining, reverse etc from the fields trailing & modTrailing.
	 */
	public void removeFormatting() {
		trailing = removeFormatting(trailing);
		modTrailing = removeFormatting(modTrailing);
	}

	/**
	 * Removes all formatting and colors from this message.
	 */
	public void removeFormattingAndColors() {
		removeFormatting();
		removeColors();
	}
}
