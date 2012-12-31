package goojax.search;

import goojax.GooJAXResponse;

public abstract class SearchResponse<T extends SearchResponseData<?>> extends GooJAXResponse {
	
	public T responseData;
	
	public SearchResponse() {
		super();
	}

	public T getResponseData() {
		return responseData;
	}

	public void setResponseData(T responseData) {
		this.responseData = responseData;
	}
	
	public int getEstimatedResultCount() {
		return responseData.cursor.estimatedResultCount;
	}
}
