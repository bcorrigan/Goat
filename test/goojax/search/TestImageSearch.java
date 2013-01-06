package goojax.search;

import junit.framework.TestCase;
import goojax.search.ImageSearchResult;
import goojax.search.ImageSearcher;
import goojax.search.SearchResponse;

public class TestImageSearch extends TestCase {
	public void testImageSearch() {
		System.out.println("Testing image search:");
		String query = "cats and dogs";
		ImageSearcher  is  = new ImageSearcher();
		try {
		SearchResponse<ImageSearchResult> isr = is.search(query);
		for(int i=0;i<isr.getResponseData().getResults().length;i++)
			System.out.println(
					"\t" + isr.getResponseData().getResults()[i].getWidth()
					+ "x" + isr.getResponseData().getResults()[i].getHeight()
					+ "\t" + isr.getResponseData().getResults()[i].getUrl());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
