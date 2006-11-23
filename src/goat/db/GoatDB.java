package goat.db;

import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.ArrayList;
//import java.util.Arrays;
import java.io.*;
import goat.Goat;

/**
 * Class to glue goat to goat's db.
 * 
 * <p>
 * This can be subclassed or used directly.
 * </p>
 * 
 * <p>
 * The main things in here are the constructors and the getConnection() method.
 * </p>
 * 
 * <p>
 * There is one constructor that takes an int, being a flag representing a
 * particular type of goat DB. current possible values are DB_TYPE_DEPLOYMENT,
 * representing the db of the real, living goat, DB_TYPE_SANDBOX, representing a
 * sandbox db where your goat in development can stagger around doing its scary
 * Frankenstein Monster dance, and DB_TYPE_JUNIT, representing a db that can be
 * wiped clean and repopulated for any and all junit tests requiring db
 * connectiveness, possibly with set db contents.
 * </p>
 * 
 * <p>
 * There is another constructor, without arguments, that decides what sort of db
 * it is going to represent based on the property db.type in your
 * resources/goat.properties file. You can rummage around in there and tinker
 * with other settings that spell out the exact meanings of the flags discussed
 * above.
 * </p>
 * 
 * <p>
 * Once you have your GoatDB instance, you'll most likely want to get a jdbc
 * Connection object using the GoatDB getConnection() method, and then take your
 * Connection and do various jdbc things with it.
 * </p>
 * 
 * <p>
 * Alternately, for one-off db operations (ie, things where you can afford to
 * create a new db connection for each operation), there are a couple of
 * convenience methods here, executeQuery() and executeUpdate(). You probably
 * won't use them much.
 * </p>
 * 
 * <p>
 * There are other miscellaneous methods lying about, including one to read a
 * file full of sql statements into your db, which might be handy for loading up
 * test data. there's a rather poorly-implemented and probably not entirely
 * reliable<sup>*</sup> method to check and see if the db is still running, if
 * you have any need for that.
 * </p>
 * 
 * <p>
 * There are also methods in here to erase the contents of a goat schema and set
 * up a fresh schema. Methods to create schemas and users (ie, stuff requiring a
 * DBA user) will be put elsewhere.
 * </p>
 * 
 * <p>
 * <sup>*</sup> <i>It won't report a dead db as living, though it will do the
 * reverse, if conditions are such that db connections are unavailable for
 * reasons other than server deadness.</i>
 * </p>
 * 
 * @author rs
 * 
 */
public class GoatDB {
	
	public static final int DB_TYPE_ERROR = -2;
	public static final int DB_TYPE_NONE = -1; //please pretend this doesn't exist for now, thx.
	public static final int DB_TYPE_DEPLOYMENT = 0;
	public static final int DB_TYPE_JUNIT      = 1;
	public static final int DB_TYPE_SANDBOX    = 2;

	private static int defaultDbType = DB_TYPE_ERROR ;
	private static boolean ignoreConfigFile = false ;
	
	// the ArrayList here is overkill, as you're quite unlikely to have more than one kind
	// of goatDB object going at once.  I should have myself checked for Aspergers.  --rs
	private static ArrayList<Connection> janitorConnections = new ArrayList<Connection>() ; 
	
	private static final String DEPLOYMENT_PROP_PREFIX = "db.deployment.";
	private static final String JUNIT_PROP_PREFIX = "db.junit.";
	private static final String SANDBOX_PROP_PREFIX = "db.sandbox.";
	private static final String SCHEMA_DEFINITION_FILE = "db/sql/schema.sql" ;
		
	/**
	 * SQL prepared statement to set default schema for a connection ;
	 */
	private static final String SQL_setSchema = "SET search_path TO " ;
	
	/**
	 * SQL statement to retrieve last value inserted automatically 
	 * into an identity column by the current connection
	 */
	private static final String SQL_identityQuery = "SELECT lastval() ;" ;
	
	private int dbType = DB_TYPE_ERROR;
	private String dbHost = "";
	private String dbPort = "" ;
	private String dbName = "";
	private String dbSchema = "";
	private String dbSSL = "" ;
	private String dbUser = "";
	private String dbPass = "";
	
