package goat.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import goat.GoatTest;
import goat.core.Message;
import goat.util.IRCLogger;
import java.sql.SQLException;

public class IRCLoggerTest extends GoatTest {
	
	public static Test suite() {
		return new TestSuite(IRCLoggerTest.class);
	}
	
	public void testInsertMessage() {
		IRCLogger logger = null;
		boolean ok = true ;
		int count = 0 ;
		logger = new IRCLogger() ;
		assertTrue(ok) ;
		ok = true ;
		assertTrue(null != logger) ;
		Message m = Message.createPrivmsg("#jism", "Shut up, joey.") ;
		try {
			logger.logMessage(m, GoatDB.DUMMY_NETWORK_NAME) ;
			count = logger.numTotalMessages() ;
			assertTrue(1 == logger.numTotalMessages()) ;
		} catch (SQLException e) {
			System.err.println("Problem logging message") ;
			e.printStackTrace() ;
			ok = false ;
		}
		assertTrue(1 == count) ;
		System.out.println("There are " + count + " messages in the DB.  Dumping:") ;
		try {
			logger.printResultSet(logger.getAllMessages()) ;
		} catch (SQLException e) {
			System.err.println("Problem dumping messages table") ;
			e.printStackTrace() ;
			ok = false ;
		}
		assertTrue(ok) ;
	}

}
