package goojax.search.web;

import goojax.search.SearchResult;

public class WebSearchResult extends SearchResult {

	String cacheUrl;
	String visibleUrl; // not documented in google's AJAX class reference, but sometimes present

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
