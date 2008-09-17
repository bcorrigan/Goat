package goat.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import static goat.util.Confessions.*;
import static goat.util.GroupHugUS.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;

import com.sleepycat.persist.EntityCursor;

public class GroupHugTest extends TestCase {
	public static Test suite() {
		return new TestSuite(GroupHugTest.class);
	}
	
	public void testConfessions() {
		ArrayList<Confession> confessions = new ArrayList<Confession>();
		try {
			// uncomment here to import the BC file
			//importBcFile("resources/confessions.gz");
			
			confessions = getNewConfessionsFromPageNumber(0);
			Assert.assertFalse(confessions.isEmpty());  // check to see that we can retrieve new confessions
			Iterator<Confession> it = confessions.iterator();
			int newConfessions = 0;
			long origInDb = dbCount();
			while(it.hasNext()) {
				Confession conf = it.next();
				// uncomment here to inspect newly-fetched confessions on the console
				// System.out.println(conf + "\n#ENDCONF");
				if(!dbContains(conf.id)) {
					dbPut(conf);
					newConfessions++;
				}
			}
			Assert.assertTrue(dbCount() != 0);
			EntityCursor<Confession> cursor = dbEntityCursor();
			Iterator<Confession> bigIt = cursor.iterator();
			// uncomment here to inspect some confessions from the db on the console
			/*
			int limit = 20;
			while(limit > 0 && bigIt.hasNext()) {
				System.out.println(bigIt.next().id);
				limit--;
			}
			cursor.close();
			*/
			if(newConfessions > 0)
				System.out.println(newConfessions + " new confessions were added");
			else
				System.out.println("confessions successfully retrieved, but none were new");
			System.out.println(dbCount() + " total confessions in the db.");
			System.out.println("   (started with " + origInDb + ")");
			System.out.println("...searching for \"poon\"");
			ArrayList<Integer> hits = search("poon");
			System.out.println("Found " + hits.size() + " results");
			if (hits.size() > 0) {
				System.out.println("Sample:\n   " + dbGet(hits.get(0)).content);
			}
			System.out.println("...searching for \"donk*\"");
			hits = search("donk*");
			System.out.println("Found " + hits.size() + " results");
			if (hits.size() > 0) {
				System.out.println("Sample:\n   " + dbGet(hits.get(0)).content);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
