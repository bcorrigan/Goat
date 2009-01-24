package goat.module;

import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import java.io.*;


//TODO - rethink this, we're not attending to it.  
//  Perhaps we should try something along the lines of an abstract String help() in core.Module, so
//  it's right there nagging you when you build a module.


/**
 * Title: Description: Copyright:    Copyright (c) 2002 Company:
 * 
 * @version 1.0
 */
public class Help extends Module {

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	public static String[] getCommands() {
		return new String[]{"help"} ;
	}

	public Help() {
	}

	public void processChannelMessage(Message m) {
		// m.setReplyTo(m.getSender());
		processPrivateMessage(m) ;
	}

	public void processPrivateMessage(Message m) {
		if (m.getModCommand().equals("help")) {
			if (m.getModTrailing().trim().equals("")) {
				printFile("index", m);
			} else {
				printFile(m.getModTrailing().trim() + ".txt", m);
			}
		}
	}

	private void printFile(String filename, Message m) {
		BufferedReader in;
		int i;
		String fname;
		if ((i = filename.indexOf(File.separatorChar)) > -1) {
			fname = filename.substring(i);
		} else
			fname = filename;
		try {
			in = new BufferedReader(new FileReader("docs" + File.separatorChar + fname));
		} catch (FileNotFoundException e) {
			m.createReply("Couldn't find help file: " + filename).send();
			return;
		}
		String line;
		String reply = "";
		for (; ;) {
			try {
				line = in.readLine();
			} catch (IOException e) {
				break;
			}
			if (line == null)
				break;
			reply += line.trim() + " " ;
		}
		reply = reply.replaceAll("#", Constants.BOLD) ;
		m.createPagedReply(reply.trim()).send() ;
		try {
			in.close();
		} catch (IOException ignored) {
		}
	}
}
