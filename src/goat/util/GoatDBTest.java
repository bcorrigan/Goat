package goat.util;

import java.sql.* ;

import goat.util.GoatDB ;
import goat.util.Dict ;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GoatDBTest extends TestCase {
	
	public static Test suite() {
		return new TestSuite(GoatDBTest.class);
	}

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
	
	public void testDummyData() {
		GoatDB.startServer(true);  // start test server
		Dict dict = new Dict();
		boolean failed = false ;
		String table = "crumpets" ;
		//set inserts to something like a million to give the db a proper working-over
		int inserts = 1000000 ;
		int rows = 0;
		try {			
			Connection c = GoatDB.getConnection() ;
			GoatDB.executeUpdate("DROP TABLE " + table + " IF EXISTS;");
			GoatDB.executeUpdate("CREATE TABLE " + table +  " ("
					+ "id INTEGER IDENTITY, " + "bandname VARCHAR NOT NULL );");
			c.close() ;
			
			// GoatDB.executeUpdate fails reliably after 3800 or so inserts,
			//   so we use a single Connection or Statement for bulk insert
			Statement st = GoatDB.getStatement() ;
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
			//GoatDB.executeUpdate("DROP TABLE " + table + " ;");
		} catch (GoatDBConnectionException e) {
			e.printStackTrace();
			failed = true;
		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
		}
		GoatDB.safeShutdown();
		assertFalse(failed) ;
		assertTrue(inserts == rows) ;
	}

}
