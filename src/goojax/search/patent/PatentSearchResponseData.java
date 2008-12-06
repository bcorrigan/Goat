package goojax.search.patent;

import goojax.search.SearchResponseData;

public class PatentSearchResponseData extends SearchResponseData {
	PatentSearchResult results[];
	
	PatentSearchResponseData() {
		super();
	}

	public PatentSearchResult[] getResults() {
		return results;
	}

	public void setResults(PatentSearchResult[] results) {
		this.results = results;
	}
}
