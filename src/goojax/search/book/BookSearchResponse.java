package goojax.search.book;

import goojax.search.SearchResponse;

public class BookSearchResponse extends SearchResponse {
	public BookSearchResponseData responseData;
	
	public BookSearchResponse() {
		super();
	}

	public BookSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(BookSearchResponseData responseData) {
		this.responseData = responseData;
	}
}
