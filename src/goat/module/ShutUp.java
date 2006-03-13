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
		joeyisms.add("huge boobs") ;
		if (fires(m.trailing, joeyisms)) {
			m.createReply("Shut up, joey.").send() ;
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
