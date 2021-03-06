package goojax.search;


public class WebSearchResult extends AbstractSearchResult {

	public String cacheUrl;
	public String visibleUrl; // not documented in google's AJAX class reference, but sometimes present

	public WebSearchResult() {
		super();
	}
	
	public String getCacheUrl() {
		return cacheUrl;
	}

	public void setCacheUrl(String cacheUrl) {
		this.cacheUrl = cacheUrl;
	}

	public String getVisibleUrl() {
		return visibleUrl;
	}

	public void setVisibleUrl(String visibleUrl) {
		this.visibleUrl = visibleUrl;
	}

}
