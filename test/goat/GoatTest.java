package goat;


import junit.framework.TestCase;

import goat.db.GoatDB ;

/** 
 * Superclass for all goaty testing.
 * <p/>
 * All of your goat-wide testing start-up and clean-up stuff can go here.
 * Add startup things to setUp(), clean-up stuff to tearDown().  Document 
 * your changes in the lists below: 
 * <p/>
 * setUp() activities;
 * <ol>
 * <li>Start up Test DB</li>
 * <li>Wipe everything in the test DB</li>
 * <li>Set up fresh Goat schema in test DB</li>
 * </ol>
 * 
 * tearDown() activities:
 * <ol>
 * <li>Wipe test DB<li>
 * <li>Shut down test DB</li>
 * </ol>
 * 
 * @author rs
 *
 */
/**
 * @author Ed
 *
 */
/**
 * @author Ed
 *
 */
public class GoatTest extends TestCase {
	
	protected GoatDB db = null;

	protected void setUp() {
		System.out.println(this.getName() + ": entering setUp()") ;
		
		//start db init
		GoatDB.setDefaultDbType(GoatDB.DB_TYPE_JUNIT) ;
		GoatDB.setIgnoreConfigFileDbType(true) ;
		try {
			db = new GoatDB(GoatDB.DB_TYPE_JUNIT) ;
		} catch (GoatDB.GoatDBConnectionException e) {
			e.printStackTrace() ;
		}
		assertTrue((null != db) && db.recreateSchema()) ;
		//end db init
		
		System.out.println(this.getName() + ": exiting setUp()") ;
	}

	protected void tearDown() {
		System.out.println(this.getName() + ": entering tearDown()") ;
		
		//start db cleanup
		assertTrue(db.eraseSchema()) ;
		//end db cleanup
		
		System.out.println(this.getName() + ": exiting tearDown()") ;
	}
	
	public void testNothing() {
		// to avoid the dreaded "no tests defined" error.
	}

}
