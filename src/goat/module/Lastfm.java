package goat.module;

import java.util.Collection;

import goat.Goat;
import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.core.Users;
import goat.util.CommandParser;


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
	private static final String GENERAL_USAGE = "Defaults are in bold.  If your user name has been set with the setuser form of the command, then the user=LASTFM_USER_NAME parameter is optional.";

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
		return new String[]{"lastfm"};
	}

	@Override
	public void processChannelMessage(Message m) {
		parser = new CommandParser(m.getModTrailing());
		String lastfmUser = getLastfmUser(m);
		if(parser.has("country"))
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
		for( Track track: tracks) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + track.getName() + " - " + track.getArtist() +
			":" + track.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send(); 
	}

	private void printAlbums(Collection<Album> albums, Message m) {
		String replyString="";
		int i=0;
		for( Album album: albums) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + album.getName() + " - " + album.getArtist() +
			":" + album.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send();
	}

	private void printArtists(Collection<Artist> artists, Message m) {
		String replyString="";
		int i=0;
		for( MusicEntry musicEntry: artists) {
			i++;
			replyString += Constants.BOLD + i + ":" + Constants.NORMAL + musicEntry.getName() + " - "+ musicEntry.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send();
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

}
