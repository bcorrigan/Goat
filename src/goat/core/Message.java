package goat.core;

import goat.Goat;
import goat.util.Pager;

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
 * @author bc & much filched from Daniel Pope's bot & bits from pircbot
 * @version 1.0
 */

public class Message {

    /**
	 * The outqueue instance for sending messages
	 */
	private static MessageQueue outqueue = Goat.outqueue;
	/**
	 * True if this message is sent by the owner.
	 */
	public boolean isAuthorised;
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
	public boolean isPrivate;

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
	public boolean isCTCP;

	/**
	 * The CTCP command (if isCTCP is true). eg. 'ACTION' for an action/emote
	 */
	public String CTCPCommand = "";

	/**
	 * The CTCP message (if isCTCP is true). eg. 'giggles' in '/me giggles'
	 */
	public String CTCPMessage = "";

	private ArrayList words;

	private static HashMap pagerCache = new HashMap() ;

	/**
	 * Is there more text in the pager buffer for the current channel/nick?
	 *
	 * @return true if there's more text to be had.
	 */
	public boolean hasNextPage() {
		if ( ! pagerCache.containsKey(params) ) 
			return false ;
		Pager pager = (Pager) pagerCache.get(params) ;
		if (pager.isEmpty()) {
			pagerCache.remove(params) ;
			return false ;
		}
		return true ;
	}

	
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

	//Various response codes follow:
	
	// Error Replies.
	public static final int ERR_NOSUCHNICK = 401;
	public static final int ERR_NOSUCHSERVER = 402;
	public static final int ERR_NOSUCHCHANNEL = 403;
	public static final int ERR_CANNOTSENDTOCHAN = 404;
	public static final int ERR_TOOMANYCHANNELS = 405;
	public static final int ERR_WASNOSUCHNICK = 406;
	public static final int ERR_TOOMANYTARGETS = 407;
	public static final int ERR_NOORIGIN = 409;
	public static final int ERR_NORECIPIENT = 411;
	public static final int ERR_NOTEXTTOSEND = 412;
	public static final int ERR_NOTOPLEVEL = 413;
	public static final int ERR_WILDTOPLEVEL = 414;
	public static final int ERR_UNKNOWNCOMMAND = 421;
	public static final int ERR_NOMOTD = 422;
	public static final int ERR_NOADMININFO = 423;
	public static final int ERR_FILEERROR = 424;
	public static final int ERR_NONICKNAMEGIVEN = 431;
	public static final int ERR_ERRONEUSNICKNAME = 432;
	public static final int ERR_NICKNAMEINUSE = 433;
	public static final int ERR_NICKCOLLISION = 436;
	public static final int ERR_USERNOTINCHANNEL = 441;
	public static final int ERR_NOTONCHANNEL = 442;
	public static final int ERR_USERONCHANNEL = 443;
	public static final int ERR_NOLOGIN = 444;
	public static final int ERR_SUMMONDISABLED = 445;
	public static final int ERR_USERSDISABLED = 446;
	public static final int ERR_NOTREGISTERED = 451;
	public static final int ERR_NEEDMOREPARAMS = 461;
	public static final int ERR_ALREADYREGISTRED = 462;
	public static final int ERR_NOPERMFORHOST = 463;
	public static final int ERR_PASSWDMISMATCH = 464;
	public static final int ERR_YOUREBANNEDCREEP = 465;
	public static final int ERR_KEYSET = 467;
	public static final int ERR_CHANNELISFULL = 471;
	public static final int ERR_UNKNOWNMODE = 472;
	public static final int ERR_INVITEONLYCHAN = 473;
	public static final int ERR_BANNEDFROMCHAN = 474;
	public static final int ERR_BADCHANNELKEY = 475;
	public static final int ERR_NOPRIVILEGES = 481;
	public static final int ERR_CHANOPRIVSNEEDED = 482;
	public static final int ERR_CANTKILLSERVER = 483;
	public static final int ERR_NOOPERHOST = 491;
	public static final int ERR_UMODEUNKNOWNFLAG = 501;
	public static final int ERR_USERSDONTMATCH = 502;


