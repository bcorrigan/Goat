package goat.module;

import java.util.Collection;

import goat.Goat;
import goat.core.Message;
import goat.core.Module;
import goat.core.Users;


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
			user = new goat.core.User(m.sender) ;
		}
		String command, trailing;
		String[] words =  m.modTrailing.split("\\s+?"); //whitespace
		if( words==null || words.length==0 ) {
			m.createReply("What you say?").send();
			return;
		}
		for (int i=0; i<words.length; i++) {
			System.out.println("words" + i + ":" + words[i]);
		}
		command = words[0];
		trailing = m.modTrailing.replaceFirst("\\S*\\s*", ""); //first word and subsequent spaces

		if("tracks".equalsIgnoreCase(command)) {
			String lastfmUser;
			if(trailing.trim().length()==0) {
				lastfmUser = user.getLastfmname();
			} else lastfmUser = trailing.trim();
			Collection<Track> tracks = User.getRecentTracks(lastfmUser, apiKey);
			if( tracks.size()==0) {
				m.createReply("I don't find anything for the user " + lastfmUser + ", sorry." ).send();
				return;
			}
			String replyString="";
			for( Track track:tracks) {
				replyString += track.getName() + " by " + track.getArtist() + ", ";
			}
			replyString = replyString.substring(0, replyString.length()-2); //hehe
			m.createPagedReply(replyString).send();
		} else if ("setuser".equalsIgnoreCase(command)) {
			if(trailing.trim().length()==0) {
				m.createReply("You have to supply a username you enormous tit.").send();
			} else {
				if (! users.hasUser(user)) {
					users.addUser(user);
				}
				if (! user.getLastfmname().equals(trailing)) {
					user.setLastfmname(trailing.trim());
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
