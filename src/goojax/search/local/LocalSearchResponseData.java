package goojax.search.local;

import goojax.search.SearchResponseData;

public class LocalSearchResponseData extends SearchResponseData {
	public LocalSearchResult results[];
	
	public LocalSearchResponseData() {
		super();
	}

	public LocalSearchResult[] getResults() {
		return results;
	}

	public void setResults(LocalSearchResult[] results) {
		this.results = results;
	}
	
}
