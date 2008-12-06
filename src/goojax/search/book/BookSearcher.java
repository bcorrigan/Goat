package goojax.search.book;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;
import goojax.search.AbstractSearcher.SearchType;
import goojax.search.web.WebSearchResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;

public class BookSearcher extends AbstractSearcher {

	protected boolean fullViewOnly = false;
	protected String libraryName = null;
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();

		try {
			if(fullViewOnly)
				tokes.add("as_brr=1");
			if(libraryName != null && ! libraryName.matches("\\s*"))
				tokes.add("as_list=" + encode(libraryName, encoding));
				/* add stuff here if google's video search ever gets more sophisticated */
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

	public BookSearchResponse search() throws MalformedURLException, IOException, SocketTimeoutException {
		Gson gson = new Gson();
		URL url = getURL(getSearchType().baseUrl, encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);

		return gson.fromJson(goojax, BookSearchResponse.class);
	}
	
	public BookSearchResponse search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return search();
	}
	
	public SearchType getSearchType() {
		return SearchType.BOOKS;
	}

	public boolean isFullViewOnly() {
		return fullViewOnly;
	}

	public void setFullViewOnly(boolean fullViewOnly) {
		this.fullViewOnly = fullViewOnly;
	}

	public String getLibraryName() {
		return libraryName;
	}

	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}
}
