package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.*;

/**
 * @author bc
 * A nickserv module that sends password to server when challenged
 */
public class NickServ extends Module {

	private String password;
	private long lastAuth;

	public NickServ() {
		File NSPassFile = new File("resources/NickServPassword");
		try {
			BufferedReader in = new BufferedReader(new FileReader(NSPassFile));
			password = in.readLine();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		inAllChannels = true;
        new Message("", "PRIVMSG", "NickServ", "identify " + password).send();
	}
	public void processPrivateMessage(Message m) {
        processChannelMessage(m);
	}

    public void processOtherMessage(Message m) {
        processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {  //TODO really need notice functionality in this thing
		//if our password is wrong we don't want to spam NickServ
		long now = System.currentTimeMillis();
		if( (now - lastAuth) > 30000)
			if(m.getSender().equals("NickServ")) 
				if(m.getPrefix().equals("NickServ!services@services.slashnet.org")) {
					lastAuth = System.currentTimeMillis();
					new Message("", "PRIVMSG", "NickServ", "identify " + password).send();				
				}
	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	/* wtf.  --commented out by rs
	public String[] getCommands() {
		return new String[]{"This"};
	}
	*/

	public static void main(String[] args) {
		NickServ ns = new NickServ();
		System.out.println(ns.password);
	}
	
	public String[] getCommands() { return new String[0]; }
}
