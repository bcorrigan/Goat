package goojax.search;

import junit.framework.TestCase;
import goojax.search.PatentSearchResult;
import goojax.search.PatentSearcher;
import goojax.search.SearchResponse;

public class TestPatentSearch extends TestCase {
	public void testPatentSearch() {
		System.out.println("Testing patent search:");
		String query = "cats and dogs";
		PatentSearcher ps  = new PatentSearcher();
		SearchResponse<PatentSearchResult> psr;
		try {
		psr = ps.search(query);
		assertTrue(psr.statusNormal());
		for(int i=0;i<psr.getResponseData().getResults().length;i++)
			System.out.println(
					"\t" + psr.getResponseData().getResults()[i].getPatentNumber()
					+ "\t" + psr.getResponseData().getResults()[i].getApplicationDate()
					+ "\t" + psr.getResponseData().getResults()[i].getPatentStatus().code
					+ "\t" + psr.getResponseData().getResults()[i].getTitleNoFormatting()
					+ " \u2014 " + psr.getResponseData().getResults()[i].getAssignee());
		query = "toaster";
		psr = ps.search(query);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
}
