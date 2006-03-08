package goat.util;

import goat.util.GoatGoogle ;
import com.google.soap.search.* ;
import junit.framework.* ;
import static junit.framework.Assert.* ;

import java.util.* ;

/**
 * junit-ness.
 * <p/>
 * This, too, really belongs in goat.util.
 */
public class GoatGoogleTest extends TestCase {

	public static Test suite() {
		return new TestSuite(GoatGoogleTest.class);
	}

	public void testNoResults() {
		String garbageQuery = "qwpdfasdh98r72sjkdvnaqer7934q34tsdjkfga" ;
		try {
			GoogleSearchResultElement re = GoatGoogle.feelingLucky(garbageQuery) ;
			Assert.assertEquals(re, null) ;
		} catch (GoogleSearchFault f) {
			System.out.println(f.toString()) ;
		}
	}
}
