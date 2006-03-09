package goat.util;

import java.sql.*;
import java.util.Date ;
import org.hsqldb.Server;
import org.hsqldb.ServerConstants;

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

	/**
	 * Change this to true for testing if you're mucking about with the DB.
	 */
	private static final boolean testing = true ;
	
	// some constants
	private static final String testDbName = "goat";
	private static final String testDbPath = "db/test.db/test";
	private static final String testDbUser = "sa";
	private static final String testDbPass = "";

	private static final String realDbName = "goat";
	private static final String realDbPath = "db/goat.db/goat";
	private static final String realDbUser = "sa";
	private static final String realDbPass = "dumpluff3323";

	/**
	 * DB server address.
	 */
	private static final String serverAddress = "127.0.0.1";
	/**
	 * DB server port.
	 */
	private static final int serverPort = 2232;
	
	/**
	 * DB server silent. <p/>
	 * 
	 * The hsqldb server is *very* chatty. You probably want this off unless
	 * you're debugging. And maybe even then, too.
	 */
	private static final boolean serverSilent = true ;
	
	/**
	 * SQL command to shut down server.
	 * 
	 * This should be "SHUTDOWN COMPACT" to maintain tidy db files,
	 * or just "SHUTDOWN" for quick DB shutdown.
	 */
	private static final String sqlShutdownCommand = "SHUTDOWN COMPACT" ;
	
	/**
	 * Maximim time to wait for the db to shut down, in seconds.
	 */
	private static final long maxShutdownWait = 180 ;
	
	private static String dbName = testDbName;
	private static String dbPath = testDbPath;
	private static String dbUser = testDbUser;
	private static String dbPass = testDbPass;
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
			hsqldbServer.setSilent(serverSilent);
			hsqldbServer.setNoSystemExit(true);
			hsqldbServer.setRestartOnShutdown(false);
			hsqldbServer.setDatabaseName(0, dbName);
			hsqldbServer.setDatabasePath(0, dbPath);
			hsqldbServer.setAddress(serverAddress);
			hsqldbServer.setPort(serverPort);
			hsqldbServer.start();
		}
	}
	
	/**
	 * Start the test or the real db, depending on the value of the static class
	 * variable "testing".
	 * 
	 * @see startServer(boolean).
	 */
	public static void startServer() {
		startServer(testing);
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
				GoatDB.executeUpdate(sqlShutdownCommand) ;
			} catch (GoatDBConnectionException e) {
				System.err.println("ERROR: Couldn't connect to goat db, reverting to direct shutdown");
				hsqldbServer.shutdown();
				return ;
			} catch (SQLException e) {
				if(GoatDB.serverRunning()) {
					System.err.println("ERROR: Problem with SQL command\"" + sqlShutdownCommand + "\", reverting to direct shutdown") ;
					hsqldbServer.shutdown() ;
				} else {
					System.err.println("ERROR: Problem with SQL command \"" + sqlShutdownCommand + "\", but server appears to have shut down successfully") ;
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
			c = DriverManager.getConnection("jdbc:hsqldb:hsql://"
					+ serverAddress + ":" + serverPort + "/" + dbName, dbUser, dbPass);
		} catch (SQLException e) {
			System.err.println("ERROR: couldn't connect to the goat db");
			e.printStackTrace() ;
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		return c;
	}
	
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
		Statement st = GoatDB.getStatement() ;
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
		Statement st = GoatDB.getStatement();
		int ret = st.executeUpdate(statement);
		st.getConnection().close();
		return ret;
	}
	
	/**
	 * Shut the DB down and wait to make sure the shutdown completed.
	 * 
	 * @return true if shutdown was successful, false if maxShutdownWait time
	 *         was exceeded.
	 * 
	 * @see GoatDB.maxShutdownWait
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
					+ maxShutdownWait + " seconds.");
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
			if ((new Date()).getTime() - started > maxShutdownWait * 1000L) {
				System.err
						.println("ERROR: Exceeded max DB shutdown wait time of "
								+ maxShutdownWait
								+ " seconds.  Not waiting around any more.");
				return false;
			}
		}
		return true;
	}

	private static void setDbReal() {
		dbName = realDbName;
		dbPath = realDbPath;
		dbUser = realDbUser;
		dbPass = realDbPass;
	}
}

class GoatDBConnectionException extends Exception {
	// eclipse complains without this.
	private static final long serialVersionUID = 1L;
}
class GoatDBNoResultSetException extends Exception {
	// eclipse complains without this.
	private static final long serialVersionUID = 1L;
}