package goojax.search.local;

import goojax.search.SearchResponse;

public class LocalSearchResponse extends SearchResponse {
	public LocalSearchResponseData responseData;
	
	public LocalSearchResponse() {
		super();
	}

	public LocalSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(LocalSearchResponseData responseData) {
		this.responseData = responseData;
	}

}
