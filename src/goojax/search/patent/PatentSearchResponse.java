package goojax.search.patent;

import goojax.search.SearchResponse;

public class PatentSearchResponse extends SearchResponse {
	public PatentSearchResponseData responseData;
	
	public PatentSearchResponse() {
		super();
	}

	public PatentSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(PatentSearchResponseData responseData) {
		this.responseData = responseData;
	}

}
