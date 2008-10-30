package goojax.search.book;

import goojax.search.SearchResult;

public class BookSearchResult extends SearchResult {
	public String authors;
	public String bookId;
	public Integer publishedYear;
	public Integer pageCount;
	public String thumbnailHtml;
	
	BookSearchResult() {
		super();
	}
}
