package goojax.search.web;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;

public class WebSearcher extends AbstractSearcher {

	public SearchType getSearchType() {
		// TODO Auto-generated method stub
		return SearchType.WEB;
	}

	protected SafeSearch safeSearch = null;
	protected String restrictLanguage = null;
	protected String customSearchIdCode = null;
	protected String customSearchReference = null;
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();
		try {
			if(safeSearch != null)
				tokes.add("safe=" + safeSearch.urlCode);
			if(restrictLanguage != null && ! restrictLanguage.matches("\\s*"))
				tokes.add("lr=" + encode(restrictLanguage, encoding));  // see http://www.google.com/coop/docs/cse/resultsxml.html#languageCollections
			if(customSearchIdCode != null && ! customSearchIdCode.matches("\\s*"))
				tokes.add("cx=" + encode(customSearchIdCode, encoding));
			if(customSearchReference != null && ! customSearchReference.matches("\\s*"))
				tokes.add("cref=" + encode(customSearchReference, encoding));
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return "";
		}
		String ret = "";
		Iterator<String> iter = tokes.iterator();
		if(iter.hasNext())
			ret += iter.next();
		while(iter.hasNext())
			ret += "&" + iter.next();
		return ret;
	}

	public WebSearchResponse search() throws MalformedURLException, IOException, SocketTimeoutException {
		Gson gson = new Gson();
		URL url = getURL(getSearchType().baseUrl, encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);

		return gson.fromJson(goojax, WebSearchResponse.class);
	}
	
	public WebSearchResponse search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return search();
	}
	
	public SafeSearch getSafeSearch() {
		return safeSearch;
	}

	public void setSafeSearch(SafeSearch safeSearch) {
		this.safeSearch = safeSearch;
	}

	public String getRestrictLanguage() {
		return restrictLanguage;
	}

	public void setRestrictLanguage(String restrictLanguage) {
		this.restrictLanguage = restrictLanguage;
	}

	public String getCustomSearchIdCode() {
		return customSearchIdCode;
	}

	public void setCustomSearchIdCode(String customSearchIdCode) {
		this.customSearchIdCode = customSearchIdCode;
	}

	public String getCustomSearchReference() {
		return customSearchReference;
	}

	public void setCustomSearchReference(String customSearchReference) {
		this.customSearchReference = customSearchReference;
	}
	
}
