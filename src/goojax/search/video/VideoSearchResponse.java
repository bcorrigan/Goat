package goojax.search.video;

import goojax.search.SearchResponse;

public class VideoSearchResponse extends SearchResponse {
	public VideoSearchResponseData responseData;
	
	public VideoSearchResponse() {
		super();
	}

	public VideoSearchResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(VideoSearchResponseData responseData) {
		this.responseData = responseData;
	}
}
