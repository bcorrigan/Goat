package goat.module ;

import goat.core.Module;
import goat.core.Message;
import goat.util.DICTClient;
import goat.util.Definition;

import java.io.*;
import java.net.*;
import java.util.Random ;
import java.util.Vector;
		


/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 *	@author encontrado
 * 
 * @version 1.0
 */
public class Define extends Module {
	
	private static String host = "edslocomb.com" ;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"define", "randef", "dictionaries", "dictionary" };
   }
	
	public Define() {
	}

	
	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		//parse out args

		System.out.println("processing command: " + m.modCommand) ;
		if (m.modCommand.equals("define")) {
			define(m) ;
		} else if (m.modCommand.equals("randef")) { 
			randef(m) ;
		} else if (m.modCommand.equals("dictionaries")) { 
			dictionaries(m) ;
		} else if (m.modCommand.equals("dictionary")) {
			dictionary(m) ;
		}
	}

	private void define(Message m) {
		// TODO: parse message for args, defaults follow:
		String dictionary = "*" ;
		String word = m.modTrailing.trim() ;
		int num = 1 ;
		String text = "" ;
		if(null == word || word.equals("") ) {
			m.createReply("Er, define what, exactly?").send() ;
			return ;
		}
		String[][] dbList = null ;
		String[][] matchList = null ;
		Vector definitionList = null ;
		DICTClient dc = getDICTClient(m) ;
		if(null == dc) 
			return ;
		try {
			dbList = dc.getDatabases() ;
			definitionList = dc.getDefinitions( new String[] {dictionary}, word) ;
			matchList = dc.getMatches(new String[] {dictionary}, ".", word) ;
		} catch (ConnectException e) {
			m.createReply("Something went wrong while connecting to DICT server at " + host ) ;
			e.printStackTrace() ;
		} finally {
			dc.close() ;
		}
		// Check to see if we were given a valid dictionary
		if(! dictionary.equals("*")) {
			boolean found = false ;
			for (int i=0; i<dbList.length ;i++) {
				if (dictionary.equals(dbList[i][0])) {
					found = true ;
					break ;
				}
			}
			if( ! found ) {
				m.createReply("\"" + dictionary + "\" is not a valid dictionary.").send() ;
				dictionaries(m) ;
				return ;
			}
		}
		// check not-null definition list and match list
		if(null == definitionList || null == matchList) {
			System.out.println("I'm sorry, Dave, something has gone horribly wrong.") ;
			return ;
		}
		// check match list not empty
		if (0 == matchList.length) {
			m.createPagedReply("No definitions found.  Couldn't find any alternate spelling suggestions.  Try a little harder next time.").send() ;
			return ;
		}
		// check definition list not empty
		if (definitionList.isEmpty()) {
			String suggestions = "" ;
			for(int i=0;i<matchList.length;i++) 
				suggestions += " " + matchList[i][1] ;
			suggestions = suggestions.replaceAll("\"", "") ;
			suggestions = suggestions.trim() ;
			String line = "No definitions found" ;
			if (! dictionary.equals("*"))
				line += " in dictionary \"" + dictionary + "\"." ;
			else
				line += "." ;
			m.createPagedReply(line + "  Suggestions: " + suggestions).send() ;
			return ;
		}
		// check num not greater than number of elements in list
		if (num > definitionList.size() ) {
			String line = "I don't have " + num + " definitions for \"" + word ;
			if (! dictionary.equals("*") ) 
				line = line + "\" in dictionary \"" + dictionary + "." ;
			else
				line = line + "." ;
			m.createReply(line).send() ;
		}
		Definition d = (Definition) definitionList.get(num - 1) ;
		text = d.getWord() + " (" + d.getDatabaseShort() + "): " + d.getDefinition() ;
		m.createPagedReply(text).send() ; 
		// TODO add definitions per dictionary line, if more than one def.
	}
	
	private void randef(Message m) {
		m.createReply("Not implmemented, please stand by").send() ; 
	}
	
	private void dictionaries(Message m) {
		DICTClient dc = getDICTClient(m) ;
		String[][] dbList = dc.getDatabases() ;
		dc.close() ;
		String line = "" ;
		for (int i = 0; i < dbList.length ; i++) {
			if (line.equals(""))
				line = dbList[i][0] ;
			else
				line = line + ", " + dbList[i][0] ;
			line = line.trim() ;
		}
		m.createPagedReply(line).send() ;
	}
	
	private void dictionary(Message m) {
		DICTClient dc = getDICTClient(m) ;
		String code = m.modTrailing.trim() ;
		String line = "" ;
		String[][] dbList = dc.getDatabases() ;
		dc.close() ;
		boolean found = false ;
		for (int i = 0; i < dbList.length ; i++ ) {
			if (dbList[i][0].equals(code)) {
				found = true ;
				line = code + ": " + dbList[i][1] ;
				break ;
			}
			if (line.equals(""))
				line = dbList[i][0] ;
			else
				line = line + ", " + dbList[i][0] ;
			line = line.trim() ;
		}
		if (! found) 
			line = "Dictionary \"" + code + "\" not found; available dictionaries: " + line ;
		m.createPagedReply(line).send() ;
	}

	private DICTClient getDICTClient(Message m) {
		DICTClient dc = null ;
		try {
			dc = new DICTClient(host) ;
		} catch (UnknownHostException e) {
			e.printStackTrace() ;
			m.createReply("Couldn't talk to dict server: host \"" + host + "\" unknown").send() ;
			System.exit(1) ;
		} catch (ConnectException e) {
			e.printStackTrace() ;
			m.createReply("Couldn't talk to dict server.").send() ;
		}
		return dc ;
	}
		
	public static void main(String[] arg) {
		Define define = new Define() ;
		DICTClient dc = define.getDICTClient(new Message("","","","")) ;
		System.out.println("Starting main()") ;
		String[][] dbList = dc.getDatabases() ;
		String line = "" ;
		for (int i = 0 ; i < dbList.length ; i++) {
			line = line + " " + dbList[i][0] ;
		}
		line = line.trim() ;
		System.out.println(line) ;
		dc.close() ;
	}
}
