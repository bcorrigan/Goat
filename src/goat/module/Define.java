package goat.module ;

import goat.core.Module;
import goat.core.Message;
import goat.util.DICTClient;

import java.io.*;
import java.net.*;
import java.util.Random ;


/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 *	@author encontrado
 * 
 * @version 1.0
 */
public class Define extends Module {
	
	private static DICTClient dictClient = null ;
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"define", "randef", "dictionaries", "dictionary" };
   }
	
	public Define() {
		if ( null == dictClient ) 
			init() ;
	}

	private void init() {
		try {
			dictClient = new DICTClient("www.edslocomb.com") ;
		} catch (UnknownHostException e) {
			e.printStackTrace() ;
			System.exit(1) ;
		} catch (ConnectException e) {
			e.printStackTrace() ;
			System.exit(1) ;
		}
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
		m.createReply("Not implmemented, please stand by").send() ; 
	}
	
	private void randef(Message m) {
		m.createReply("Not implmemented, please stand by").send() ; 
	}
	
	private void dictionaries(Message m) {
		String[][] dbList = dictClient.getDatabases() ;
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
		String code = m.modTrailing.trim() ;
		String line = "" ;
		String[][] dbList = dictClient.getDatabases() ;
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

	public static void main(String[] arg) {
		new Define() ;
		System.out.println("Starting main()") ;
		String[][] dbList = dictClient.getDatabases() ;
		String line = "" ;
		for (int i = 0 ; i < dbList.length ; i++) {
			line = line + " " + dbList[i][0] ;
		}
		line = line.trim() ;
		System.out.println(line) ;
	}
}
