package goojax.search;

import goojax.search.SearchResponse;
import goojax.search.WebSearcher;
import junit.framework.TestCase;

public class TestWebSearch extends TestCase {

	/*	
		public static Test suite() {
			return new TestSuite(GooJAXTest.class);
		}
	*/
		public void testWebSearch() {
			try {
				System.out.println("Testing web search:");
				
				WebSearcher ws = new WebSearcher();
				SearchResponse srs;
	
				String query = "cats and dogs";
				System.out.println("\tquery: \"" + query + "\"");
			
				srs = ws.search(query);
				System.out.println("\testimated results: " + srs.getEstimatedResultCount());
				assertTrue(srs.statusNormal());
				for(int i=0;i<srs.getResponseData().getResults().length;i++)
					System.out.println(
							"\t" + (i + 1) 
							+ "  " + srs.getResponseData().getResults()[i].getTitleNoFormatting()
							+ "\t" + srs.getResponseData().getResults()[i].getUnescapedUrl() 
							);			
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

}
