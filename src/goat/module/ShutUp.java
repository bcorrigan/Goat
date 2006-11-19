package goat.module;

import goat.core.Message;
import goat.core.Module;
import java.util.*;

/**
 * Module to tell people to shut up.
 * 
 * @author rs
 *
 */
public class ShutUp extends Module {

	private Random random = new Random() ;
	
	public int messageType() {
		return WANT_ALL_MESSAGES ;
	}
	
	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	@Override
	public void processChannelMessage(Message m) {
		// TODO make this general, right now it's just a quick hack to fill a pressing need.
		
		ArrayList<String> joeyisms = new ArrayList<String>() ;
		joeyisms.add("dogs are fat") ;
		joeyisms.add("proliferate") ;
		joeyisms.add("proliferating") ;
		joeyisms.add("proliferation") ;
		joeyisms.add("Erica Campbell") ;
		joeyisms.add("huge boobs") ;
		joeyisms.add("regina") ;
		joeyisms.add("katie") ;
		joeyisms.add("marge") ;
		joeyisms.add("true pimp") ;
		joeyisms.add("nerd virgin") ;
		joeyisms.add("nergin") ;
		joeyisms.add("melons") ;
		joeyisms.add("power up") ;
		joeyisms.add("saiyan") ;
		joeyisms.add("vegeta ") ;
		joeyisms.add("freethinking athiests");
		joeyisms.add("totally gay");
	
		ArrayList<String> joeyResponses = new ArrayList<String>() ;
		joeyResponses.add("joey, shut the fuck up.") ;
		joeyResponses.add("joey, give it a fucking rest.") ;
		joeyResponses.add("joey, you're getting fucking boring again.") ;
		joeyResponses.add("joey: fuck off.  Now.") ;
		joeyResponses.add("joey, please shut the fuck up already.") ;
		joeyResponses.add("joey: shut up.") ;
		joeyResponses.add("joey, get your greasy little fingers away from the fucking computer.") ;
		joeyResponses.add("joey: fucking shut up.") ;
		joeyResponses.add("joey, have you stopped taking your medicine again?") ;

		if (fires(m.trailing, joeyisms)) {
			// debug
			// System.out.println("JOEYISM detected from umask: " + m.prefix) ;
			if(m.prefix.matches(".*\\.pacbell\\.net.*")) {
				// for now, we'll only respond to joey himself occasionally, 
				//  since he seems to be getting off on this.
				if (random.nextInt(100) < 34 )
					m.createReply(pickRandom(joeyResponses)).send() ;
				// debug
				//System.out.println("JOEY detected!") ;
			} else {
				m.createReply("Shut up, joey.").send() ;
			}
		}
	}
	
	/**
	 * check to see if any string in a list in a list of trigger strings is
	 * contained in the target string.
	 * 
	 * @param m
	 * @param phrases
	 * @return
	 */
	private boolean fires(String target, ArrayList<String> triggers) {
		Iterator<String> i = triggers.iterator() ;
		String s ;
		while (i.hasNext()) {
			s = i.next() ;
			if(target.toLowerCase().contains(s.toLowerCase()))
				return true ;
		}
		return false ;
	}

	private String pickRandom(ArrayList<String> strings) {
		return strings.get(random.nextInt(strings.size())) ;
	}
}
