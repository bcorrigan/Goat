package goat.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.sql.Timestamp;
import java.sql.SQLException;

import goat.Goat;
import goat.core.BotStats;
import goat.core.Constants;
import goat.db.IRCLogger;

/**
 * Class to parse irssi logfiles and cram them into the goat db.
 * 
 * There's a lot of hard-coded stuff in here, it would probably
 * take some careful modification to make it work for anything
 * other than default irssi logfiles of slashnet channels.
 * 
 * @author rs
 *
 */
public class LogFileLoader {

	public static TimeZone goatTimeZone = TimeZone.getTimeZone("PST") ;
	private static final String HEADER_PREFIX = "--- Log opened" ;
	private static final String FOOTER_PREFIX = "--- Log closed" ;
	private static final String DAY_CHANGE_PREFIX = "--- Day changed" ;
	private static final String NETWORK = "slashnet" ;
	private static long START_TIME = 0 ;
	private static final String BOT_NICK = "goat" ;
	private static SimpleDateFormat headerDateFormat ;
	private static SimpleDateFormat daychangeDateFormat ;
	private static IRCLogger logger ;
	
	private static long lastSeenMillis ;
	private static long millisOffset ;
	private static Date logDate = new Date(0) ;
	private static String self_nick = "" ;
	private static String self_hostmask = "" ;
	private static String lastLine = "" ;
	private static String lastSeenMillisLine = "";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger = new IRCLogger() ;
		System.out.println("Starting logfile reader") ;
		START_TIME = System.currentTimeMillis() ;
		headerDateFormat = new SimpleDateFormat("MMM d HH:mm:ss yyyy") ;
		daychangeDateFormat = new SimpleDateFormat("MMM d yyyy") ;
		//logger = new IRCLogger() ;
		System.out.println("Starting logfile reading at " + (new Date(START_TIME)).toString()) ;
        for (String arg : args) {
            processFile(arg);
        }
        try {
			//logger.printResultSet(logger.getAllMessages()) ;
			System.out.println() ;
			System.out.println(logger.numTotalMessages() + " messages in db.") ;
		} catch (SQLException e) {
			e.printStackTrace() ;
		}
		System.out.println("\nFinished.  Press enter to exit.") ;
		try {
			System.in.read() ;
		} catch (IOException e) {
			
		}
		System.exit(0) ;
	}
	
	private static void processFile(String filename) {
		System.out.println("FILE: " + filename);
		
		logDate = new Date(START_TIME + 1) ;
		lastSeenMillis = 0 ;
		millisOffset = 0 ;
		String channel = "" ;
		BufferedReader br;
		String line = "" ;
		
		int offset1 = filename.indexOf("#");
		int offset2 = filename.indexOf(".");
		if ((offset1 > -1) && (offset2 > -1))
			channel = filename.substring(offset1, offset2);
		else {
			System.out.println("  Could not determine channel from filename \""
					+ filename + "\", skipping.");
			return;
		}
		try {
			br = new BufferedReader(new FileReader(filename));
			line = br.readLine();
		} catch (FileNotFoundException e) {
			System.out.println("  File not found: \"" + filename
					+ "\", skipping.");
			return;
		} catch (IOException e) {
			System.out.println("  Problem reading first line of file \""
					+ filename + "\", skipping file.");
			return;
		}
		if (line.startsWith(HEADER_PREFIX)) {
			try {
				logDate = headerDateFormat.parse(line.substring(HEADER_PREFIX
						.length() + 5));
			} catch (ParseException e) {
				System.out.println("  Could not parse date in header, \""
						+ line.substring(HEADER_PREFIX.length() + 5)
						+ "\", skipping file.");
				// might fall back on filename here
				return;
			}
		} else {
			System.out.println("  Unrecognized logfile header, skipping file.");
			return;
		}
		//System.out.println("  Time in log header: " + logDate.toString());
		int fails = 0 ;
		try {
			while(null != (line = br.readLine())) {
				processLine(line, channel) ;
			}
			br.close() ;
			// it turns out this next is a really bad idea, once your database
			// gets to be fairly large.
			//logger.hsqldbCheckpoint();
			//System.out.println("Cached items: " + logger.cacheSize()) ;
		} catch (IOException e) {
			if (fails > 10) {
				System.out.println("  Too many read failures, aborting file \"" + filename + "\"") ;
				return ;
			} else {
				System.out.println("  Problem reading file \"" + filename + "\", continuing." ) ;
				e.printStackTrace() ;
				fails++ ;
				try {
					// wait half a second before retrying
					Thread.sleep(500) ;
				} catch (InterruptedException ie) {				
				}
			}
		}
	}
	
	private static int processLine(String line, String channel) {
		int id = -1 ;
		String hour = "" ;
		String minute = "" ;
		String nick = "" ;
		String hostmask = "" ;
		String body = "" ;
		String ircCommand = "" ;
		String ctcpCommand = "" ;
		String botCommand = "" ;
		if (2 == line.indexOf(":")) {
			hour = line.substring(0,2) ;
			minute = line.substring(3,5) ;
		} else if (line.startsWith(DAY_CHANGE_PREFIX)) {
			updateLogDate(DAY_CHANGE_PREFIX, line, daychangeDateFormat) ;
			return id;
		} else if (line.startsWith(HEADER_PREFIX)) {
			updateLogDate(HEADER_PREFIX, line, headerDateFormat) ;
			return id;
		} else if (line.startsWith(FOOTER_PREFIX)) {
			//ignore log close messages
			return id ;
		} else {
			System.out.println("  Um, found line without timestamp? -- " + line) ;
			return id;
		}
		String subline ;	
		if (6 == line.indexOf("<")) {
			// PRIVMSG (normal message)
			ircCommand = "PRIVMSG" ;
			nick = line.substring(8, line.indexOf(">")) ;
			//hostmask = getLastHostmask(nick) ;
			body = line.substring(line.indexOf(">") + 2) ;
			StringTokenizer st = new StringTokenizer(body);
			String firstWord = "";
			if (st.hasMoreTokens()) {
				firstWord = st.nextToken() ;
			}
			if ((!firstWord.toLowerCase().matches(BOT_NICK.toLowerCase() + "\\w+"))
                    && firstWord.toLowerCase().matches(BOT_NICK.toLowerCase() + "\\W*")) {
				if (st.hasMoreTokens()) {
					firstWord = st.nextToken();
				}
			}
			if(firstWord.equals("")) {
				// don't log blank user messages
				return id ;
			}
			firstWord = Constants.removeFormattingAndColors(firstWord);
			if (BotStats.getInstance().isLoadedCommand(firstWord)) {
				botCommand = firstWord ;
			}
			// System.out.println("  PRIVMSG from: " + nick + " | " + body) ;
		} else if ( 7 == line.indexOf("*")) {
			// ACTION
			ircCommand = "PRIVMSG" ;
			ctcpCommand = "ACTION" ;
			subline = line.substring(9) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			//hostmask = getLastHostmask(nick) ;
			body = subline.substring(subline.indexOf(" ") + 1) ;
			// System.out.println("  ACTION by " + nick + " ! " + body) ;
		} else if (line.contains("] has joined #")) {
			// JOIN
			ircCommand = "JOIN" ;
			subline = line.substring(10) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			// nick can contain ] and [, ouch.
			subline = subline.substring(subline.indexOf(" ") + 1) ;
			hostmask = subline.substring(subline.indexOf("[") + 1, subline.indexOf("]")) ;
			body = channel ;
			//System.out.println("  JOIN by " + nick + "[" + hostmask + "]") ;
		} else if(line.contains("] has left #")) {
			// PART
			ircCommand = "PART" ;
			subline = line.substring(10) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			subline = subline.substring(subline.indexOf(" ") + 1) ;
			hostmask = subline.substring(subline.indexOf("[") + 1, subline.indexOf("]")) ;
			body = channel ;
			// System.out.println("  PART by " + nick + "[" + hostmask + "]") ;
		} else if(line.contains("] has quit [")) {
			// QUIT
			ircCommand = "QUIT" ;
			subline = line.substring(10) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			subline = subline.substring(subline.indexOf(" ") + 1) ;
			hostmask = subline.substring(subline.indexOf("[") + 1, subline.indexOf("]")) ;
			//TODO body should be quit message
			body = ircCommand ;
			//System.out.println("  QUIT by " + nick + "[" + hostmask + "]") ;
		} else if(line.contains(" is now known as ")) {
			// NICK
			ircCommand = "NICK" ;
			String phrase = " is now known as " ;
			subline = line.substring(10) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			//hostmask = getLastHostmask(nick) ;
			body = line.substring(line.indexOf(phrase) + phrase.length()) ;
			//System.out.println("  NICK from " + nick + " to " + body) ;
		} else if (line.contains(" -!- mode/")){
			// mode command, ignore.
			return id;
		} else if (line.contains(" -!- ServerMode")) {
			// servermode command, ignore.
			return id;
		} else if (line.contains(" -!- Irssi: ")) {
			// irssi client command or message, ignore.
			return id;
		} else if (line.contains(" -!- Netsplit ")) {
			// ignore netsplit crap
			return id;
		} else if (line.contains(" was kicked from #")) {
			// ignore kick messages
			return id;
		} else if (line.contains(" -!- You're now known as ")) {
			//self NICK messages.
			//  this is a little strange, because we don't know who we are when we open a log file.
			ircCommand = "NICK" ;
			String phrase = "You're now known as " ;
			String new_nick = line.substring(10 + phrase.length()) ;
			body = new_nick ;
			if (self_nick.equals(""))
				nick = new_nick;
			else  
				nick = self_nick;
			if (self_hostmask.equals(""))
				self_hostmask = getLastHostmask(self_nick) ;
			if (self_hostmask.equals(""))
				self_hostmask = getLastHostmask(new_nick) ;
			self_nick = new_nick ;
			hostmask = self_hostmask ;
		} else if (line.contains(" changed the topic of #")) {
			//TOPIC message
			ircCommand = "TOPIC" ;
			subline = line.substring(10) ;
			nick = subline.substring(0, subline.indexOf(" ")) ;
			subline = subline.substring(subline.indexOf(" ") + 1) ; // move past the nick
			subline = subline.substring(subline.indexOf("#")) ; // move past the filler
			body = subline.substring(subline.indexOf(" ") + 5) ; // move past "#channel to: "
			//hostmask = getLastHostmask(nick) ;
 		} else {
			// ???
			// other things.
			// ignore, even though we should probably do something.
			System.err.println("  ignoring OTHER message: " + line) ;
			return id;
		}
		if (nick.equals(""))
			return id;  // don't log if we don't have a sender
		if (hostmask.equals("")) 
			hostmask = getLastHostmask(nick) ; // try to set hostmask if we don't already have it
		Calendar cal = Calendar.getInstance() ;
		cal.setTime(logDate) ;
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour)) ;
		cal.set(Calendar.MINUTE, Integer.parseInt(minute)) ;
		long thisMillis = cal.getTimeInMillis() ;
		if (thisMillis > lastSeenMillis) {
			lastSeenMillis = thisMillis ;
			lastSeenMillisLine = line ;
			millisOffset = 0 ;
		} else if(thisMillis == lastSeenMillis) {
			millisOffset += 100 ;
			thisMillis += millisOffset ;
		} else {
			System.err.println("  Warning: time going backwards, aborting file.") ;
			System.err.println("    last w/distinct time: " + lastSeenMillisLine) ;
			System.err.println("                    last: " + lastLine) ;
			System.err.println("                    this: " + line) ;
			return id ;
		}
		if (thisMillis < START_TIME) {
			Timestamp timestamp = new Timestamp(thisMillis) ; 
			try {
				id = logger.logMessage(timestamp, nick, hostmask, channel, NETWORK, ircCommand, ctcpCommand, botCommand, body) ;
				// logger.printResultSet(logger.getMessage(id)) ;
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("  SQL problem while logging message: " + line) ;
				System.err.println("    timestamp   : " + timestamp ) ;
				System.err.println("    nick        : " + nick ) ;
				System.err.println("    hostmask    : " + hostmask ) ;
				System.err.println("    channel     : " + channel ) ;
				System.err.println("    network     : " + NETWORK ) ;
				System.err.println("    ircCommand  : " + ircCommand ) ;
				System.err.println("    ctcpCommand : " + ctcpCommand ) ;
				System.err.println("    botCommand  : " + botCommand ) ;
				System.err.println("    body        : " + body ) ;
			}
		} else {
			System.out.println("  log line is more recent than start of script, skipping line.") ;
		}
		lastLine = line ;	
		return id ;
	}
	
	private static void updateLogDate(String prefix, String line, SimpleDateFormat df) {
		Date d = new Date(0) ;
		try {
			d = df.parse(line.substring(prefix.length() + 5));
		} catch (ParseException e) {
		}
		if (0 == d.getTime()) { 
			System.out.println("  Could not parse date in day change, \""
					+ line.substring(prefix.length() + 5)
					+ "\", aborting entire clambake.");
			System.exit(1) ;
		}
		logDate = d ;
		millisOffset = d.getTime() % (60 * 1000) ;
		lastSeenMillis = d.getTime() - millisOffset ;
	}
	
	private static String getLastHostmask(String nick) {
		String hostmask = "" ;
		try {
			hostmask = logger.getLastHostmask(nick) ;
		} catch (SQLException e) {
			System.out.println("problem looking up last hostmask for : " + nick) ;
			e.printStackTrace() ;
		}
		return hostmask ;
	}
}