	// These maybe should go in goat.Goat
	public static final String DEFAULT_DB_HOST = "127.0.0.1" ;
	public static final String DEFAULT_DB_PORT = "5432" ;
	public static final String DEFAULT_DB_NAME = "goatdb" ;
	public static final String DEFAULT_DB_SSL = "false" ;	

	/**
	 * Constructor.  This takes an int representing a db type, and sets things up
	 * accordingly.  Once you have your GoatDB instance, you just call its
	 * getConnection() to get your jdbc Connection object, and go 
	 * do your jdbc things from there.
	 */
	public GoatDB(int type) throws GoatDBConnectionException {
		init(type) ;
	}
	
	private void init(int type) throws GoatDBConnectionException {
		dbType = type;
		String prefix = "";
		switch (dbType) {
		case DB_TYPE_DEPLOYMENT:
			prefix = DEPLOYMENT_PROP_PREFIX;
			break;
		case DB_TYPE_JUNIT:
			prefix = JUNIT_PROP_PREFIX;
			break;
		case DB_TYPE_SANDBOX:
			prefix = SANDBOX_PROP_PREFIX;
			break;
		default:
			// For now, pretend there is no DB_TYPE_NONE
			/* 
			dbType = DB_TYPE_NONE ;
			return ;  // which would result in a pretty useless object
			*/
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			System.err.println("Bad DB type specified.  Expecting DB_TYPE_[DEPLOYMENT|JUNIT|SANDBOX]") ;
			throw gdbe ;
		}
		Properties props = Goat.getProps() ;
		dbHost = props.getProperty(prefix + "host", DEFAULT_DB_HOST) ;
		dbPort = props.getProperty(prefix + "port", DEFAULT_DB_PORT) ;
		dbName = props.getProperty(prefix + "name", DEFAULT_DB_NAME) ;
		dbSSL = props.getProperty(prefix + "ssl", DEFAULT_DB_SSL) ;
		dbUser = props.getProperty(prefix + "user") ;
		dbSchema = props.getProperty(prefix + "schema", dbUser) ;
		props = Goat.getPasswords() ;
		dbPass = props.getProperty(prefix + "password") ;
		while (janitorConnections.size() <= dbType)
			janitorConnections.add(null) ;
		if ((DB_TYPE_NONE != dbType)&&(null == janitorConnections.get(dbType)))
			janitorConnections.set(dbType, getConnection()) ;
	}
	
	public GoatDB() throws GoatDBConnectionException {
		String key = "db.type" ;
		String [] validTypes = { "DEPLOY", "JUNIT", "SAND" } ;  // note order
																// matters here
		Properties props = Goat.getProps() ;
		int type = -2 ;
		if((!ignoreConfigFile) && props.containsKey(key)) { 
			for(int i = 0; i<validTypes.length; i++) 
				if(validTypes[i].equalsIgnoreCase(props.getProperty(key).substring(0, validTypes[i].length())))
					type = i ;
		} else {
			type = defaultDbType ;
		}
		if (DB_TYPE_ERROR == type) {
			System.err.println("You need to either 1) specify db.type in your goat.properties file, 2) use the 'new GoatDB(int dbType) constructor, or 3) use the static methods setDefaultDbType(int type) and/or setIgnoreConfigFileDbType(boolean).") ;
			System.err.println("    Valid types are \"deployment\", \"junit\", or \"sandbox\"") ;
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			throw gdbe ;
		} else {
			init(type) ;
		}
	}
	
	/**
	 * Is the server up?
	 * 
	 * Don't call this inside loops; it has to create a new db connection every
	 * time it is called.
	 * 
	 * //TODO: figure out a more betterer way to check this.
	 * 
	 * @return true if the goat db server is running, false if it ain't.
	 */
	public boolean serverRunning() {
		if (DB_TYPE_NONE == dbType) {
			// we'll assume the no-db db is always up
			return true ;
		}
		Connection c;
		try {
			c = getConnection() ;
		} catch (Exception e) {
			return false;
		}
		try {
			c.close() ;
		} catch (SQLException e) {
		}
		return true;
	}
	
