package goojax.search;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllSearchTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for goojax.search");
		//$JUnit-BEGIN$
		suite.addTestSuite(goojax.search.TestBlogSearch.class);
		suite.addTestSuite(goojax.search.TestBookSearch.class);
		suite.addTestSuite(goojax.search.TestImageSearch.class);
		suite.addTestSuite(goojax.search.TestLocalSearch.class);
		suite.addTestSuite(goojax.search.TestNewsSearch.class);
		suite.addTestSuite(goojax.search.TestPatentSearch.class);
		suite.addTestSuite(goojax.search.TestVideoSearch.class);
		suite.addTestSuite(goojax.search.TestWebSearch.class);
		//$JUnit-END$
		return suite;
	}

}
