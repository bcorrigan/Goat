package goojax.search.local;

import junit.framework.TestCase;

public class TestLocalSearch extends TestCase {
	public void testLocalSearch() {
		System.out.println("Testing local search:");
		String query = "bahai";
		LocalSearcher ls = new LocalSearcher();
		ls.setLatitude(42.068763F);
		ls.setLongitude(-87.704699F);
		try {
			LocalSearchResponse lsr = ls.search(query);
			assertTrue(lsr.statusNormal());
			LocalSearchResult res[] = lsr.getResponseData().getResults();
			for(int i=0;i<res.length;i++) {
				System.out.println(
						"\t" + res[i].country
						+ "\t" + res[i].region
						+ "\t" + res[i].city
						+ "\t" + res[i].getTitleNoFormatting());
			}
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
