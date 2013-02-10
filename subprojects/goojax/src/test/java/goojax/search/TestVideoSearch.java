package goojax.search;

import junit.framework.TestCase;
import goojax.search.SearchResponse;
import goojax.search.VideoSearchResult;
import goojax.search.VideoSearcher;

public class TestVideoSearch extends TestCase {
	public void testVideoSearch() {
		System.out.println("Testing video search:");
		String query = "cats and dogs";
		VideoSearcher vs  = new VideoSearcher();
		SearchResponse<VideoSearchResult> vsr;
		try {
		vsr = vs.search(query);
		assertTrue(vsr.statusNormal());
		VideoSearchResult res[] = vsr.getResponseData().getResults();
		for(int i=0;i<res.length;i++)
			System.out.println(
					"\t" + res[i].getPublished() 
					+ "\t" + res[i].getTitleNoFormatting()
					+ ", " + res[i].getAuthor()
					+ " (" + res[i].getPublisher() + ")"
					+ " " + res[i].getDurationString()
					+ ", " + res[i].getViewCount() + " views"
					+ ", rating: " + res[i].getRating());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