	// Command Responses.
	public static final int RPL_TRACELINK = 200;
	public static final int RPL_TRACECONNECTING = 201;
	public static final int RPL_TRACEHANDSHAKE = 202;
	public static final int RPL_TRACEUNKNOWN = 203;
	public static final int RPL_TRACEOPERATOR = 204;
	public static final int RPL_TRACEUSER = 205;
	public static final int RPL_TRACESERVER = 206;
	public static final int RPL_TRACENEWTYPE = 208;
	public static final int RPL_STATSLINKINFO = 211;
	public static final int RPL_STATSCOMMANDS = 212;
	public static final int RPL_STATSCLINE = 213;
	public static final int RPL_STATSNLINE = 214;
	public static final int RPL_STATSILINE = 215;
	public static final int RPL_STATSKLINE = 216;
	public static final int RPL_STATSYLINE = 218;
	public static final int RPL_ENDOFSTATS = 219;
	public static final int RPL_UMODEIS = 221;
	public static final int RPL_STATSLLINE = 241;
	public static final int RPL_STATSUPTIME = 242;
	public static final int RPL_STATSOLINE = 243;
	public static final int RPL_STATSHLINE = 244;
	public static final int RPL_LUSERCLIENT = 251;
	public static final int RPL_LUSEROP = 252;
	public static final int RPL_LUSERUNKNOWN = 253;
	public static final int RPL_LUSERCHANNELS = 254;
	public static final int RPL_LUSERME = 255;
	public static final int RPL_ADMINME = 256;
	public static final int RPL_ADMINLOC1 = 257;
	public static final int RPL_ADMINLOC2 = 258;
	public static final int RPL_ADMINEMAIL = 259;
	public static final int RPL_TRACELOG = 261;
	public static final int RPL_NONE = 300;
	public static final int RPL_AWAY = 301;
	public static final int RPL_USERHOST = 302;
	public static final int RPL_ISON = 303;
	public static final int RPL_UNAWAY = 305;
	public static final int RPL_NOWAWAY = 306;
	public static final int RPL_WHOISUSER = 311;
	public static final int RPL_WHOISSERVER = 312;
	public static final int RPL_WHOISOPERATOR = 313;
	public static final int RPL_WHOWASUSER = 314;
	public static final int RPL_ENDOFWHO = 315;
	public static final int RPL_WHOISIDLE = 317;
	public static final int RPL_ENDOFWHOIS = 318;
	public static final int RPL_WHOISCHANNELS = 319;
	public static final int RPL_LISTSTART = 321;
	public static final int RPL_LIST = 322;
	public static final int RPL_LISTEND = 323;
	public static final int RPL_CHANNELMODEIS = 324;
	public static final int RPL_NOTOPIC = 331;
	public static final int RPL_TOPIC = 332;
	public static final int RPL_TOPICINFO = 333;
	public static final int RPL_INVITING = 341;
	public static final int RPL_SUMMONING = 342;
	public static final int RPL_VERSION = 351;
	public static final int RPL_WHOREPLY = 352;
	public static final int RPL_NAMREPLY = 353;
	public static final int RPL_LINKS = 364;
	public static final int RPL_ENDOFLINKS = 365;
	public static final int RPL_ENDOFNAMES = 366;
	public static final int RPL_BANLIST = 367;
	public static final int RPL_ENDOFBANLIST = 368;
	public static final int RPL_ENDOFWHOWAS = 369;
	public static final int RPL_INFO = 371;
	public static final int RPL_MOTD = 372;
	public static final int RPL_ENDOFINFO = 374;
	public static final int RPL_MOTDSTART = 375;
	public static final int RPL_ENDOFMOTD = 376;
	public static final int RPL_YOUREOPER = 381;
	public static final int RPL_REHASHING = 382;
	public static final int RPL_TIME = 391;
	public static final int RPL_USERSSTART = 392;
	public static final int RPL_USERS = 393;
	public static final int RPL_ENDOFUSERS = 394;
	public static final int RPL_NOUSERS = 395;


