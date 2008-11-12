package goat.module;

import java.util.Collection;

import goat.Goat;
import goat.core.Message;
import goat.core.Module;
import goat.core.Users;
import goat.util.CommandParser;


import net.roarsoftware.lastfm.User;
import net.roarsoftware.lastfm.Track;


/**
 * Allows users to make sure their exquisite music taste is always in other user's faces
 * Stole some lib that does most of the work
 * @author bc
 */
public class Lastfm extends Module {
	
	private String apiKey = "3085fb38b1bec1a008690cf4011c66e8";
	private String secret = "6d87a0b1ea6f89f23546c5cd30eb92c3";
	private Users users = Goat.getUsers();
	
	public static String[] getCommands() {
		return new String[]{"lastfm"};
	}
	
	@Override
	public void processChannelMessage(Message m) {
		goat.core.User user;
		if (users.hasUser(m.sender)) {
			user = users.getUser(m.sender);
		} else {
			user = new goat.core.User(m.sender);
		}
		
		CommandParser parser = new CommandParser(m.modTrailing);
		if("tracks".equalsIgnoreCase(parser.command())) {
			String lastfmUser;
			if(!parser.has("user")) {
				lastfmUser = user.getLastfmname();
				if("".equals(lastfmUser)) {
					m.createReply(m.sender + ": I don't know your lastfm name!").send();
					return;
				}
			} else lastfmUser = parser.get("user");
			Collection<Track> tracks = User.getRecentTracks(lastfmUser, apiKey);
			if( tracks.size()==0) {
				m.createReply("I don't find anything for the user " + lastfmUser + ", sorry." ).send();
				return;
			}
			String replyString="";
			int i=0;
			for( Track track: tracks) {
				i++;
				replyString += Message.BOLD + i + ":" + Message.NORMAL + track.getName() + " - " + track.getArtist() + " ";
			}
			m.createPagedReply(replyString).send();
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
		}
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

}
