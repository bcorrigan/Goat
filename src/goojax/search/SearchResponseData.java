package goojax.search;

import goojax.Cursor;

/* There's really no need for this to be sitting in a file of its own, 
 * apart from GSON's inability to deal with inner classes.
 * 
 *  Similarly, there's no reason this shouldn't be an abstract
 *  class, apart from GSON's tendency to choke on them */

public class SearchResponseData<T extends SearchResult> {

	
	public Cursor cursor;
	public T results[];

	// public SearchResponseData() {}

	public Cursor getCursor() {
		return cursor;
	}

	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
	}
	
	public T[] getResults() {
		return results;
	}

	public void setResults(T[] results) {
		this.results = results;
	}

}