	// Reserved Numerics.
	public static final int RPL_TRACECLASS = 209;
	public static final int RPL_STATSQLINE = 217;
	public static final int RPL_SERVICEINFO = 231;
	public static final int RPL_ENDOFSERVICES = 232;
	public static final int RPL_SERVICE = 233;
	public static final int RPL_SERVLIST = 234;
	public static final int RPL_SERVLISTEND = 235;
	public static final int RPL_WHOISCHANOP = 316;
	public static final int RPL_KILLDONE = 361;
	public static final int RPL_CLOSING = 362;
	public static final int RPL_CLOSEEND = 363;
	public static final int RPL_INFOSTART = 373;
	public static final int RPL_MYPORTIS = 384;
	public static final int ERR_YOUWILLBEBANNED = 466;
	public static final int ERR_BADCHANMASK = 476;
	public static final int ERR_NOSERVICEHOST = 492;


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

	public static Message createPagedPrivmsg(String to, String message) {
		Pager pager = new Pager(message) ;
		pagerCache.put(to, pager) ;
		return new Message("", "PRIVMSG", to, pager.getNext()) ;
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
		return new Message("", command, to, (char) 0x01 + CTCPcommand + ' ' + CTCPparams + (char) 0x01);
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
			words = new ArrayList(30);

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
			if (firstWord.toLowerCase().startsWith(BotStats.botname.toLowerCase())) {
				if (st.hasMoreTokens()) {
					modCommand = st.nextToken();
					modCommand = modCommand.replaceAll("\\W", "");  //zap nonword characters
					while (st.hasMoreTokens())
						modTrailing += st.nextToken() + ' ';         //TODO all this String concatenation in loops is nae use, need to replace with StringBuffer. But StringBuilder comes with jdk1.5, so will just wait till it is widespread
				}
			} else {
				modCommand = firstWord;
				while (st.hasMoreTokens())
					modTrailing += st.nextToken() + ' ';
			}
		} else if (!isCTCP) {
			String words = trailing;
			StringTokenizer st = new StringTokenizer(words);
			String firstWord = "";
			if (st.hasMoreTokens())
				firstWord = st.nextToken();
			if (firstWord.toLowerCase().startsWith(BotStats.botname.toLowerCase())) {
				if (st.hasMoreTokens())
					modCommand = st.nextToken();
				modCommand = modCommand.replaceAll("\\W", "");  //zap nonword characters
			} else {
				modCommand = firstWord;
			}

			while (st.hasMoreTokens()) {
				modTrailing += st.nextToken() + ' ';
			}
		}

		if (isPrivate && prefix.equals(BotStats.owner))
			isAuthorised = true;
	}

	byte[] toByteArray() {
		String message = (prefix.length() > 0 ? ':' + prefix + ' ' : "") + command + (params.length() > 0 ? " " : "") + params + (trailing.length() > 0 ? " :" + trailing : "");

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
	 * Creates a new paged reply, using createReply(), and initializes the pager cache with the supplied string
	 * 
	 * @param trailing The text to be paged and sent
	 *
	 * @return a message containing the first chunk of paged text, which the caller will most likely want to send()
	 */
	public Message createPagedReply(String trailing) {
		if (trailing.length() <= Pager.maxMessageLength) 
			return createReply(Pager.smush(trailing)) ;
		else {
			Pager pager = new Pager(trailing) ;
			pagerCache.put(params, pager) ;
			return createReply(pager.getNext()) ;
		}
	}
	
	/** 
	 * returns a reply message via createReply containing the next page of text, if any, from the pager cache for the current channel/nick (ie, "params")
	 *
	 * @return aforsaid message, if there is more text in the buffer, else an empty message.
	 */
	public Message createNextPage() {
		if (! hasNextPage() ) 
			return new Message("", "", "", "") ;
		Pager pager = (Pager) pagerCache.get(params) ;
		return createReply(pager.getNext()) ;
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
	 * Removes all colours from a line of text. nicked from pircbot
	 */
	private static String removeColors(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer(length);
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
	 * Remove formatting from a line of IRC text. From pircbot
	 *
	 * @param line the input text.
	 * @return the same text, but without any bold, underlining, reverse, etc.
	 */
	private static String removeFormatting(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer(length);
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

	/**
	 * Sends a generic message using the current connection.
	 */
	public static void send(Message m) {
		outqueue.enqueue(m);
	}

	/**
	 * Sends this message
	 */
	public void send() {
		outqueue.enqueue(this);
	}
}
