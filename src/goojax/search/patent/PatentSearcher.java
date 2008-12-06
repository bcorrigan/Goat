package goojax.search.patent;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;
import goojax.search.AbstractSearcher.Scoring;
import goojax.search.AbstractSearcher.SearchType;
import goojax.search.book.BookSearchResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;

public class PatentSearcher extends AbstractSearcher {

	protected boolean onlyFiled = false;
	protected boolean onlyRegistered = false;
	protected Scoring scoring = null;
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();

		try {
			if(scoring != null)
				tokes.add("scoring=" + encode(scoring.urlCode, encoding));
			if(onlyFiled && ! onlyRegistered)
				tokes.add("as_psrg=1");
			if(onlyRegistered && ! onlyFiled)
				tokes.add("as_psra=1");
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
		return ret;		}

	public SearchType getSearchType() {
		return SearchType.PATENT;
	}

	public PatentSearchResponse search() throws MalformedURLException, IOException, SocketTimeoutException {
		Gson gson = new Gson();
		URL url = getURL(getSearchType().baseUrl, encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);

		return gson.fromJson(goojax, PatentSearchResponse.class);
	}
	
	public PatentSearchResponse search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return search();
	}

}
