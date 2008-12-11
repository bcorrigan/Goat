package goojax.search.image;

import goojax.search.SearchResponse;

public class ImageSearchResponse extends SearchResponse {
	public ImageSearchResponseData responseData;
	
	public ImageSearchResponse() {
		super();
	}

	public ImageSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(ImageSearchResponseData responseData) {
		this.responseData = responseData;
	}
}
