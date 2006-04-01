package goat.util;

import java.sql.* ;

import goat.GoatTest ;
import goat.util.GoatDB ;
import goat.util.Dict ;


import junit.framework.Test;
import junit.framework.TestSuite;

public class GoatDBTest extends GoatTest {
	
	public static Test suite() {
		return new TestSuite(GoatDBTest.class);
	}

	/**
	 * Old tests -- test db startup/shutdown is now handled by setUp() and tearDown() in goat.GoatTest 
	 *  
	public void testStartStopTestDB() {
		GoatDB.startServer(true) ;
		assertTrue(GoatDB.serverRunning()) ;
		GoatDB.safeShutdown() ;
		assertFalse(GoatDB.serverRunning()) ;
	}

	public void testStartStopRealDB() {
		GoatDB.startServer(false) ;
		assertTrue(GoatDB.serverRunning()) ;
		GoatDB.safeShutdown() ;
		assertFalse(GoatDB.serverRunning()) ;
	}
	*/
	
	public void testDummyData() {
		Dict dict = new Dict();
		boolean failed = false ;
		String table = "crumpets" ;
		//set inserts to something like 1000000 to give the db a proper working-over
		int inserts = 100000 ;
		int rows = 0;
		try {
			
			Connection c = GoatDB.getConnection() ;
			GoatDB.executeUpdate("DROP TABLE " + table + " IF EXISTS;");
			GoatDB.executeUpdate("CREATE TABLE " + table +  " ("
					+ "id INTEGER IDENTITY, " + "bandname VARCHAR NOT NULL );");
			c.close() ;
			
			// GoatDB.executeUpdate fails reliably after 3800 or so inserts,
			//   so we use a single Connection or Statement for bulk insert
			Statement st = GoatDB.getConnection().createStatement() ;
			for (int i = 0; i < inserts; i++) {
				st.executeUpdate("INSERT INTO " + table + " (bandname) VALUES ('"
						+ dict.getRandomWord() + " " + dict.getRandomWord()
						+ "');");
			}
			st.getConnection().close() ;
			
			ResultSet rs = GoatDB.executeQuery("SELECT count(*) FROM " + table + " ;") ;
			rs.next() ;
			rows = rs.getInt(1) ;
			rs.getStatement().getConnection().close() ;  //be polite, close yer connections
			System.out.println(rows + " rows in table \"" + table + "\"");
			
			//  uncomment if you don't want to leave the table in the test db.
			// GoatDB.executeUpdate("DROP TABLE " + table + " IF EXISTS;");
		} catch (GoatDBConnectionException e) {
			e.printStackTrace();
			failed = true;
		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
		}
		assertFalse(failed) ;
		assertTrue(inserts == rows) ;
	}
}
