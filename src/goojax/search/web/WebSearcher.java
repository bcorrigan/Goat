package goojax.search.web;

import static java.net.URLEncoder.encode;
import goojax.search.AbstractSearcher;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public class WebSearcher extends AbstractSearcher {

	public SearchType getSearchType() {
		return SearchType.WEB;
	}

	public SafeSearch safeSearch = null;
	public String restrictLanguage = null;
	public String customSearchIdCode = null;
	public String customSearchReference = null;
	
	public WebSearcher() {
		super();
	}
	
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
