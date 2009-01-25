package goat.module;

import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import goat.Goat;
import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.core.Users;
import goat.util.CommandParser;
import goat.core.IrcUser;


import net.roarsoftware.lastfm.User;
import net.roarsoftware.lastfm.Track;
//import net.roarsoftware.lastfm.Event;
import net.roarsoftware.lastfm.Album;
import net.roarsoftware.lastfm.Artist;
import net.roarsoftware.lastfm.MusicEntry;
import net.roarsoftware.lastfm.Geo;

/**
 * Allows users to make sure their exquisite music taste is always in other user's faces
 * Stole some lib that does most of the work
 * @author bc
 */
public class Lastfm extends Module {
	
	public boolean isThreadSafe() {
		return true;
	}

	private static final String TRACKS_USAGE = "lastfm [user=LASTFM_USER_NAME]";
	private static final String CHARTS_USAGE = "lastfm chart=(artists|albums|tracks) [type=(" + Constants.BOLD + "weekly" + Constants.NORMAL + "|loved|alltime)] [user=LASTFM_USER_NAME]";
	private static final String COUNTRY_CHART_USAGE = "lastfm country=\"FULL COUNTRY NAME\" [chart=(artists|" + Constants.BOLD + "tracks" + Constants.NORMAL + ")]";
	private static final String SETUSER_USAGE = "lastfm setuser LASTFM_USER_NAME";
	private static final String GENERAL_USAGE = "Default values are in bold.  If your user name has been " +
			"set with the " + Constants.UNDERLINE + "setuser" + Constants.NORMAL + " form of the command, " +
			"then the user=LASTFM_USER_NAME parameter is optional.  The first form of the command, with " +
			"only the username set, reports recent tracks.  The " + Constants.UNDERLINE + "chart=" +
			Constants.NORMAL + " form reports various charts for the user specified.  The " +
			Constants.UNDERLINE + "country=" + Constants.NORMAL + " form supplies track or artist " +
			"charts for a given nation.";

	// private static final String DEFAULT_COUNTRY = "United Kingdom";


	private String apiKey = "3085fb38b1bec1a008690cf4011c66e8";
	//private String secret = "6d87a0b1ea6f89f23546c5cd30eb92c3";
	private Users users = Goat.getUsers();
	private CommandParser parser;
	//private goat.core.User user;
	//private String lastfmUser = "";

	enum ChartCoverage {
		WEEKLY,LOVED,ALLTIME;	
		public static String valuesAsString() {
			String ret = "";
			for(ChartCoverage value: values()) {
				ret += value.name() + " ";
			}
			return ret.substring(0,ret.length());
		}
	}

	private static final ChartCoverage DEFAULT_COVERAGE_TYPE = ChartCoverage.WEEKLY;

	enum ChartType {
		ARTISTS, ALBUMS, TRACKS;
		public static String valuesAsString() {
			String ret = "";
			for(ChartType value: values()) {
				ret += value.name() + " ";
			}
			return ret.substring(0,ret.length() - 1);
		}
	}

	// private static final ChartType DEFAULT_CHART_TYPE = ChartType.TRACKS;

	public static String[] getCommands() {
		return new String[]{"lastfm", "nowplaying"};
	}

	@Override
	public void processChannelMessage(Message m) {
		parser = new CommandParser(m.getModTrailing());
		String lastfmUser = getLastfmUser(m);
		if("nowplaying".equalsIgnoreCase(m.getModCommand())
				|| "nowplaying".equalsIgnoreCase(parser.command()))
			ircNowPlaying(m);
		else if(parser.has("country"))
			ircCountry(m);
		else if(parser.has("chart"))
			ircChart(m, lastfmUser);
		else if ("setuser".equalsIgnoreCase(parser.command()))
			ircSetUser(m);
		else if("tracks".equalsIgnoreCase(parser.command()))
			ircTracks(m, lastfmUser);

		else if("".equals(parser.command()) && !lastfmUser.equals(""))  // default, as long as we've got a lastfm user name
			ircTracks(m, lastfmUser); 
		else
			ircUsage(m, lastfmUser);
	}

