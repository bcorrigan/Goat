package goojax.search.news;

import junit.framework.TestCase;

public class TestNewsSearch extends TestCase {

	public void testNewsSearch() {
		NewsSearcher   ns  = new NewsSearcher();
		String query = "cats and dogs";
		System.out.println("\nNews Search\n");
		try {
			NewsSearchResponse nss = ns.search(query);
			for(int i=0;i<nss.getResponseData().getResults().length;i++)
				System.out.println(i + "\t" + nss.getResponseData().getResults()[i].getTitleNoFormatting()
						+ "\t" + nss.getResponseData().getResults()[i].getPublisher()
						+ "\t" + nss.getResponseData().getResults()[i].getPublishedDate()
						+ "\t" + nss.getResponseData().getResults()[i].getLocation());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	
	}

}
