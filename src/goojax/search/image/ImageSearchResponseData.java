package goojax.search.image;

import goojax.search.SearchResponseData;

public class ImageSearchResponseData extends SearchResponseData {
	public ImageSearchResult results[];
	
	public ImageSearchResponseData() {
		super();
	}

	public ImageSearchResult[] getResults() {
		return results;
	}

	public void setResults(ImageSearchResult[] results) {
		this.results = results;
	}
}