	private String getLastfmUser(Message m) {
		String lastfmUser = "";
		if(parser.has("user"))
			lastfmUser = parser.get("user");
		else if(parser.has("ircuser"))
			if(users.hasUser(parser.get("ircuser"))) {
				goat.core.User u = users.getUser(parser.get("ircuser"));
				if (null != u.getLastfmname() && !"".equals(u.getLastfmname()))
					lastfmUser = users.getUser(parser.get("ircuser")).getLastfmname();
				else
					m.createReply(u.getName() + " doesn't have a LastFM username set.").send();
			}
			else
				m.createReply("I've never heard of \"" + parser.get("ircuser") + "\".").send();
		else if(users.hasUser(m.getSender()))
			lastfmUser = users.getUser(m.getSender()).getLastfmname();
		return lastfmUser;
	}

	private void ircTracks(Message m, String lastfmUser) {
		if (lastfmUser.equals("")) {
			m.createReply("I don't know your lastfm username; set it with \"lastfm setuser [yourname]\", or specify a name with \"lastfm tracks user=[lastfm_user_name]\"").send();
			return;
		}
		Collection<Track> tracks = User.getRecentTracks(lastfmUser, apiKey);
		if (tracks.isEmpty()) {
			m.createReply("I don't have any tracks for lastfm user \"" + lastfmUser + "\"").send();
			return;
		}
		printTracks(tracks,m);
	}

	private void ircSetUser(Message m) {
		if(parser.remaining().length()==0) {
			m.createReply(m.getSender() + ": You have to supply a username you enormous tit.").send();
		} else if (parser.remainingAsArrayList().size()>1) {
			m.createReply(m.getSender() + ": A lastfm username can't have spaces.").send();
		} else {
			users.getOrCreateUser(m.getSender()).setLastfmname(parser.remaining().trim());
			m.createReply(m.getSender() + ":  lastfm username set to \"" + users.getUser(m.getSender()).getLastfmname() + "\"").send();
		}
	}

	private void ircChart(Message m, String lastfmUser) {
		
		if(null == lastfmUser || lastfmUser.equals("")) {
			m.createPagedReply("You need to supply a username to chart for, with the user=LASTFM_USER_NAME or ircuser=IRC_USER_NAME options, or with 'lastfm setuser YOUR_USER_NAME' to set a default username.  You can also use the form 'lastfm country=\"FULL NATION NAME\" <type=(artists|tracks)>' for top tracks or artists in a given nation.").send();
			return;
		}
		ChartType type = null; // = DEFAULT_CHART_TYPE;
		ChartCoverage coverage = null; // = DEFAULT_COVERAGE_TYPE;

		for(ChartType t : ChartType.values())
			if(t.name().equalsIgnoreCase(parser.get("chart"))) {
				type = t;
				break;
			}
		if(null == type) {
			m.createReply(m.getSender() + ": I've never heard of your stupid \"" + parser.get("chart") + "\" chart. Right now I only support:  " + ChartCoverage.valuesAsString().replaceAll(" ", "|").toLowerCase()).send();
			return;
		}

		if(parser.has("type")) {
			for(ChartCoverage c: ChartCoverage.values())
				if(c.name().equalsIgnoreCase(parser.get("type"))) {
					coverage = c;
					break;
				}
			if(null == coverage) {
				m.createReply("invalid chart coverage type \"" + parser.get("type") + "\", valid types are:  " + ChartCoverage.valuesAsString().replaceAll(" ", "|").toLowerCase()).send();
				return;
			}
		} else {
			coverage = DEFAULT_COVERAGE_TYPE;
		}

		if(ChartType.ALBUMS == type) {
			Collection<Album> albums = null;
			if(ChartCoverage.ALLTIME == coverage)
				albums = User.getTopAlbums(lastfmUser, apiKey);
			else if(ChartCoverage.WEEKLY == coverage)
				albums = User.getWeeklyAlbumChart(lastfmUser, apiKey).getEntries();
			else if(ChartCoverage.LOVED == coverage) {
				m.createReply(m.getSender() + ": only tracks can be loved.").send();
				return;
			} 
			if (albums.isEmpty()) {
				m.createReply("LastFM doesn't have a " + coverage.name().toLowerCase() + " album chart for user \"" + lastfmUser +"\".").send();
				return;
			} else
				printAlbums(albums,m);
		} else if(ChartType.ARTISTS == type) {
			Collection<Artist> artists = null;
			if(ChartCoverage.ALLTIME == coverage)
				artists = User.getTopArtists(lastfmUser, apiKey);
			else if(ChartCoverage.WEEKLY == coverage)
				artists = User.getWeeklyArtistChart(lastfmUser, apiKey).getEntries();
			else if(ChartCoverage.LOVED == coverage) {
				m.createReply(m.getSender() + ": only tracks can be loved.").send();
				return;
			}
			if (artists.isEmpty()) {
				m.createReply("LastFM doesn't have a " + coverage.name().toLowerCase() + " artists chart for user \"" + lastfmUser +"\".").send();
				return;
			} else
				printArtists(artists,m);
		} else if(ChartType.TRACKS == type) {
			Collection<Track> tracks = null;
			if(ChartCoverage.ALLTIME == coverage)
				tracks = User.getTopTracks(lastfmUser, apiKey);
			else if(ChartCoverage.WEEKLY == coverage)
				tracks = User.getWeeklyTrackChart(lastfmUser, apiKey).getEntries();
			else if(ChartCoverage.LOVED == coverage) {
				tracks = User.getLovedTracks(lastfmUser, apiKey);
			}
			if (tracks.isEmpty()) {
				m.createReply("LastFM doesn't have a " + coverage.name().toLowerCase() + " tracks chart for user \"" + lastfmUser +"\".").send();
				return;
			} else
				printTracks(tracks,m);
		}
	}

