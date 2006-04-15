package goat.db;

import java.sql.* ;
import java.util.Date;

import goat.GoatTest;
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
		int inserts = 10000 ;
		int rows = 0;
		try {
			
			Connection c = db.getConnection() ;
			if(db.hasTable(table, c))
				db.executeUpdate("DROP TABLE " + table);
			
			db.executeUpdate("CREATE TABLE " + table +  " ("
					+ "id SERIAL, " + "bandname VARCHAR NOT NULL );");
			
			// GoatDB.executeUpdate fails reliably after 3800 or so inserts,
			//   so we use a single Connection or Statement for bulk insert
			PreparedStatement ps = c.prepareStatement("INSERT INTO " + table + " (bandname) VALUES (?);") ;
			Date startTime = new Date() ;
			System.out.println("Starting " + inserts + " test inserts at " + startTime.toString()) ;
			for (int i = 0; i < inserts; i++) {
				ps.setString(1, dict.getRandomWord() + " " + dict.getRandomWord()) ;
				ps.execute() ;
				if (0 == (i % 250))
					System.out.print(i + "...") ;
				if (0 == (i % 2500))
					System.out.println();
			}
			Date finishTime = new Date() ;
			long elapsed = finishTime.getTime() - startTime.getTime() ;
			long min = elapsed / (1000*60) ;
			long sec = (elapsed/1000) - (min * 60) ;
			System.out.println() ;
			System.out.println(inserts + " inserts finished in " + min + "min, " + sec + "s") ;
			ps.getConnection().close() ;
			
			ResultSet rs = db.executeQuery("SELECT count(*) FROM " + table + " ;") ;
			rs.next() ;
			rows = rs.getInt(1) ;
			rs.getStatement().getConnection().close() ;  //be polite, close yer connections
			System.out.println(rows + " rows in table \"" + table + "\"");
			
			//  uncomment if you don't want to leave the table in the test db.
			/*
			if(db.hasTable(table, c))
				db.executeUpdate("DROP TABLE " + table);
			*/
		} catch (GoatDB.GoatDBConnectionException e) {
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
