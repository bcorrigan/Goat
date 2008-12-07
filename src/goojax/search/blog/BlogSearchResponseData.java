package goojax.search.blog;

import goojax.search.SearchResponseData;

public class BlogSearchResponseData extends SearchResponseData {
	public BlogSearchResult results[];
	
	public BlogSearchResponseData() {
		super();
	}

	public BlogSearchResult[] getResults() {
		return results;
	}

	public void setResults(BlogSearchResult[] results) {
		this.results = results;
	}
}
