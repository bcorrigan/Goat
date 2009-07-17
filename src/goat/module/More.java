package goat.module;

import goat.core.Module;
import goat.core.Message;


/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 * @title More
 * 
 *	@author encontrado
 * 
 * @version 1.0
 */
public class More extends Module {

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"more", "moar"};
   }
	
	public More() {
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		if (m.getModTrailing().trim().equals("")) {
			if (m.hasNextPage())
				m.createNextPage().send() ;
			//else
			//	m.createReply("No more :(").send() ;
		}
	}
}
