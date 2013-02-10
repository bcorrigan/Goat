package goat.module;
/**
 * Title: Vernie Module
 * Description: Retrieves and parses spammed URLs
 * Copyright:    Copyright (c) 2002
 * Company: Mauvesoft
 * @author Daniel Pope
 * @version 1.0
 */

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.ListIterator;

public class Vernie extends Module {
	private LinkedList linkhistory;

	private boolean posttitles = true;

	private String channel;

	public Vernie() {
		linkhistory = new LinkedList();
		loadHistory();
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		boolean performaction = false;

		String message;

		channel = m.getChanname();

		String towho = m.getChanname();

		message = m.getTrailing();

		if (m.getParams().equals(BotStats.getInstance().getBotname())) {
			towho = m.getSender();
		}

		if (m.isCTCP() && m.getCTCPCommand().equals("ACTION"))
			message = m.getCTCPMessage();

		findLinks(message, towho, m.getSender());

		if (m.isPrivate()) {
			//check the command

			towho = m.getSender();

			if (message.equals("link history") || message.equals("show history"))
				performaction = true;
		} else if (message.length() > BotStats.getInstance().getBotname().length() && message.toLowerCase().substring(0, BotStats.getInstance().getBotname().length()).equals(BotStats.getInstance().getBotname().toLowerCase())) {
			//check the command

			towho = m.getSender();

			int i = message.indexOf(' ');
			message = message.substring(i + 1);

			if (message.equals("link history") || message.equals("show history"))
				performaction = true;
		}

		if (m.isAuthorised()) {
			//check the command
			towho = m.getSender();

			if (message.equals("post titles") || message.equals("show titles")) {
				posttitles = !posttitles;
				Message.createNotice(towho, "title posting " + (posttitles ? "On" : "Off"));
			}
		}

		if (performaction)
			showLinkHistory(towho);
	}

	private void showLinkHistory(String towho) {
		ListIterator it = linkhistory.listIterator();

		int i = 1;

		while (it.hasNext()) {
			String currentitem = (String) it.next();

			String num = Integer.toString(i);

			if (num.length() < 2)
				num = ' ' + num;

			Message.createNotice(towho, num + ". " + currentitem);

			i++;
		}
	}

	/**
	 * find links in a string (haystack) in a Vernie-like way
	 */
	private void findLinks(String haystack, String towho, String owner) {
		int i, k;
		int j = 0;

		i = haystack.indexOf("http://", j);
		if (i > -1)
			i += 7;

		k = haystack.indexOf("www.", j);
		if (k < i && k != -1 || i == -1) i = k;

		while (i > -1) {
			for (j = i; j < haystack.length(); j++) {
				// : /  . ? & % are the only allowable symbols
				char ch = haystack.charAt(j);

				if (!Character.isLetterOrDigit(ch) && ch != ':' && ch != '@' && ch != '/' && ch != '.' && ch != '?' && ch != '&' && ch != '%' && ch != '-' && ch != '~' && ch != '#' && ch != '_' && ch != '=')
					break;
			}

			String link = haystack.substring(i, j);

			doLink("http://" + link, towho, owner);

			i = haystack.indexOf("http://", j);
			if (i > -1)
				i += 7;

			k = haystack.indexOf("www.", j);
			if (k < i && k != -1 || i == -1) i = k;
		}
	}

	/**
	 * Processes a link in a Vernie-like way
	 */
	private void doLink(String link, String towho, String owner) {
		System.out.println("Vernieing link " + link + " to " + towho);

		URLConnection file;

		BufferedReader in;

		try {
			file = (new URL(link)).openConnection();
		} catch (MalformedURLException e) {/* this is our fault, so we'll say nothing */ System.out.println("Tried to Vernie malformed link: " + link);
			return;
		} catch (IOException e) {
			if (posttitles)
				Message.createNotice(owner, link + " appears to be broken: " + e.getMessage());
			return;
		}

		try {
			file.connect();
			in = new BufferedReader(new InputStreamReader(file.getInputStream()));
		} catch (IOException e) {
			if (posttitles)
				Message.createNotice(owner, link + " appears to be broken: " + e.getMessage());
			return;
		}

		String parseabletype = "text/html";

		if (file.getContentType() != null && (file.getContentType().length() < parseabletype.length() || !file.getContentType().substring(0, parseabletype.length()).equals(parseabletype))) {
			if (posttitles)
				Message.createNotice(towho, owner + "'s URL is of type " + file.getContentType() + " (" + file.getContentLength() / 1024 + "KB)");

			if (towho.equals(channel))
				addHistory(owner + " - " + link + " (" + file.getContentLength() / 1024 + "KB)");

			try {
				in.close();
			} catch (IOException e) {
			}

			return;
		}

		if (file.getContentLength() > 500 * 1024) {
			if (posttitles)
				Message.createNotice(towho, owner + "'s URL was too large a file for me to bother parsing: " + file.getContentLength() / 1024 + "KB");

			if (towho.equals(channel))
				addHistory(owner + " - " + link + " (" + file.getContentLength() / 1024 + "KB)");

			try {
				in.close();
			} catch (IOException e) {
			}
			return;
		}

		String line;

		int i, j;

		for (; ;) {
			try {
				line = in.readLine();
			} catch (IOException e) {
				return;
			}

			if (line == null) {
				Message.createNotice(towho, owner + "'s URL: \"No Title\" (" + file.getContentLength() / 1024 + "KB)");
				addHistory(owner + " - " + link + " : \"No Title\" (" + file.getContentLength() / 1024 + "KB)");
				return;
			}

			String title = "<title>";

			if ((i = line.toLowerCase().indexOf(title)) > -1) {
				i += title.length();

				j = line.toLowerCase().indexOf("<", i);

				String thetitle;

				if (j > -1)
					thetitle = line.substring(i, j);
				else
					thetitle = line.substring(i);

				if (thetitle.length() > 100)
					thetitle = thetitle.substring(0, 100) + "...";

				if (posttitles)
					Message.createNotice(towho, owner + "'s URL: \"" + thetitle + "\" (" + file.getContentLength() / 1024 + "KB)");

				if (towho.equals(channel))
					addHistory(owner + " - " + link + " : \"" + thetitle + "\" (" + file.getContentLength() / 1024 + "KB)");

				break;
			}
		}

		try {
			in.close();
		} catch (IOException e) {
		}
	}

	private void addHistory(String history) {
		linkhistory.addLast(history);

		while (linkhistory.size() > 10) linkhistory.removeFirst();

		PrintWriter pw;

		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("vernie.history.txt", true)));

			pw.println(history);

			pw.close();

		} catch (IOException e) {
		}
	}

	private void loadHistory() {
		BufferedReader br;

		String line;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("vernie.history.txt")));

			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					linkhistory.addLast(line);
				}
			}

			br.close();
		} catch (IOException e) { /* fail silently (non-critical) */
		}

		while (linkhistory.size() > 10) linkhistory.removeFirst();
	}
	
	public String[] getCommands() { return new String[0]; }
}