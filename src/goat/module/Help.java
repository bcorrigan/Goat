package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.io.*;


/**
 * Title: Description: Copyright:    Copyright (c) 2002 Company:
 * 
 * @version 1.0
 */

public class Help extends Module {

	public int messageType() {
		return WANT_ALL_MESSAGES;
	}

	public Help() {
	}

	public void processChannelMessage(Message m) {
	}

	public void processPrivateMessage(Message m) {
		if (m.getWord(0).equals("help")) {
			if (m.getWord(1).equals("")) {
				printFile("index", m.sender, m.prefix.equals(BotStats.owner));
			} else {
				printFile(m.getWord(1) + ".txt", m.sender, m.prefix.equals(BotStats.owner));
			}
		}
	}

	private void printFile(String filename, String towho, boolean isowner) {
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
			sendMessage(new Message("", "NOTICE", towho, "No such help file."));
			return;
		}

		String line;

		for (; ;) {
			try {
				line = in.readLine();
			} catch (IOException e) {
				break;
			}

			if (line == null)
				break;

			StringBuffer buf = new StringBuffer(line);

			for (i = 0; i < line.length(); i++) {
				if (buf.charAt(i) == '#') {
					buf.setCharAt(i, (char) 0x02);
				}
			}

			if (buf.length() > 0) {
				if (buf.charAt(0) != '@')
					sendMessage(new Message("", "NOTICE", towho, buf.toString()));
				else if (isowner)
					sendMessage(new Message("", "NOTICE", towho, buf.toString().substring(1)));
			}
		}

		try {
			in.close();
		} catch (IOException ignored) {
		}
	}
}