	private void ircCountry(Message m) {

		String country = Constants.removeFormattingAndColors(parser.get("country"));
		ChartType type = null;
		for(ChartType t : ChartType.values())
			if(t.name().equalsIgnoreCase(parser.get("chart"))) {
				type = t;
				break;
			}
		if (ChartType.ALBUMS == type) {
			m.createReply("Sorry, lastfm won't let you see which albums are most popular in a given country.").send();
			return;
		} else if (! parser.has("chart")) {
			type = ChartType.TRACKS;
		}

		if(ChartType.TRACKS == type) {
			Collection<Track> tracks = Geo.getTopTracks(country, apiKey);
			if(tracks.size() == 0) 
				m.createReply("It would appear LastFM doesn't do any charting for the country \"" + country + "\".").send();
			else
				printTracks(tracks, m);
		} else if (ChartType.ARTISTS == type) {
			Collection<Artist> artists = Geo.getTopArtists(country, apiKey);
			if(artists.size() == 0) 
				m.createReply("It would appear LastFM doesn't do any charting for the country \"" + country + "\".").send();
			else
				printArtists(artists, m);
		} else 
			m.createReply("I don't know anything about your weird \"" + parser.get("chart") + "\" chart.  Valid chart types are: artists|tracks" ).send();
	}

