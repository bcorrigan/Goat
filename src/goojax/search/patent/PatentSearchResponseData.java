package goojax.search.patent;

import goojax.search.SearchResponseData;

public class PatentSearchResponseData extends SearchResponseData {
	public PatentSearchResult results[];
	
	public PatentSearchResponseData() {
		super();
	}

	public PatentSearchResult[] getResults() {
		return results;
	}

	public void setResults(PatentSearchResult[] results) {
		this.results = results;
	}
}