	/**
	 * Get a connection to the goat db. <p/>
	 * 
	 * @return a new jdbc connection to the goat db.
	 * @throws GoatDBConnectionException
	 *             if a connection could not be obtained, for any reason.
	 */
	public Connection getConnection() 
		throws GoatDBConnectionException {
		if (DB_TYPE_NONE == dbType)
			return null; //might want to change this if we come up with an actual use for the no-db db.
		Connection c;
		try {
			Class.forName("org.postgresql.Driver");
		} catch (Exception e) {
			System.err.println("ERROR: failed to load postgreSQL JDBC driver.");
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		try {
			c = DriverManager.getConnection(connectionUrl());
		} catch (SQLException e) {
			System.err.println("Couldn't get connection to DB server");
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		try {
			setSchema(dbSchema, c) ;
		} catch (SQLException e) {
			System.err.println("Couldn't set schema to \"" + dbSchema + "\"") ;
			e.printStackTrace() ;
			GoatDBConnectionException gdbe = new GoatDBConnectionException() ;
			gdbe.initCause(e) ;
			throw gdbe ;
		}
		return c;
	}
	
	public String connectionUrl() {
		String ret = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName +
			"?user=" + dbUser + "&password=" + dbPass;
		if (dbSSL.equalsIgnoreCase("true")||dbSSL.equalsIgnoreCase("yes"))
			ret += "&ssl=true" ;
		//ret += "&prepareThreshold=1" ;
		return ret;
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
	 *  creates a new Connection every time it's called.<p/>
	 * 
	 * If this method or GoatDB.executeUpdate(String) doesn't do what you need,
	 * start with GoatDB.getConnection() and go from there.
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
	public ResultSet executeQuery(String statement)
			throws GoatDBConnectionException, SQLException {
		Statement st = getConnection().createStatement() ;
		return st.executeQuery(statement) ;
	}
	
	/**
	 * "Convenience" method to do a query on teh goat DB.<p/>
	 * 
	 * see GoatDB.executeQuery(String) for details.<p/>
	 * 
	 * <b>Do Not</b> use this method for bulk inserts-- it creates a new
	 * Connection every time it's called.
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
	public int executeUpdate(String statement)
			throws GoatDBConnectionException, SQLException {
		Statement st = getConnection().createStatement();
		int ret = st.executeUpdate(statement);
		st.getConnection().close();
		return ret;
	}
	
	public boolean eraseSchema() {
		return eraseSchema(false) ;
	}
	
	public boolean eraseSchema(boolean allow_deployment_db_destruction) {
		System.out.println("Attempting to erase contents of schema \"" + dbSchema + "\" in database " + dbName + "\"") ; 
		boolean success = false ;
		
		// bail out if someone is trying to delete the deployment db with the safety on
		if (DB_TYPE_DEPLOYMENT == dbType) {
			System.err.println("  erasure of deployment schema not permitted.") ;
			return false ;
		}

		// schema is already erased if we're using the no-db db.
		if (DB_TYPE_NONE == dbType)
			return true ;
		
		// Erase the schema
		Connection conn = janitorConnections.get(dbType) ;
		try {
			if (hasSchema(dbSchema, conn)) {
				//String [] procs = getProcNames() ;
				String [] tables = getTableNames() ;
				
				/*
				System.out.print("Dropping Procs:  ") ;
				for(int i=0; i < procs.length; i++) 
					System.out.print(procs[i] + " ") ;
				System.out.println() ;
				*/
				
				System.out.print("Dropping tables:  ") ;
                for (String table1 : tables) System.out.print(table1 + " ");
                System.out.println() ;
				
				/*
				 * This block is a big ol' debugging dump, for getting a handle on 
				 * the output of getProcedureColumns
				DatabaseMetaData dmd = conn.getMetaData() ;
				for(int i=0; i<procs.length; i++) {
					ResultSet rs = dmd.getProcedureColumns(null,dbSchema,procs[i],null) ;
					//ResultSet rs = dmd.getProcedureColumns(null,null,null,null) ;
					while(rs.next()) {
						String ptype = "" ;
						short pcode = rs.getShort(5) ;
						switch (pcode) {
						case DatabaseMetaData.procedureColumnIn:
							ptype = "in" ;
							break;
						case DatabaseMetaData.procedureColumnInOut:
							ptype = "in/out" ;
							break;
						case DatabaseMetaData.procedureColumnOut:
							ptype = "out" ;
							break;
						case DatabaseMetaData.procedureColumnResult:
							ptype = "result" ;
							break ;
						case DatabaseMetaData.procedureColumnReturn:
							ptype = "return" ;
							break;
						case DatabaseMetaData.procedureColumnUnknown:
							ptype = "unknown" ;
							break;
						default :
							ptype = "WTF???" ;
						}
						System.out.printf("%-40s %15s %8s %15s\n", rs.getString(3), rs.getString(4), ptype, rs.getString(7)) ;
					}
				}
				*/
				
				Statement st = conn.createStatement() ;
				conn.setAutoCommit(false) ;

				/*
				for (int i=0; i<procs.length; i++) {
					//TODO: drop the proc.  it's not at all clear we can do this 100%
					// reliably via jdbc, as DatabaseMetaData.getProcedureColumns()
					// does not give you a unique procedure identifier
					// 
					// if we're lucky, when we drop the tables with CASCADE, any functions
					//   we've set up will be wiped...
					//
					// st.addBatch(dropProcSQL(procs[i], getProcArgTypes(procs[i]))) ;
				}
				*/

                for (String table : tables) st.addBatch(dropTableSQL(table));
                st.executeBatch() ;
				conn.commit() ;
				conn.setAutoCommit(true) ;
				success = true ;
			}
		} catch (SQLException e) {
			e.printStackTrace() ;
			e.getNextException().printStackTrace();
			success = false ;
		}
		System.out.println("Erased contents of schema \"" + dbSchema + "\" in database \"" + dbName + "\"") ;
		return success ;
	}
	
