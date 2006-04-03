package goat.util;

import java.sql.*;
import java.util.Date ;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;
import org.hsqldb.util.SqlTool;
import org.hsqldb.util.SqlTool.SqlToolException ;

/**
 * Some convenience methods for teh Goat DB Server. <p/>
 * 
 * There are methods here to start up and shut down goat's hsqldb database, grab
 * a connection to it, and do simple queries.</p>
 * 
 * This is implemented as a bag of static methods, meant to be used in combination
 * with the standard jdbc library.  It's not a "wrapper" so much as a few widgets
 * that save you some of the hassle of connecting to the Goat DB.
 * 
 * @author rs
 * 
 * @see <a href="http://www.hsqldb.org/web/hsqlDocsFrame.html">hsqldb docs</a>
 * @see <a href="http://www.hsqldb.org/doc/src/index.html">hsqldb javadocs</a>
 * @see <a
 *      href="http://www.kickjava.com/src/org/hsqldb/Server.java.htm">org.hsqldb.Server
 *      source</a>
 */
public class GoatDB {

	/*
	 * Change this to true for TESTING if you're working on major DB changes and don't want
	 * to screw up the real db.  Note that junit tests written by subclassing goat.GoatTest
	 * will always use a fresh test DB; you can probably just use that for your testing 
	 * needs without changing this.  If you do set this to true, you will want to be
	 * careful about running your test goat and running junit tests at the same time,
	 * as they will share the test database, and one process will most likely
	 * step on the other, resulting in bolloxed unit tests, or a bolloxed test-goat.
	 * 
	 * If this arrangement isn't flexible enough, we'll have to set up a method
	 * that lets you specify your own db name, file location, etc.
	 */
	private static final boolean TESTING = true ;
	
	/*
	 * If TESTING (above) is true, then setting this to true will cause the test db
	 * to be wiped and a new schema set up every time the server is started.  Caveats
	 * above for TESTING apply here, too.
	 */
	private static final boolean RECREATE_TEST_SCHEMA_ON_START = true ;
	
	// some constants
	private static final String TEST_DB_NAME = "test";
	private static final String TEST_DB_PATH = "db/test.db/test";
	private static final String TEST_DB_USER = "sa";
	private static final String TEST_DB_PASS = "";

	private static final String REAL_DB_NAME = "goat";
	private static final String REAL_DB_PATH = "db/goat.db/goat";
	private static final String REAL_DB_USER = "sa";
	private static final String REAL_DB_PASS = "dumpluff3323";
	private static final String REAL_DB_INIT_FILE = "db/sql/setup-goat.db.sql";
	
	private static final String SCHEMA_NAME = "goat" ;
	private static final String SCHEMA_DEFINITION_FILE = "db/sql/schema.sql" ;
	
	private static final String SQLTOOL_RCFILE = "db/sqltool.rc" ;
	/**
	 * DB server address.
	 */
	private static final String SERVER_ADDRESS = "127.0.0.1";
	/**
	 * DB server port.
	 */
	private static final int SERVER_PORT = 2232;
	
	/**
	 * DB server silent. <p/>
	 * 
	 * The hsqldb server is *very* chatty. You probably want this off unless
	 * you're debugging. And maybe even then, too.
	 */
	private static final boolean SERVER_SILENT = true ;
	
	/**
	 * SQL command to shut down server.
	 * 
	 * This should be "SHUTDOWN COMPACT" to maintain tidy db files,
	 * or just "SHUTDOWN" for quick DB shutdown.
	 */
	private static final String SQL_SHUTDOWN_COMMAND = "SHUTDOWN COMPACT" ;
	
	/**
	 * HSQLDB SQL statement to retrieve last value inserted automatically 
	 * into an identity column by the current connection
	 */
	private static final String HSQLDB_IDENTITY_QUERY = "CALL IDENTITY() ;" ;

