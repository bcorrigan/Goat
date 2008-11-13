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
	
	private String apiKey = "3085fb38b1bec1a008690cf4011c66e8";
	private String secret = "6d87a0b1ea6f89f23546c5cd30eb92c3";
	private Users users = Goat.getUsers();
	private CommandParser parser;
	private goat.core.User user;
	
	enum ChartType {
		WEEKLY,LOVED,ALLTIME,COUNTRY;
	}
	
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
		if("tracks".equalsIgnoreCase(parser.command())) {
			String lastfmUser = getUser(m);
			if (lastfmUser.equals("")) 
				return;
			Collection<Track> tracks = User.getRecentTracks(lastfmUser, apiKey);
			if (!haveResults(tracks,m))
				return;
			printTracks(tracks,m);
		} else if ("setuser".equalsIgnoreCase(parser.command())) {
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
				m.createReply(m.sender + ": set.").send();
			}
		} if("chart".equalsIgnoreCase(parser.command())) {
			String lastfmUser = "";
			if(!parser.has("country"))
				lastfmUser = getUser(m);
			ChartType type = ChartType.ALLTIME;
			String country="";
			if (lastfmUser.equals("") && !parser.has("country")) 
				return;
			if(parser.has("type")) {
				if(parser.get("type").equals("weekly"))
					type=ChartType.WEEKLY;
				else if(parser.get("type").equals("loved"))
					type=ChartType.LOVED;
				else if(parser.get("type").equals("alltime"))
					type=ChartType.ALLTIME;
				else if(parser.get("type").equals("country")) {
					type=ChartType.COUNTRY;
					if(parser.has("country")) {
						country = parser.get("country");
					} else country="United Kingdom";
				} else {
					m.createReply(m.sender + ": I've never heard of that stupid type. Right now I only support weekly, loved, country and alltime.").send();
					return;
				}
				if(!parser.get("type").equals("country") && parser.has("country") ) {
					m.createReply(m.sender + ": You can't ask for country chart ratings of that type - only of type country").send();
					return;
				}
			} //if parser has country and type is not set just set type to country
			else if(parser.has("country")) {
				type = ChartType.COUNTRY;
				country = parser.get("country");
			}
			if(parser.hasRemaining()) {
				if(parser.remaining().equals("albums")) {
					Collection<Album> albums = null;
					if(type==ChartType.ALLTIME)
						albums = User.getTopAlbums(lastfmUser, apiKey);
					else if(type==ChartType.WEEKLY)
						albums = User.getWeeklyAlbumChart(lastfmUser, apiKey).getEntries();
					else if(type==ChartType.LOVED) {
						m.createReply(m.sender + ": only tracks can be loved.").send();
						return;
					} else if(type==ChartType.COUNTRY) {
						m.createReply(m.sender + ": only tracks and artists have country ratings.").send();
						return;
					}
						
					if (!haveResults(albums,m))
						return;
					printAlbums(albums,m);
				} else if(parser.remaining().equals("artists")) {
					Collection<Artist> artists = null;
					if(type==ChartType.ALLTIME)
						artists = User.getTopArtists(lastfmUser, apiKey);
					else if(type==ChartType.WEEKLY)
						artists = User.getWeeklyArtistChart(lastfmUser, apiKey).getEntries();
					else if(type==ChartType.LOVED) {
						m.createReply(m.sender + ": only tracks can be loved.").send();
						return;
					} else if(type==ChartType.COUNTRY) {
						artists = Geo.getTopArtists(country, apiKey);
					}
					
					if (!haveResults(artists,m))
						return;
					printMusicEntry(artists,m);
				} else if(parser.remaining().equals("tracks")) {
					Collection<Track> tracks = null;
					if(type==ChartType.ALLTIME)
						tracks = User.getTopTracks(lastfmUser, apiKey);
					else if(type==ChartType.WEEKLY)
						tracks = User.getWeeklyTrackChart(lastfmUser, apiKey).getEntries();
					else if(type==ChartType.LOVED) {
						tracks = User.getLovedTracks(lastfmUser, apiKey);
					} else if(type==ChartType.COUNTRY) {
						tracks = Geo.getTopTracks(country, apiKey);
					}
					
					if (!haveResults(tracks,m))
						return;
					printTracks(tracks,m);
				}
				else {
					m.createReply(m.sender + ": I don't know what that is - choose from artists, albums and tracks").send();
				}
			} else {
				m.createReply(m.sender + ": You need to supply a type of record you want a chart of - choose from artists, albums and tracks").send();
			} 
		} 
	}

	private String getUser(Message m) {
		String lastfmUser;
		if(!parser.has("user")) {
			lastfmUser = user.getLastfmname();
			if("".equals(lastfmUser)) {
				m.createReply(m.sender + ": I don't know your lastfm name!").send();
			}
		} else lastfmUser = parser.get("user");
		return lastfmUser;
	}
	
	private boolean haveResults(Collection col,Message m) {
		if( col.size()==0) {
			m.createReply(m.sender + ": I don't find anything for that, sorry." ).send();
			return false;
		}
		return true;
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
	
	private void printMusicEntry(Collection<Artist> artists, Message m) {
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
