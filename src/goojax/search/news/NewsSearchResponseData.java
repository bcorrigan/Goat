package goojax.search.news;

import goojax.search.SearchResponseData;

public class NewsSearchResponseData extends SearchResponseData {
	protected NewsSearchResult results[];

	NewsSearchResponseData() { super(); }
	
	public NewsSearchResult[] getResults() {
		return results;
	}

	public void setResults(NewsSearchResult[] results) {
		this.results = results;
	}
}