	private void ircNowPlaying(Message m) {
		
		Module module = Goat.modController.getLoaded(ServerCommands.class);
		ServerCommands sc = null;
		if(module instanceof ServerCommands) 
			sc = (ServerCommands) module;
		if(null != sc && sc.isRunning()) {
			if(! m.getChanname().startsWith("#")) {
				m.createReply("I only do nowplaying for channels, not in private.  Pervert.").send();
				return;
			}
			List<IrcUser> ircUsers;
			try {
				 ircUsers = sc.who(m.getChanname());
			} catch (SocketTimeoutException ste) {
				m.createReply("Timed out waiting for the server to tell me who's around.").send();
				return;
			}
			List<goat.core.User> usersToCheck = new ArrayList<goat.core.User>();
			List<goat.core.User> scaredUsers = new ArrayList<goat.core.User>();
			for (IrcUser iu: ircUsers) {
				if(users.hasUser(iu.getNick())) {
					goat.core.User u = users.getUser(iu.getNick());
					if(u.getLastfmname() != null && !"".equals(u.getLastfmname()))
						usersToCheck.add(u);
					else
						scaredUsers.add(u);
				}
			}
			if(usersToCheck.isEmpty()) {
				m.createReply("Nobody in this channel wants to share their precious musical tastes " +
						"with the corporate hegemon at LastFM").send();
				return;
			}
			Map<goat.core.User, Track> results = new HashMap<goat.core.User, Track>();
			for(goat.core.User u: usersToCheck) {
				Collection<Track> tracks = User.getRecentTracks(u.getLastfmname(), apiKey);
				for(Track track: tracks) {
					if(track.isNowPlaying()) {
						results.put(u, track);
					}
				}
			}
			if(results.isEmpty())
				m.createReply("This channel is sunk in a deep silence, shunning all music and probably all humanity, too.").send();
			else if(results.size() == 1 && results.keySet().iterator().next().getName().equals(m.getSender())) {
				Track t = results.values().iterator().next();
				String reply = "You're the only one in the channel listening to music.  And you want " +
						"everyone to know it, don't you.  You want them to know that you're listening to ";
				reply += Constants.BOLD + t.getName() + Constants.NORMAL;
				reply += " by " + Constants.BOLD + t.getArtist() + Constants.NORMAL;
				reply += ", and now we all know it, don't we.  Are you happy now?  Are you?";
				m.createPagedReply(reply).send();
			} else {
				String reply = "";
				for(goat.core.User u: results.keySet()) {
					Track val = results.get(u);
					reply += Constants.BOLD + u.getName() + Constants.NORMAL + ": ";
					reply += val.getName() + " by " + val.getArtist() + "  ";
				}
				m.createPagedReply(reply).send();
			}
				
		} else {
			m.createReply("I can't do that without the ServerCommands module running").send();
		}			
	}
	
	private void ircUsage(Message m, String lastfmUser) {
		String usage = "Usage:  " 
			+ "\"" + TRACKS_USAGE + "\"  " + Constants.BOLD + "OR" + Constants.NORMAL 
			+ "  \"" + CHARTS_USAGE + "\"  " + Constants.BOLD + "OR" + Constants.NORMAL 
			+ "  \"" + COUNTRY_CHART_USAGE + "\"  " + Constants.BOLD + "OR" + Constants.NORMAL 
			+ "  \"" + SETUSER_USAGE + "\"  ";
		m.createPagedReply(usage).send();
		if("".equals(lastfmUser) 
				|| parser.command().equalsIgnoreCase("help") 
				|| parser.command().equalsIgnoreCase("usage")) {
			m.createReply(GENERAL_USAGE).send();
		}
	}

	private void printTracks(Collection<Track> tracks, Message m ) {
		String replyString="";
		int i=0;
		boolean playsIndicated = false;
		for( Track track: tracks) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + " "; 
			replyString += track.getName() + Constants.BOLD + " \u2014 " + Constants.NORMAL + track.getArtist();
			if(track.getPlaycount() > 1) {
				replyString += " (" + track.getPlaycount();
				if(!playsIndicated) {
					replyString += " plays";
					playsIndicated = true;
				}
				replyString += ")";
			}
			replyString += "  ";
		}
		m.createPagedReply(replyString).send(); 
	}

	private void printAlbums(Collection<Album> albums, Message m) {
		String replyString="";
		int i=0;
		boolean playsIndicated = false;
		for( Album album: albums) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + " "; 
			replyString += album.getName() + Constants.BOLD + " \u2014 " + Constants.NORMAL + album.getArtist();
			if(album.getPlaycount() > 1) {
				replyString += " (" + album.getPlaycount();
				if(!playsIndicated) {
					replyString += " plays";
					playsIndicated = true;
				}
				replyString += ")";
			}
			replyString += "  ";
		}
		m.createPagedReply(replyString).send();
	}

	private void printArtists(Collection<Artist> artists, Message m) {
		String replyString="";
		int i=0;
		boolean playsIndicated = false;
		for( MusicEntry musicEntry: artists) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + " "; 
			replyString += musicEntry.getName();
			if(musicEntry.getPlaycount() > 1) {
				replyString += " (" + musicEntry.getPlaycount();
				if(!playsIndicated) {
					replyString += " plays";
					playsIndicated = true;
				}
				replyString += ")";
			}
			replyString += "  ";
		}
		m.createPagedReply(replyString).send();
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

}
