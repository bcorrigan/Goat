package goojax.book;

import goojax.search.book.BookSearchResult;
import goojax.search.book.BookSearcher;
import goojax.search.book.BookSearchResponse;
import junit.framework.TestCase;

public class TestBookSearch extends TestCase {
	public void testBookSearch() {
		BookSearcher   bs  = new BookSearcher();
		BookSearchResponse bsrs;
		BookSearchResult br;
		String query = "cats and dogs";
		try {
			bsrs = bs.search(query);
			for(int i=0;i<bsrs.getResponseData().getResults().length;i++)
				System.out.println(i 
						+ "\t" + bsrs.getResponseData().getResults()[i].getBookId()
						+ "\t" + bsrs.getResponseData().getResults()[i].getPublishedYear()
						+ "\t" + bsrs.getResponseData().getResults()[i].getTitleNoFormatting()
						+ ", " + bsrs.getResponseData().getResults()[i].getPageCount() + "pp."
						+ "  " + bsrs.getResponseData().getResults()[i].getUnescapedUrl());
			query = "the gate With dreadful faces thronged and fiery arms: Some natural tears they dropped, but wiped them soon;";
			bsrs = bs.search(query);
			if(! bsrs.statusNormal()) {
				System.out.println("Search problem: " + bsrs.getResponseStatus() + ", " + bsrs.getResponseDetails());
				assertTrue(false);
			}
			br = bsrs.getResponseData().getResults()[0];
			System.out.println("query:  \"" + query + "\"");
			System.out.println(br.getAuthors() + ", " + br.getTitle() + ", " + br.getPublishedYear());
			System.out.print(" ...by John Milton? ");
			assertTrue(br.getAuthors().contains("John Milton"));
			System.out.println("y");
			System.out.print(" ...titled Paradise Lost? ");
			assertTrue(br.getTitle().contains("Paradise Lost"));
			System.out.println("y");
			System.out.print(" ...published 1918? ");
			assertTrue(br.getPublishedYear() == 1918);
			System.out.println("y");
	
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
