package goojax.search.video;

import goojax.search.SearchResult;
import java.util.Date;

public class VideoSearchResult extends SearchResult {
	public Date published;
	public String publisher;
	public Integer duration;
	public Integer tbWidth;
	public Integer tbHeight;
	public String tbUrl;
	public String playUrl;
	public String author;
	public String viewCount;
	public Float rating;
	
	VideoSearchResult() {
		super();
	}
}
