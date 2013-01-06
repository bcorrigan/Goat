package goojax.search;

import junit.framework.TestCase;
import goojax.search.NewsSearchResult;
import goojax.search.NewsSearcher;
import goojax.search.SearchResponse;

public class TestNewsSearch extends TestCase {

	public void testNewsSearch() {
		System.out.println("Testing news search");
		NewsSearcher   ns  = new NewsSearcher();
		String query = "cats and dogs";
		try {
			SearchResponse<NewsSearchResult> nss = ns.search(query);
			assertTrue(nss.statusNormal());
			for(int i=0;i<nss.getResponseData().getResults().length;i++)
				System.out.println(
						"\t" + nss.getResponseData().getResults()[i].getTitleNoFormatting()
						+ "\t" + nss.getResponseData().getResults()[i].getPublisher()
						+ "\t" + nss.getResponseData().getResults()[i].getPublishedDate()
						+ "\t" + nss.getResponseData().getResults()[i].getLocation());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	
	}

}
