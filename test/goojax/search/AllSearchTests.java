package goojax.search;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllSearchTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for goojax.search");
		//$JUnit-BEGIN$
		suite.addTestSuite(goojax.search.blog.TestBlogSearch.class);
		suite.addTestSuite(goojax.search.book.TestBookSearch.class);
		suite.addTestSuite(goojax.search.image.TestImageSearch.class);
		suite.addTestSuite(goojax.search.local.TestLocalSearch.class);
		suite.addTestSuite(goojax.search.news.TestNewsSearch.class);
		suite.addTestSuite(goojax.search.patent.TestPatentSearch.class);
		suite.addTestSuite(goojax.search.video.TestVideoSearch.class);
		suite.addTestSuite(goojax.search.web.TestWebSearch.class);
		//$JUnit-END$
		return suite;
	}

}
