package goojax.search;

import goojax.Cursor;

public class SearchResponseData<T extends AbstractSearchResult> {
    
    public Cursor cursor;
    public T results[];

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
