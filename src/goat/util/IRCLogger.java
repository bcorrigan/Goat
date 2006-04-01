package goat.util;

import goat.Goat;
import goat.util.GoatDB ;
import goat.core.Message ;
import goat.core.BotStats ;
import java.sql.* ;
import java.util.HashMap ;


/**
 * A class to log IRC events to teh GoatDB.
 * 
 * @author rs
 *
 */
public class IRCLogger {
	
	/*
	 * We're likely to do a lot of message inserts, so we'll 
	 * keep a prepared statement in an instance variable, like we
	 * do with our DB connection.
	 */
	private Connection conn ;
	private PreparedStatement msgInsertPS ;
	
	/*
	 * Masses of dull SQL, let's try to keep it all in one place here.
	 * 
	 * We'll do everything with prepared statements, for that proper 
	 * whips and chains feeling, and to make it slightly easier to keep
	 * all of our SQL in this ugly heap of constants.
	 * 
	 */
	private static String msgInsert = 
		"INSERT INTO messages (timestamp, sender, hostmask, channel, irc_command, ctcp_command, bot_command, text) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?) ;" ;
	private static String channelInsert = 
		"INSERT INTO channels (name, network) VALUES (?, ?) ;" ;
	private static String networkInsert = 
		"INSERT INTO networks (name, primary_server) VALUES (?, ?) ;" ;
	private static String hostmaskInsert =
		"INSERT INTO hostmasks (hostmask) VALUES (?) ;" ;
	private static String nickInsert = 
		"INSERT INTO nicks (name, seen, network, last_hostmask) VALUES (?, ?, ?, ?)" ;
	private static String ircCommandInsert = 
		"INSERT INTO irc_commands (name) VALUES (?) ;" ;
	private static String ctcpCommandInsert = 
		"INSERT INTO ctcp_commands (name) VALUES (?) ;" ;
	private static String botCommandInsert = 
		"INSERT INTO bot_commands (name) VALUES (?) ;" ;


	private static String networkIdByName = 
		"SELECT id FROM networks WHERE name = ? ;" ;
	private static String channelIdByNameAndNetwork = 
		"SELECT id FROM channels WHERE name = ? AND network = ? ;" ;
	private static String hostmaskIdByName = 
		"SELECT id FROM hostmasks WHERE hostmask = ?" ;
	private static String nickIdByNameAndNetwork =
		"SELECT id FROM nicks WHERE name = ? AND network = ? ;" ;
	private static String ircCommandByName = 
		"SELECT id FROM irc_commands WHERE name = ?" ;
	private static String ctcpCommandByName = 
		"SELECT id FROM ctcp_commands WHERE name = ?" ;
	private static String botCommandByName = 
		"SELECT id FROM bot_commands WHERE name = ?" ;
	
	private static String allMessages = 
		"SELECT * FROM messages_view ;" ;
	private static String msgById =
		"SELECT * FROM messages_view WHERE id = ? ;"  ;
	
	private static String nickTimestampUpdate = 
		"UPDATE nicks SET seen = ? WHERE id = ? ;" ;

	private static String msgTotalCount = 
		"SELECT COUNT(*) FROM messages ;" ;
	private static String msgChannelCount =
		"SELECT COUNT(*) FROM messages WHERE channel = ? ;" ;
	private static String msgNetworkCount = 
		"SELECT COUNT(*) FROM messages WHERE network = ? ;" ;
	
	/*
	 * We'll cache nick, channel and network IDs that we've looked up in the DB.
	 * Also IDs for irc, ctcp, and bot commans.
	 */
	private HashMap<String, Integer> networkIdCache = new HashMap<String, Integer>();
	private HashMap<String [], Integer> channelIdCache = new HashMap<String [], Integer>();
	private HashMap<String, Integer> hostmaskIdCache = new HashMap<String, Integer>();
	private HashMap<String [], Integer> nickIdCache = new HashMap<String [], Integer>();
	private HashMap<String, Integer> ircCommandIdCache = new HashMap<String, Integer>();
	private HashMap<String, Integer> ctcpCommandIdCache = new HashMap<String, Integer>();
	private HashMap<String, Integer> botCommandIdCache = new HashMap<String, Integer>();
	
	// TODO : do something to flush these caches once in a while?
	
