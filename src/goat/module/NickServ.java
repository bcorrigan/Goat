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
		if(m.sender.equals("NickServ")) 
			if(m.prefix.equals("NickServ!services@services.slashnet.org"))
					new Message("", "PRIVMSG", "NickServ", "identify " + password).send();
	}

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public String[] getCommands() {
		return new String[]{"This"};
	}

	public static void main(String[] args) {
		NickServ ns = new NickServ();
		System.out.println(ns.password);
	}
}
