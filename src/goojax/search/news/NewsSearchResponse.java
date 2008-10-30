package goojax.search.news;

import goojax.search.SearchResponse;

public class NewsSearchResponse extends SearchResponse {
	protected NewsSearchResponseData responseData;

	public NewsSearchResponse() { super();}
	
	public NewsSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(NewsSearchResponseData responseData) {
		this.responseData = responseData;
	}
	
}
