package goojax.search.blog;

import goojax.search.SearchResult;
import java.util.Date;

public class BlogSearchResult extends SearchResult {
	public String postUrl;
	public String author;
	public String blogUrl;
	public Date publishedDate;
	
	public BlogSearchResult() {
		super();
	}
}
