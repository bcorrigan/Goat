package goojax.search.image;

import goojax.search.SearchResult;

public class ImageSearchResult extends SearchResult {

	public String visibleUrl;
	public String originalContextUrl;
	public Integer width;
	public Integer height;
	public Integer tbWidth;
	public Integer tbHeight;
	public String tbUrl;
	public String contentNoFormatting;
	
	ImageSearchResult() {
		super();
	}
}
