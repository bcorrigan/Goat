package goat.module;

import goat.eliza.ElizaMain;
import goat.core.Module;
import goat.core.Message;

/**
 * @author bc
 */
public class Psychiatrist extends Module {

	private ElizaMain eliza;
	static String scriptPath = "resources/elizaScript";

	public Psychiatrist() {
		eliza = new ElizaMain();
		eliza.readScript(true, scriptPath);
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public String[] getCommands() {
		return new String[]{"psychiatrist", "psy"};
	}

	public void processChannelMessage(Message m) {
		m.createReply(eliza.processInput(m.modTrailing)).send();
	}
}
