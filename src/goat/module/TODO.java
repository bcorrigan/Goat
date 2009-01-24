package goat.module;

import goat.core.Module;
import goat.core.Message;

/**
 * Things to do.
 * 
 * @author encontrado</b> All Rights Reserved.</p>
 */
public class TODO extends Module {

	private String todo = 
		"get rid of the Thread.stop()s, " + 
		"make a go-get-web-page-using thread method somewhere, and use that whenever goat asks the interweb for something" +
		"clean the fridge";
	
	public TODO() {
	}
	
	public void processPrivateMessage(Message m) {
		if (m.getCommand().equalsIgnoreCase("reallytodo"))
			m.createPagedReply(todo) ;
		else
			m.createPagedReply("Not implemented.").send() ;
	}

	public void processChannelMessage(Message m) {
		processPrivateMessage(m) ;
	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public static String[] getCommands() {
		return new String[]{"TODO", "todo", "reallytodo"};
	}
}