	/**
	 * Constructor.
	 * 
	 * Sets up a connection to the Goat DB, prepares a statement for message inserts,
	 * gives you an object you can use to log IRC messages.
	 * 
	 * @throws SQLException
	 */
	public IRCLogger() {
		try {
			conn = GoatDB.getConnection() ;
			msgInsertPS = conn.prepareStatement(msgInsert);
		} catch (GoatDBConnectionException e) {
			System.err.println("ERROR -- logger could not connect to Goat DB") ;
			e.printStackTrace() ;
		} catch (SQLException e) {
			System.err.println("ERROR -- could not prepare statement: \"" + msgInsert + "\"") ;
		}
	}
	
	/*
	 * Destructor.
	 * 
	 * Closes the prepared statement and the connection to the Goat DB.
	 */
	// Ha ha, just kidding!  Java doens't have destructors!  Hehe!
	
	
	/**
	 * Log an irc message.
	 * 
	 * @param timestamp
	 *            timestamp
	 * @param sender
	 *            irc nick of message sender
	 * @param channel
	 *            channel this message was sent to
	 * @param network
	 *            name (not necessarily a hostname) of the irc network this
	 *            message was sent on
	 * @param message
	 *            text of message
	 * 
	 * @throws SQLException
	 *             if there was some problem inserting the message into the Goat
	 *             DB
	 * @return the id for this message (auto-generated by DB), or -1 if the id
	 *         could not be retrieved after the insert (which shouldn't ever
	 *         happen) ;
	 */
	public int logMessage(Timestamp timestamp, String sender, String hostmask, String channel, 
			String network, String ircCommand, String ctcpCommand, String botCommand, String message) 
		throws SQLException {
			int messageID = -1 ;
			int senderID = getID(sender, network, nickIdByNameAndNetwork, nickIdCache) ;
			int hostmaskID = getID(hostmask, hostmaskIdByName, hostmaskIdCache) ;
			int channelID = getID(channel, network, channelIdByNameAndNetwork, channelIdCache) ;
			int ircCommandID = getID(ircCommand, ircCommandByName, ircCommandIdCache) ;
			int ctcpCommandID = getID(ctcpCommand, ctcpCommandByName, ctcpCommandIdCache) ;
			int botCommandID = getID(botCommand, botCommandByName, botCommandIdCache) ;
			if (-1 == hostmaskID) 
				hostmaskID = addHostmask(hostmask) ;
			if (-1 == senderID) 
				senderID = addNick(sender, hostmaskID, network) ;
			if (-1 == channelID) 
				channelID = addChannel(channel, network) ;
			if (-1 == ircCommandID) 
				ircCommandID = addIrcCommand(ircCommand) ;
			if (-1 == ctcpCommandID)
				if ((null != ctcpCommand) && ("" != ctcpCommand))
					ctcpCommandID = addCtcpCommand(ctcpCommand) ;
				else
					ctcpCommandID = 0 ;
			if (-1 == botCommandID) 
				if ((null != botCommand) && ("" != botCommand))
					botCommandID = addBotCommand(botCommand) ;
				else
					botCommandID = 0 ;				
			msgInsertPS.setTimestamp(1, timestamp) ;
			msgInsertPS.setInt(2, senderID) ;
			msgInsertPS.setInt(3, hostmaskID) ;
			msgInsertPS.setInt(4, channelID) ;
			msgInsertPS.setInt(5, ircCommandID) ;
			msgInsertPS.setInt(6, ctcpCommandID) ;
			msgInsertPS.setInt(7, botCommandID) ;
			msgInsertPS.setString(8, message) ;
			msgInsertPS.execute() ;
			messageID = GoatDB.getIdentity(conn) ;
			updateNickTimestamp(senderID) ;
			
			return messageID ;
	}
	
	/**
	 * This one logs with timestamp of "now".
	 * 
	 * @param sender
	 * @param message
	 */
	public int logMessage(String sender, String hostmask, String channel, 
			String network, String ircCommand, String ctcpCommand, 
			String botCommand, String message) 
		throws SQLException {
		return logMessage(new Timestamp(System.currentTimeMillis()), 
				sender, hostmask, channel, network, ircCommand, 
				ctcpCommand, botCommand, message) ;
	}

