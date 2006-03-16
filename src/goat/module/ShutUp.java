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
		
		ArrayList<String> joeyisms = new ArrayList() ;
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
		joeyisms.add("melons") ;
		joeyisms.add("power up") ;
		joeyisms.add("saiyan") ;
		joeyisms.add("vegeta") ;
		if (fires(m.trailing, joeyisms)) {
			if(m.sender.equalsIgnoreCase("joey")) {
				// for now, we'll only respond to joey himself occasionally, 
				//  since he seems to be getting off on this.
				if (random.nextInt(100) < 23 )
					m.createReply("joey, shut the fuck up.").send() ;
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
}
