package goojax.search.video;

import goojax.search.SearchResponseData;

public class VideoSearchResponseData extends SearchResponseData {
	public VideoSearchResult results[];
	//public VideoSearchResponseData() {
		//super();
	//}

	public VideoSearchResult[] getResults() {
		return results;
	}

	public void setResults(VideoSearchResult[] results) {
		this.results = results;
	}
	
}