	/** 
	 * The dummy IRC network name.  Avoid using this if you can.
	 * 
	 * This should match the name inserted into table networks in db/sql/schema.sql
	 */
	public static final String DUMMY_NETWORK_NAME = "" ;	

	/**
	 * Maximim time to wait for the db to shut down, in seconds.
	 */
	private static final long MAX_SHUTDOWN_WAIT = 180 ;
	
	private static String dbName = TEST_DB_NAME;
	private static String dbPath = TEST_DB_PATH;
	private static String dbUser = TEST_DB_USER;
	private static String dbPass = TEST_DB_PASS;
	private static Server hsqldbServer = new Server();

	/**
	 * Start up a db server. <p/> 
	 * 
	 * Note that hsqldb.Server class takes care of
	 * threading, so we don't have to worry about it, here. Oh, and thanks for
	 * not mentioning threading at all in the docs for the Server class, hsqldb
	 * devs. <p/> 
	 * 
	 * This method checks to see if the server is running before
	 * attempting to start it; if it does find it running, it does nothing. <p/>
	 * 
	 * If this class starts to get complicated, we might want to make it a 
	 * Singleton, but the everything-static approach is fine for now.
	 * 
	 * @param isATest
	 *            use the real db if false, otherwise use the test db.
	 */
	public static void startServer(boolean isATest) {
		if (! GoatDB.serverRunning()) {
			System.out.println("Initiating Goat DB Server startup.") ;
			if (! isATest)
				setDbReal();
			hsqldbServer.setSilent(SERVER_SILENT);
			hsqldbServer.setNoSystemExit(true);
			hsqldbServer.setRestartOnShutdown(false);
			hsqldbServer.setDatabaseName(0, dbName);
			hsqldbServer.setDatabasePath(0, dbPath);
			hsqldbServer.setAddress(SERVER_ADDRESS);
			hsqldbServer.setPort(SERVER_PORT);
			hsqldbServer.start();
			if (isATest && RECREATE_TEST_SCHEMA_ON_START) 
				recreateTestSchema() ;
		}
	}
	
	/**
	 * Start the test or the real db, depending on the value of the static class
	 * variable "TESTING".
	 * 
	 * @see startServer(boolean).
	 */
	public static void startServer() {
		startServer(TESTING);
	}
	
	/**
	 * Shut down the db server. <p/>
	 * 
	 * This performs an asynchronous shutdown; you almost certainly want
	 * safeShutdown instead.
	 * 
	 * @see safeShutdown()
	 */
	public static void shutdown() {
		if (GoatDB.serverRunning()) {
			System.out.println("Initiating Goat DB Server shutdown");
			try {
				GoatDB.executeUpdate(SQL_SHUTDOWN_COMMAND) ;
			} catch (GoatDBConnectionException e) {
				System.err.println("ERROR: Couldn't connect to goat db, reverting to direct shutdown");
				hsqldbServer.shutdown();
				return ;
			} catch (SQLException e) {
				if(GoatDB.serverRunning()) {
					System.err.println("ERROR: Problem with SQL command\"" + SQL_SHUTDOWN_COMMAND + "\", reverting to direct shutdown") ;
					hsqldbServer.shutdown() ;
				} else {
					System.err.println("ERROR: Problem with SQL command \"" + SQL_SHUTDOWN_COMMAND + "\", but server appears to have shut down successfully") ;
				}
				return ;
			}
		} else {
			System.out.println("DB Shutdown: Goat db not running");
		}
	}

	/**
	 * sensible wrapper around hsqlsb.Server's weird checkRunning() method.
	 * 
	 * @return true if the goat db server is running, false if it ain't.
	 */
	public static boolean serverRunning() {
		boolean running;
		try {
			hsqldbServer.checkRunning(true);
			running = true;
		} catch (RuntimeException e) {
			running = false;
		}
		return running;
	}
	
