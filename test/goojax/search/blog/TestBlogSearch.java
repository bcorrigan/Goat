package goojax.search.blog;

import junit.framework.TestCase;

public class TestBlogSearch extends TestCase {
	public void testSearch() {
		String query = "cats and dogs";
		BlogSearcher   bgs = new BlogSearcher();
		BlogSearchResponse bsr;
		try {
			bsr = bgs.search(query);
			for(int i=0;i<bsr.getResponseData().getResults().length;i++)
				System.out.println(
						bsr.getResponseData().getResults()[i].getPublishedDate()
						+ "\t" + bsr.getResponseData().getResults()[i].getAuthor()
						+ "\t\t" + bsr.getResponseData().getResults()[i].getBlogUrl()
						+ "\n\t" + bsr.getResponseData().getResults()[i].getTitleNoFormatting()
						+ "\n\t" + bsr.getResponseData().getResults()[i].getContent());
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
