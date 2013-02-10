package goojax.search;

import goojax.GooJAXResponse;

public class SearchResponse<T extends AbstractSearchResult> extends GooJAXResponse {
	
	public SearchResponseData<T> responseData;
	
	public SearchResponse() {
		super();
	}

	public SearchResponseData<T> getResponseData() {
		return responseData;
	}

	public void setResponseData(SearchResponseData<T> responseData) {
		this.responseData = responseData;
	}
	
	public int getEstimatedResultCount() {
		return responseData.cursor.estimatedResultCount;
	}
}