	/**
	 * Get a connection to the goat db. <p/>
	 * 
	 * @return a new jdbc connection to the goat db.
	 * @throws GoatDBConnectionException
	 *             if a connection could not be obtained, for any reason.
	 */
	public static Connection getConnection() 
		throws GoatDBConnectionException {
		Connection c;
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (Exception e) {
			System.err.println("ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		try {
			c = DriverManager.getConnection(connectionUrl(), dbUser, dbPass);
		} catch (SQLException e) {
			System.err.println("Couldn't get connection to DB server; attempting to start in-process server...") ;
			try {
				startServer() ;
				c = DriverManager.getConnection(connectionUrl(), dbUser, dbPass);
			} catch (SQLException e2) {
				System.err.println("ERROR: couldn't connect to the goat db (or couldn't start in-process server)");
				e2.printStackTrace() ;
				GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
				gdbe.initCause(e2) ;
				throw gdbe ;
			}
		} 
		try {
			if (hasSchema(SCHEMA_NAME, c))
				c.createStatement().executeUpdate("SET SCHEMA " + SCHEMA_NAME + " ;");
		} catch (SQLException e) {
			System.err.println("WARNING: Schema " + SCHEMA_NAME + " not found while creating goat connection; we'll assume you know what you're doing.") ;
			//e.printStackTrace() ;
		}
		return c;
	}
	
	public static String connectionUrl() {
		return "jdbc:hsqldb:hsql://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/" + dbName ;
	}
	
	/* This is pretty useless; might as well ditch it.
	public static Statement getStatement () 
		throws GoatDBConnectionException {
		Connection c = GoatDB.getConnection();
		Statement st ;
		try {
			st = c.createStatement();
		} catch (SQLException e) {
			System.err.println("ERROR:  Couldn't create java.sql.Statement for some reason") ;
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		return st ;
	}
	*/
	
	/**
	 * "Convenience" method to do a query on teh goat DB.<p/>
	 * 
	 * This throws exceptions like a monkey throws poop, but it's still a bit
	 * more convenient than doing all the connection blather yourself.<p/>
	 * 
	 * This works just like java.sql.Statement.executeQuery(String), except it
	 * takes care the overhead of getting a connection and a statement and so
	 * on.<p/>
	 * 
	 * Use this for SELECT statments; for UPDATE, INSERT, DELETE, or other SQL
	 * statements that return a count, use GoatDB.executeUpdate(String) <p/>
	 * 
	 * Please be polite, and close() the Connection associated with your
	 * ResultSet when you're done with the results (unless you are some sort of
	 * java-psychic and know it will be garbage-collected immediately).<p/>
	 *  e.g.:<p/> 
	 * <code>
	 * ResultSet rs = GoatDB.executeQuery("SELECT * FROM goatse ;") ;
	 * 
	 * // do stuff with rs from goatse
	 * 
	 * rs.getStatement().getConnection().close() ;
	 * </code>
	 * </p>
	 * 	<b>Do Not</b> use this method for bulk inserts-- it
	 *  creates a new Connection every time it's called, and
	 *  the Goat DB Server will crap out after about 3800
	 *  connections in rapid succession.<p/>
	 * 
	 * If this method or GoatDB.executeUpdate(String) doesn't do what you need,
	 * start with GoatDB.getConnection() or GoatDB.getStatement() and go from
	 * there.
	 * 
	 * @param statement
	 *            your SQL query (SELECT statement)
	 * @return the ResultSet for your query. Empty (not null) if the query finds
	 *         nothing in goat's DB. Remember to close() this when you're done!
	 * @throws GoatDBConnectionException
	 *             if there was a problem connecting to the goat DB
	 * @throws SQLException
	 *             if there was a problem executing the query
	 * @see java.sql.Statement.executeQuery(java.lang.String)
	 * @see goat.util.GoatDB.executeUpdate(java.lang.String)
	 * @see goat.util.GoatDB.getConnection()
	 * @see goat.util.GoatDB.getStatement()
	 */
	public static ResultSet executeQuery(String statement)
			throws GoatDBConnectionException, SQLException {
		Statement st = GoatDB.getConnection().createStatement() ;
		return st.executeQuery(statement) ;
	}
	
	/**
	 * "Convenience" method to do a query on teh goat DB.<p/>
	 * 
	 * see GoatDB.executeQuery(String) for details.<p/>
	 * 
	 * <b>Do Not</b> use this method for bulk inserts-- it creates a new
	 * Connection every time it's called, and the Goat DB Server will crap out
	 * after about 3800 connections in rapid succession.
	 * 
	 * This method, unlike GoatDB.executeQuery(String), <i>does</i> close() its
	 * Connection for you.
	 * 
	 * @param statement
	 *            your SQL query
	 * @return either the row count for INSERT, UPDATE, or DELETE, or 0 for an
	 *         SQL statement that does nothing.
	 * @throws GoatDBConnectionException
	 *             if there was a problem connecting to the goat DB
	 * @throws SQLException
	 *             if there was a problem executing the query
	 * @see java.sql.Statement.executeUpdate(java.lang.String)
	 * @see goat.util.GoatDB.executeQuery(java.lang.String)
	 */
	public static int executeUpdate(String statement)
			throws GoatDBConnectionException, SQLException {
		Statement st = GoatDB.getConnection().createStatement();
		int ret = st.executeUpdate(statement);
		st.getConnection().close();
		return ret;
	}
	
	/**
	 * Shut the DB down and wait to make sure the shutdown completed.
	 * 
	 * @return true if shutdown was successful, false if MAX_SHUTDOWN_WAIT time
	 *         was exceeded.
	 * 
	 * @see GoatDB.MAX_SHUTDOWN_WAIT
	 */
	public static boolean safeShutdown() {
		long started = (new Date()).getTime();
		GoatDB.shutdown();
		boolean ret = GoatDB.waitForShutdownComplete();
		if (ret)
			System.out.println("Goat DB Server shut down safely in "
					+ ((new Date()).getTime() - started) + " ms.");
		else
			System.err.println("Goat DB Server shutdown timed out afer "
					+ MAX_SHUTDOWN_WAIT + " seconds.");
		return ret;
	}
	
	public static String getStateDescriptor() {
		return hsqldbServer.getStateDescriptor() ;
	}
	
	private static boolean waitForShutdownComplete() {
		long started = (new Date()).getTime();
		while (hsqldbServer.getState() != ServerConstants.SERVER_STATE_SHUTDOWN) {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
			if ((new Date()).getTime() - started > MAX_SHUTDOWN_WAIT * 1000L) {
				System.err
						.println("ERROR: Exceeded max DB shutdown wait time of "
								+ MAX_SHUTDOWN_WAIT
								+ " seconds.  Not waiting around any more.");
				return false;
			}
		}
		return true;
	}
	
	public static boolean deleteTestSchema() {
		System.out.println("DESTROYING goat schema in test DB") ;
		boolean success = false ;
		// stop server restart with test db if it's running and not serving the test db
		if(serverRunning() && (! dbName.equals("test")))
			GoatDB.safeShutdown() ;
		if(! serverRunning()) {
			GoatDB.startServer(true) ;
		}
			
		// Drop the goat schema
		try {
			if (GoatDB.hasSchema(SCHEMA_NAME))
				GoatDB.executeUpdate("DROP SCHEMA " + SCHEMA_NAME + " CASCADE") ;
			success = true ;
		} catch (SQLException e) {
			e.printStackTrace() ;
			success = false ;
		}
		System.out.println("DESTROYED goat schema in test DB") ;
		return success ;
	}
	
	/**
	 * This pains me.
	 * 
	 * @param s name of the schema you want to check for.
	 * @param c connection to the db you want to check
	 * @return
	 * @throws SQLException
	 */
	private static boolean hasSchema(String s, Connection c) {
		boolean found = false ;
		try {
			ResultSet rs = c.getMetaData().getSchemas() ;
			while (rs.next()) {
				if (rs.getString(1).equalsIgnoreCase(s)) {
					found = true ;
					break ;
				}
			}
		} catch (SQLException e) {
			System.err.println("WARNING:  Couldn't retrieve schema list from goat db") ;
		}
		return found ;
	}
	
	private static boolean hasSchema(String s) throws GoatDBConnectionException {
		return hasSchema(s, GoatDB.getConnection()) ;
	}
	
	public static boolean recreateTestSchema() {
		if(! serverRunning())
			GoatDB.startServer(true) ;
		boolean success = GoatDB.deleteTestSchema() ;
		if (success)
			success = loadSchema(true) ;
		return success ;
	}
	
	/**
	 * Method to load the goat schema into a database.
	 * 
	 * goat.util.GoatDB is set up to not allow you to initialize the deployment
	 * db. If you want to do that, you can make a method that calls this one
	 * with isTest == false, or you can change this to a public method and call
	 * it in the aforesaid manner, or you can use hsqldb's SqlTool to manually
	 * load db/sql/setup-goat.db.sql and then db/sql/schema.sql (recommended).
	 * 
	 * @param isTest
	 * @return
	 */
	private static boolean loadSchema(boolean isTest) {
		boolean success = false ;
		if (serverRunning()) {
			if (isTest && (dbName.equals("test")))
				success = loadFile(SCHEMA_DEFINITION_FILE) ;
			else if (dbName.equals("goat")) {  
				System.err.println("someone called loadSchema() on the deployment db with the server already running;  naughty, naughty.  DB not altered.") ;
			}
		} else {
			GoatDB.startServer(isTest) ;
			if(dbName.equals("goat")) {
				success = loadFile(REAL_DB_INIT_FILE) ;
				if (success) 
					success = loadFile(SCHEMA_DEFINITION_FILE) ;
			} else {
				success = loadFile(SCHEMA_DEFINITION_FILE) ;
			}
		}
		return success ;
	}
	
	/**
	 * Load a file of valid sql into the running db.
	 * 
	 * Does nothing if the db server has not been started.
	 * 
	 * @param filename
	 * @return true if file is successfully loaded
	 */
	public static boolean loadFile(String filename) {
		boolean success = false ;
		if (! serverRunning() ) {
			System.err.println("Someone tried to load a file with no db server running") ;
			return success ;
		}
		String args [] = {"--rcfile", SQLTOOL_RCFILE, dbName, filename} ;
		try {
			System.setProperty("sqltool.noexit", "true") ;
			if(null == System.getProperty("sqltool.noexit"))
				System.err.println("system property not set: sqltool.noexit");
			SqlTool.main(args) ;
			success = true ;
		} catch (SqlToolException e) {
			e.printStackTrace() ;
			success = false ;
		}
		return success ;
	}

	private static void setDbReal() {
		dbName = REAL_DB_NAME;
		dbPath = REAL_DB_PATH;
		dbUser = REAL_DB_USER;
		dbPass = REAL_DB_PASS;
	}
	
	public static int getIdentity(Connection c) {
		int ret = -1 ;
		try {
			ResultSet rs = c.createStatement().executeQuery(HSQLDB_IDENTITY_QUERY) ;
			if (rs.next()) 
				ret = rs.getInt(1) ;
		} catch (SQLException e) {
			System.err.println("Problem retrieving identity via \"" + HSQLDB_IDENTITY_QUERY + "\"") ; ;
		}
		return ret ;
	}

}

class GoatDBConnectionException extends SQLException {
	// eclipse complains without this.
	private static final long serialVersionUID = 1L;
}
class GoatDBNoResultSetException extends Exception {
	// eclipse complains without this.
	private static final long serialVersionUID = 1L;
}