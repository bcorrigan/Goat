package goat.module ;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.io.*;
//import java.util.ArrayList ;
//import java.util.Iterator ;
import java.util.Random ;
import net.zuckerfrei.jcfd.DictFactory ;
import net.zuckerfrei.jcfd.Dict ;
import net.zuckerfrei.jcfd.DictException ;
import net.zuckerfrei.jcfd.DatabaseList ;
import net.zuckerfrei.jcfd.Database ;

/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 * @title Definer
 * 
 *	@author encontrado
 * 
 * @version 1.0
 */

public class Define extends Module {

	private static Dict dict ;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"define", "randef", "dictionaries", "dictionary" };
   }
	
	public void Define() {
		init() ;
	}
	
	private void init() {
		try {
			DictFactory df = DictFactory.getInstance() ;
			dict = df.getDictClient() ;
		} catch (DictException e) {
			System.out.println("Dict Exception:") ;
			e.printStackTrace() ;
			System.exit(1) ;
		}
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		//parse out args
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
		DatabaseList dbList = dict.listDatabases() ;
		Database db ;
		String line = "" ;
		while (dbList.hasNext()) {
			db = dbList.next() ;
			line = line + ", " + db.getCode() ;
		}
		line = line.trim() ;
		m.createPagedReply(line).send() ;
	}
	
	private void dictionary(Message m) {
		m.createReply("Not implmemented, please stand by").send() ; 
	}

	public static void main(String[] arg) {
		System.out.println("Starting main()") ;
		
		Define d = new Define() ;
		d.Define() ; // Seems the constructor isn't actually called in a static context, or some such ballocks, so we need this.
		System.out.println("initialized.") ;
		if (null == dict) {
			System.out.println("null dict, dying") ;
			System.exit(1) ;
		}

		DatabaseList dbList ;
		dbList = dict.listDatabases() ;
		Database db ;
		String line = "" ;
		while (dbList.hasNext()) {
			db = dbList.next() ;
			line = line + " " + db.getCode() ;
		}
		line = line.trim() ;
		System.out.println(line) ;
	}
}