	private String dropTableSQL(String table) {
		return "DROP TABLE " + table + " CASCADE ;" ;
	}
	
	/* kept lying around in case I take up the task again of dropping stored procs in a schema --rs
	private String dropProcSQL(String proc, String [] argTypes) {
		String ret =  "DROP FUNCTION " + proc + " (" ;
		for(int i=0; i<argTypes.length; i++) { 
			ret += argTypes[i] ;
			if (i+1 != argTypes.length)
				ret += "," ;
		}
		return ret;
	}
	*/
	
	/**
	 * .
	 * 
	 * @param s name of the schema you want to check for.
	 * @param c connection to the db you want to check
	 * @return
	 * @throws SQLException
	 */
	private boolean hasSchema(String s, Connection c) {
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
	
	public boolean hasTable(String table, Connection c) {
		boolean found = false ;
		String [] tables = getTableNames() ;
        for (String table1 : tables)
            if (table.equalsIgnoreCase(table1)) {
                found = true;
                break;
            }
        return found;
	}
	
	public String [] getProcNames() {
		ArrayList<String> names = new ArrayList<String>() ;
		Connection c = janitorConnections.get(dbType) ;
		try {
			ResultSet rs = c.getMetaData().getProcedures(null, dbSchema, null) ;
			while (rs.next()) {
				names.add(rs.getString(3)) ;
			}
			rs.close() ;
		} catch (SQLException e) {
			System.err.println("WARNING:  Problem retrieving stored procedures list from db \"" + dbName + "\", schema \"" + dbSchema + "\"") ;
		}
		return names.toArray(new String[0]) ;	
	}
	
	public String[] getTableNames() {
		ArrayList<String> names = new ArrayList<String>();
		Connection c = janitorConnections.get(dbType);
		try {
			ResultSet rs = c.getMetaData()
					.getTables(null, dbSchema, null, null);
			while (rs.next()) {
				String type = rs.getString(4);
				// sometimes, tables are not tables.  let's avoid that problem.
				if (type.equals("TABLE"))
					names.add(rs.getString(3));
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("WARNING:  Problem retrieving tables list from db \"" + dbName + "\", schema \"" + dbSchema + "\"");
		}
		return names.toArray(new String[0]);
	}
	
	public boolean hasSchema(String s) {
		return hasSchema(s, janitorConnections.get(dbType)) ;
	}
	
	public boolean hasTable(String s) {
		return hasTable(s, janitorConnections.get(dbType)) ;
	}
	
	public boolean recreateSchema() {
		boolean success = eraseSchema() ;
		if (success)
			success = loadSchemaFile() ;
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
	public boolean loadSchemaFile() {
		// TODO: prevent accidental execution on deployment db
		return loadSQLFile(SCHEMA_DEFINITION_FILE) ;
	}
	
	/**
	 * Execute the commands in an SQL file.<p/>
	 * 
	 * <p>
	 * The line-base "parser" in here that breaks the file down into individual
	 * sql statments is crap, but hey, jdbc doesn't think you'll ever have more
	 * than one sql statement in a string, so what are you gonna do?
	 * </p>
	 * 
	 * @param filename
	 * @return
	 */
	public boolean loadSQLFile(String filename) {
		boolean success = false ;
		if (DB_TYPE_NONE == dbType) 
			return true; // always report success if there's no real db
		String sqlblock = "" ;
		try {
			// behold the wonders of Scanner
			sqlblock = new Scanner( new File(filename) ).useDelimiter("\\A").next();
			

			//now our sql file "parser".
//			ArrayList<String> lines = new ArrayList<String>() ; 
//			lines.addAll(Arrays.asList(sqlblock.split("\n"))) ;
			//remove blank lines and one-line comments
//			String line = "" ;
//			for(int i=lines.size()-1; i>=0; i--) {
//				line  = lines.get(i).trim() ;
//				if (lines.get(i).matches("^\\s*$")
//						|| line.matches("^\\s*--.*")
//						|| lines.get(i).matches("^\\s*/\\*.*\\*/\\s*$")) 
//					lines.remove(i);
//			}
			//turn the arraylist of lines back into a string
//			sqlblock = "" ;
//			for(int i=0; i <lines.size(); i++)
//				sqlblock += lines.get(i) ;
			
			
			//ugly regex.  (?m) turns on multiline mode, which alters the behavior of ^ and $
			String [] statements = sqlblock.split("(?m);\\s*$\\s*") ;
			// uncomment below to check regex
			// for(int i=0; i<statements.length; i++)
			//	System.out.println(i + ":  " + statements[i]) ;
			Connection conn = janitorConnections.get(dbType) ;
			conn.setAutoCommit(false) ;
			Statement st = conn.createStatement() ;
			//PreparedStatement ps = conn.prepareStatement(sqlblock) ;
			//ps.execute() ;

            for (String statement : statements) st.addBatch(statement);
            st.executeBatch() ;
			//st.execute(sqlblock) ;
			conn.commit() ;
			conn.setAutoCommit(true) ;
			success = true ;
		} catch (FileNotFoundException e) {
			System.err.println("WARNING: SQL file \"" + filename +  "\" not found") ;
			e.printStackTrace() ;
		} catch (SQLException e) {
			System.err.println("Problem exectuting SQL in file \"" + filename + "\"") ;
			e.getNextException().printStackTrace() ;  //to see errors in batch
			//e.printStackTrace() ;
		}
		return success ;
	}
		
	public void setSchema(String schema, Connection c) throws SQLException {
		if (hasSchema(schema, c)) {
			PreparedStatement ps = c.prepareStatement(SQL_setSchema + schema) ;
			ps.execute() ;
		} else {
			SQLException e = new SQLException("Schema \"" + schema + "\" not found in db") ;
			throw(e) ;
		}
	}
	
	/**
	 * retrtieve the last IDENTITY column value generated by a connection;
	 * @param c
	 * @return
	 */
	public static int getIdentity(Connection c) {
		int ret = -1 ;
		try {
			ResultSet rs = c.createStatement().executeQuery(SQL_identityQuery) ;
			if (rs.next()) 
				ret = rs.getInt(1) ;
		} catch (SQLException e) {
			System.err.println("Problem retrieving identity via \"" + SQL_identityQuery + "\"") ;
			e.printStackTrace() ;
		}
		return ret ;
	}

	public static void setIgnoreConfigFileDbType(boolean ignore) {
		ignoreConfigFile = ignore ;
	}
	
	public static void setDefaultDbType(int type) {
		defaultDbType = type ;
	}
	
	public class GoatDBConnectionException extends SQLException {
		// eclipse complains without this.
		private static final long serialVersionUID = 1L;
	}

	public class GoatDBNoResultSetException extends Exception {
		// eclipse complains without this.
		private static final long serialVersionUID = 1L;
	}
}



