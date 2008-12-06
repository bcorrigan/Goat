package goojax.search.book;

import goojax.search.SearchResponseData;

public class BookSearchResponseData extends SearchResponseData {
	BookSearchResult results[];
	
	public BookSearchResponseData() {
		super();
	}

	public BookSearchResult[] getResults() {
		return results;
	}

	public void setResults(BookSearchResult[] results) {
		this.results = results;
	}
	
}
