package goojax.search.patent;

import junit.framework.TestCase;

public class TestPatentSearch extends TestCase {
	public void testPatentSearch() {
		String query = "cats and dogs";
		PatentSearcher ps  = new PatentSearcher();
		PatentSearchResponse psr;
		try {
		psr = ps.search(query);
		for(int i=0;i<psr.getResponseData().getResults().length;i++)
			System.out.println(
					psr.getResponseData().getResults()[i].getPatentNumber()
					+ "\t" + psr.getResponseData().getResults()[i].getApplicationDate()
					+ "\t" + psr.getResponseData().getResults()[i].getPatentStatus().code
					+ "\t" + psr.getResponseData().getResults()[i].getTitleNoFormatting()
					+ " \u2014 " + psr.getResponseData().getResults()[i].getAssignee());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}
}