	/**
	 * Log and index a goat Message, with timestamp set to "now".
	 * 
	 * You probably want logIncomingMessage() or logOutgoingMessage() instead; 
	 * this method does not filter out error messages or fill in empty
	 * message bodies for JOINs and PARTs and so forth.
	 * 
	 * @param m
	 *            a Message object to log and index
	 */
	public int logMessage(Message m, String network) throws SQLException {
		return logMessage(m, m.sender, network);
	}
	
	public int logIncomingMessage(Message m, String network) throws SQLException {
		if("" == m.sender) {
			System.err.println("No sender for incoming \"" + m.command + "\" message; not logging") ;
			return -1 ;
		}
		return logMessage(m, network) ;
	}
	
	public int logOutgoingMessage(Message m, String network)
			throws SQLException {
		return logMessage(m, BotStats.botname, network);
	}
	
	public int logMessage(Message m, String sender, String network)
			throws SQLException {
		// the javaWank(tm) way to do this would be to have Message implement
		// the SQLData interface. Needless to say, we're not going to do that.
		String ctcpCommand = null;
		String body = m.trailing;
		if ("" == body) {
			System.err.println("No body found for \"" + m.command + "\" message, using \"" + m.command + " " + m.params + "\" for logging purposes") ;
			body = m.command + " " + m.params;
			body = body.trim() ;
		}
		if (m.isCTCP) {
			ctcpCommand = m.CTCPCommand;
			body = m.CTCPCommand + " " + m.CTCPMessage;
		}
		String ircCommand = m.command;
		String hostmask = m.prefix;
		int offset = m.prefix.indexOf('!');
		if (offset > -1) {
			hostmask = m.prefix.substring(offset + 1);
		}
		String botCommand = null;
		if (Goat.modController.isLoadedCommand(m.modCommand))
			botCommand = m.modCommand;
		return logMessage(sender, hostmask, m.channame, network, ircCommand,
				ctcpCommand, botCommand, body);
	}
	
