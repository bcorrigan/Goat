package goat.module;

import java.util.Collection;

import goat.Goat;
import goat.core.Message;
import goat.core.Module;
import goat.core.Users;
import goat.util.CommandParser;


import net.roarsoftware.lastfm.User;
import net.roarsoftware.lastfm.Track;
import net.roarsoftware.lastfm.Event;
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

	private static final String TRACKS_USAGE = "lastfm [user=LASTFM_USER_NAME]";
	private static final String CHARTS_USAGE = "lastfm chart=(artists|albums|tracks) [type=(" + Message.BOLD + "weekly" + Message.NORMAL + "|loved|alltime)] [user=LASTFM_USER_NAME]";
	private static final String COUNTRY_CHART_USAGE = "lastfm country=\"FULL COUNTRY NAME\" [chart=(artists|" + Message.BOLD + "tracks" + Message.NORMAL + ")]";
	private static final String SETUSER_USAGE = "lastfm setuser LASTFM_USER_NAME";
	private static final String GENERAL_USAGE = "Defaults are in bold.  If your user name has been set with the setuser form of the command, then the user=LASTFM_USER_NAME parameter is optional.";

	// private static final String DEFAULT_COUNTRY = "United Kingdom";


	private String apiKey = "3085fb38b1bec1a008690cf4011c66e8";
	private String secret = "6d87a0b1ea6f89f23546c5cd30eb92c3";
	private Users users = Goat.getUsers();
	private CommandParser parser;
	private goat.core.User user;
	private String lastfmUser = "";

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
		if (users.hasUser(m.sender)) {
			user = users.getUser(m.sender);
		} else {
			user = new goat.core.User(m.sender);
		}
		parser = new CommandParser(m.modTrailing);
		lastfmUser = getLastfmUser(m);
		if(parser.has("country"))
			ircCountry(m);
		else if(parser.has("chart"))
			ircChart(m);
		else if ("setuser".equalsIgnoreCase(parser.command()))
			ircSetUser(m);
		else if("tracks".equalsIgnoreCase(parser.command()))
			ircTracks(m);
		else if("".equals(parser.command()) && !lastfmUser.equals(""))  // default, as long as we've got a lastfm user name
			ircTracks(m); 
		else
			ircUsage(m );
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
		else
			lastfmUser = user.getLastfmname();
		return lastfmUser;
	}

	private void ircTracks(Message m) {
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
			m.createReply(m.sender + ": You have to supply a username you enormous tit.").send();
		} else if (parser.remainingAsArrayList().size()>1) {
			m.createReply(m.sender + ": A lastfm username can't have spaces.").send();
		} else {
			if (! users.hasUser(user)) {
				users.addUser(user);
			}
			if (! user.getLastfmname().equals(parser.remaining())) {
				user.setLastfmname(parser.remaining().trim());
				users.save();
			}
			m.createReply(m.sender + ":  lastfm username set to \"" + user.getLastfmname() + "\"").send();
		}
	}

	private void ircChart(Message m) {
		ChartType type = null; // = DEFAULT_CHART_TYPE;
		ChartCoverage coverage = null; // = DEFAULT_COVERAGE_TYPE;

		for(ChartType t : ChartType.values())
			if(t.name().equalsIgnoreCase(parser.get("chart"))) {
				type = t;
				break;
			}
		if(null == type) {
			m.createReply(m.sender + ": I've never heard of your stupid \"" + parser.get("chart") + "\" chart. Right now I only support:  " + ChartCoverage.valuesAsString().replaceAll(" ", "|").toLowerCase()).send();
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
				m.createReply(m.sender + ": only tracks can be loved.").send();
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
				m.createReply(m.sender + ": only tracks can be loved.").send();
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

		String country = Message.removeFormattingAndColors(parser.get("country"));
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

	private void ircUsage(Message m) {
		String usage = "Usage:  " 
			+ "\"" + TRACKS_USAGE + "\"  " + Message.BOLD + "OR" + Message.NORMAL 
			+ "  \"" + CHARTS_USAGE + "\"  " + Message.BOLD + "OR" + Message.NORMAL 
			+ "  \"" + COUNTRY_CHART_USAGE + "\"  " + Message.BOLD + "OR" + Message.NORMAL 
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
			replyString += Message.BOLD + i + ":" + Message.NORMAL + track.getName() + " - " + track.getArtist() +
			":" + track.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send(); 
	}

	private void printAlbums(Collection<Album> albums, Message m) {
		String replyString="";
		int i=0;
		for( Album album: albums) {
			i++;
			replyString += Message.BOLD + i + ":" + Message.NORMAL + album.getName() + " - " + album.getArtist() +
			":" + album.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send();
	}

	private void printArtists(Collection<Artist> artists, Message m) {
		String replyString="";
		int i=0;
		for( MusicEntry musicEntry: artists) {
			i++;
			replyString += Message.BOLD + i + ":" + Message.NORMAL + musicEntry.getName() + " - "+ musicEntry.getPlaycount() + " plays ";
		}
		m.createPagedReply(replyString).send();
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

}
