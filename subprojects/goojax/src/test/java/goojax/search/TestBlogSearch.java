package goojax.search;

import junit.framework.TestCase;
import goojax.search.BlogSearchResult;
import goojax.search.BlogSearcher;
import goojax.search.SearchResponse;

public class TestBlogSearch extends TestCase {
	public void testSearch() {
		System.out.println("Testing Blog Search");
		String query = "cats and dogs";
		BlogSearcher   bgs = new BlogSearcher();
		SearchResponse<BlogSearchResult> bsr;
		try {
			bsr = bgs.search(query);
			assertTrue(bsr.statusNormal());
			for(int i=0;i<bsr.getResponseData().getResults().length;i++)
				System.out.println(
						"\t" + bsr.getResponseData().getResults()[i].getPublishedDate()
						+ "\t\t" + bsr.getResponseData().getResults()[i].getAuthor()
						+ "\t" + bsr.getResponseData().getResults()[i].getBlogUrl()
						+ "\n\t\t" + bsr.getResponseData().getResults()[i].getTitleNoFormatting()
						+ "\n\t\t" + bsr.getResponseData().getResults()[i].getContent());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
