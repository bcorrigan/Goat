package goat.module;

import goat.core.Module;
import goat.core.Message;

/**
 * Things to do.
 * 
 * @author encontrado</b> All Rights Reserved.</p>
 */
public class TODO extends Module {

	private String todo = "Things to do as of $version$: fix ctcp #channel response; add option processing to Define; pick up hose from dry cleaners" ;

	public void TODO() {
		//TODO: implement something
	}
	
	public void processPrivateMessage(Message m) {
		m.createPagedReply("Not implemented.").send() ;
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m) ;
	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public String[] getCommands() {
		return new String[]{"TODO", "todo"};
	}
}
