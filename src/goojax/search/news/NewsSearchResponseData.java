package goojax.search.news;

import goojax.search.SearchResponseData;

public class NewsSearchResponseData extends SearchResponseData {
	public NewsSearchResult results[];

	public NewsSearchResponseData() { super(); }
	
	public NewsSearchResult[] getResults() {
		return results;
	}

	public void setResults(NewsSearchResult[] results) {
		this.results = results;
	}
}
