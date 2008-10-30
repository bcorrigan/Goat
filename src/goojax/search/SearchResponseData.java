package goojax.search;

import goojax.Cursor;

/* There's really no need for this to be sitting in a file of its own, apart from GSON's inability to deal with inner classes */

public class SearchResponseData {

	protected Cursor cursor;
	protected SearchResult results[];

	protected SearchResponseData() {}

	public Cursor getCursor() {
		return cursor;
	}

	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
	}

	public SearchResult[] getResults() {
		return results;
	}

	public void setResults(SearchResult[] results) {
		this.results = results;
	}

}
