package goojax.search.blog;

import goojax.search.SearchResponse;

public class BlogSearchResponse extends SearchResponse {
	public BlogSearchResponseData responseData;
	
	public BlogSearchResponse() {
		super();
	}

	public BlogSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(BlogSearchResponseData responseData) {
		this.responseData = responseData;
	}
	
}
