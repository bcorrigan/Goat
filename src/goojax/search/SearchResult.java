package goojax.search;

/* this would make a fine inner class, if GSON played nice with inner classes */

public class SearchResult {
	String GsearchResultClass;
	String url;
	String unescapedUrl;
	String title;
	String titleNoFormatting;
	String content;
	
	protected SearchResult() {}

	public String getGsearchResultClass() {
		return GsearchResultClass;
	}

	public void setGsearchResultClass(String gsearchResultClass) {
		GsearchResultClass = gsearchResultClass;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUnescapedUrl() {
		return unescapedUrl;
	}

	public void setUnescapedUrl(String unescapedUrl) {
		this.unescapedUrl = unescapedUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitleNoFormatting() {
		return titleNoFormatting;
	}

	public void setTitleNoFormatting(String titleNoFormatting) {
		this.titleNoFormatting = titleNoFormatting;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