	public int addChannel(String channel, String network) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(channelInsert) ;
		ps.setString(1, channel) ;
		ps.setInt(2, getID(network, networkIdByName, networkIdCache)) ;
		ps.execute() ;
		int id = GoatDB.getIdentity(conn) ;
		String [] key = {channel, network} ;
		channelIdCache.put(key, id) ;
		return id ;
	}
	
	public int addNick(String nick, int hostmaskId, String network) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(nickInsert) ;
		ps.setString(1, nick) ;
		ps.setTimestamp(2, new Timestamp(System.currentTimeMillis())) ;
		ps.setInt(3, getID(network, networkIdByName, networkIdCache)) ;
		ps.setInt(4, hostmaskId) ;
		ps.execute() ;
		int id = GoatDB.getIdentity(conn) ;
		String [] key = {nick, network} ;
		nickIdCache.put(key, id) ;
		return id ;
	}

	/*
	public int getNetworkID(String name) throws SQLException {
		return getID(name, networkIdByName, networkIdCache) ;
	}
	*/
	/**
	 * Add a new network to the goat DB.
	 * 
	 * If the network named is already in the db, it is left alone.
	 * 
	 * @param name name of the network, not necessarily a hostname.
	 * @param primaryServer address of the primary server for the network.
	 * @return the id of the network (auto-generated by the db on insert).
	 * @throws SQLException
	 */
	public int addNetwork(String name, String primaryServer) throws SQLException {
		int id = getID(name, networkIdByName, networkIdCache) ;
		if (-1 == id) {
			// network not in DB, insert it.
			PreparedStatement ps = conn.prepareStatement(networkInsert) ;
			ps.setString(1, name) ;
			ps.setString(2, primaryServer) ;
			ps.execute() ;
			id = GoatDB.getIdentity(conn) ;
			// cache the id we've just inserted into the db
			networkIdCache.put(name, id) ;
		}
		return id ;
	}
	
	public int addIrcCommand(String command) throws SQLException {
		return insertString(command, ircCommandInsert) ;
	}
	
	public int addCtcpCommand(String command) throws SQLException {
		return insertString(command, ctcpCommandInsert) ;
	}
	
	public int addBotCommand(String command) throws SQLException {
		return insertString(command, botCommandInsert) ;
	}
	
	public int addHostmask(String hostmask) throws SQLException {
		return insertString(hostmask, hostmaskInsert) ;
	}
	
	private int insertString(String name, String preparedInsert) throws SQLException {
		int id = -1 ;
		PreparedStatement ps = conn.prepareStatement(preparedInsert) ;
		ps.setString(1, name) ;
		ps.execute();
		id = GoatDB.getIdentity(conn) ;
		return id ;
	}
	
	public long updateNickTimestamp(int nickID)  throws SQLException {
		long millis = System.currentTimeMillis() ;
		Timestamp ts = new Timestamp(millis) ;
		PreparedStatement ps = conn.prepareStatement(nickTimestampUpdate) ;
		ps.setTimestamp(1, ts) ;
		ps.setInt(2, nickID) ;
		ps.execute() ;
		return millis ;
	}
	
	private int getID(String key, String preparedQuery, HashMap<String, Integer> cache) 
		throws SQLException {
		int id = -1 ;
		if(cache.containsKey(key)) {
			id = cache.get(key) ;
		} else {
			PreparedStatement ps = conn.prepareStatement(preparedQuery) ;
			ps.setString(1, key) ;
			ps.execute() ;
			ResultSet rs = ps.getResultSet() ;
			if (rs.next()) {
				id = rs.getInt(1) ;
				// cache the id we've just retrieved from the db
				cache.put(key, id) ;
			}
		}
		return id ;
	}
	
	private int getID(String name, String network, String preparedQuery, HashMap<String [], Integer> cache) 
		throws SQLException {
		int id = -1 ;
		String [] key = {name, network} ;
		if(cache.containsKey(key)) {
			id = cache.get(key) ;
		} else {
			PreparedStatement ps = conn.prepareStatement(preparedQuery) ;
			ps.setString(1, name) ;
			ps.setInt(2, getID(network, networkIdByName, networkIdCache)) ;
			ps.execute() ;
			ResultSet rs = ps.getResultSet() ;
			// getResultSet() can return null, but in theory it won't for this query.
			if (rs.next()) {
				id = rs.getInt(1) ;
				cache.put(key, id) ;
			}
		}
		return id ;
	}
	
	public int numTotalMessages() throws SQLException {
		int count = 0;
		PreparedStatement st = conn.prepareStatement(msgTotalCount);
		st.execute();
		ResultSet rs = st.getResultSet();
		if(rs.next())
			count = rs.getInt(1);
		return count;
	}
	
	public int numChannelMessages(String channel, String network) throws SQLException {
		int count = 0;
		PreparedStatement st = conn.prepareStatement(msgChannelCount);
		st.setInt(1, getID(channel, network, channelIdByNameAndNetwork, channelIdCache)) ;
		st.execute();
		ResultSet rs = st.getResultSet();
		if(rs.next())
			count = rs.getInt(1);
		return count;		
	}
	
	public int numNetworkMessages(String network) throws SQLException {
		int count = 0;
		PreparedStatement st = conn.prepareStatement(msgNetworkCount);
		st.setInt(1, getID(network, networkIdByName, networkIdCache)) ;
		st.execute();
		ResultSet rs = st.getResultSet();
		if(rs.next())
			count = rs.getInt(1);
		return count ;
	}
	
	public ResultSet getMessage(int id) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(msgById) ;
		ps.setInt(1, id) ;
		ps.execute() ;
		return ps.getResultSet() ;
	}
	
	public ResultSet getAllMessages() throws SQLException {
		PreparedStatement ps = conn.prepareStatement(allMessages) ;
		ps.execute() ;
		return ps.getResultSet() ;
	}
	
	public void printResultSet(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData() ;
		// spit out a header
		String f ;
		for(int i = 1; i <= md.getColumnCount(); i++) {
			f = "%" + (md.getColumnName(i).length() + 5) + "s" ;
			System.out.format(f, md.getColumnName(i)) ;
		}
		System.out.println() ;
		//and dump teh data
		int width = 0 ;
		while (rs.next()) {
			for(int i = 1; i <= md.getColumnCount(); i++) {
				width = md.getColumnName(i).length() + 4 ;
				f = " %" + width + "." + width + "s" ;
				System.out.format(f, rs.getObject(i)) ;
			}
			System.out.println() ;
		}
	}
}
