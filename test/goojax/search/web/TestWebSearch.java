package goojax.search.web;

import goojax.search.SearchResponse;
import goojax.search.blog.BlogSearcher;
import goojax.search.image.ImageSearcher;
import goojax.search.video.VideoSearcher;
import junit.framework.TestCase;

public class TestWebSearch extends TestCase {

	/*	
		public static Test suite() {
			return new TestSuite(GooJAXTest.class);
		}
	*/
		public void testWebSearch() {
			try {
				String query = "2 liters to ounces";
				
				WebSearcher ws = new WebSearcher();
				SearchResponse srs = ws.search(query);
				assertTrue(srs != null);
				
				for(int i = 0; i < srs.getResponseData().getResults().length; i++)
					System.out.println(i + "\t" + srs.getResponseData().getResults()[i].getTitle());
				
				System.out.println();
	
				query = "cats and dogs";
	
				
				ImageSearcher  is  = new ImageSearcher();
				BlogSearcher   bgs = new BlogSearcher();
				VideoSearcher  vs  = new VideoSearcher();
				
				srs = ws.search(query);
				for(int i=0;i<srs.getResponseData().getResults().length;i++)
					System.out.println(i + "\t" + srs.getResponseData().getResults()[i].getGsearchResultClass() + "\t" + srs.getResponseData().getResults()[i].getTitleNoFormatting());			
				srs = is.search(query);
				for(int i=0;i<srs.getResponseData().getResults().length;i++)
					System.out.println(i + "\t" + srs.getResponseData().getResults()[i].getGsearchResultClass() + "\t" + srs.getResponseData().getResults()[i].getTitleNoFormatting());			
				srs = bgs.search(query);
				for(int i=0;i<srs.getResponseData().getResults().length;i++)
					System.out.println(i + "\t" + srs.getResponseData().getResults()[i].getGsearchResultClass() + "\t" + srs.getResponseData().getResults()[i].getTitleNoFormatting());			
				srs = vs.search(query);
				for(int i=0;i<srs.getResponseData().getResults().length;i++)
					System.out.println(i + "\t" + srs.getResponseData().getResults()[i].getGsearchResultClass() + "\t" + srs.getResponseData().getResults()[i].getTitleNoFormatting());			
				//ns.setQuery("");
				//srs = ns.search();
				//assertTrue(null != srs);
				//for(int i=0;i<srs.responseData.results.length;i++)
				//	System.out.println(i + "\t" + srs.responseData.results[i].GsearchResultClass + "\t" + srs.responseData.results[i].titleNoFormatting);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

}
