package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.io.*;
//import java.util.ArrayList ;
//import java.util.Iterator ;
import java.util.Random ;
import net.zuckerfrei.jcfd.* ;

/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 * @title Define
 * 
 *	@author encontrado
 * 
 * @version 1.0
 */

public class Define extends Module {

	private static Dict dict ;
	
	public int messageType() {
		return WANT_ALL_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"define", "randef" };
   }
	
	public Define() {
		try {
			dict = DictFactory.getInstance().getDictClient() ;
		} catch (DictException e) {
			System.out.println("Dict Exeption:") ;
			System.exit(1) ;
		}
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		//parse out args
		if (m.modCommand.equals("define")) {
			//blither blither
			m.createReply("blather").send() ;
		} else if (m.getWord(0).equals("randef")) {
		}
	}

	public static void main(String[] arg) {
		new Define() ;
		DatabaseList dbList = dict.listDatabases() ;
		Database db ;
		while (dbList.hasNext()) {
			db = dbList.next() ;
			System.out.println(db.getCode() + " : " + db.getName() + " : " + db.toString()) ;
		}
	}
}
