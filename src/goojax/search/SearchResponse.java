package goojax.search;

import goojax.GooJAXResponse;

public abstract class SearchResponse extends GooJAXResponse {
	
	public SearchResponseData responseData;
	
	public SearchResponse() {
		super();
	}

	public SearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(SearchResponseData responseData) {
		this.responseData = responseData;
	}
	
	public int getEstimatedResultCount() {
		return responseData.cursor.estimatedResultCount;
	}
}
