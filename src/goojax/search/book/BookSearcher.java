package goojax.search.book;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;
import goojax.search.AbstractSearcher.SearchType;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

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
