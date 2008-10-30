package goojax.search;

import goojax.GooJAXResponse;

public class SearchResponse extends GooJAXResponse {
	
	protected SearchResponseData responseData;
	
	protected SearchResponse() {
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
