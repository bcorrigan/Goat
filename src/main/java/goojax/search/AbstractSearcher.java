package goojax.search;

import static java.net.URLEncoder.encode;

import goojax.GooJAXFetcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Type;

import com.google.gson.Gson;

abstract public class AbstractSearcher<T extends AbstractSearchResult> extends GooJAXFetcher {
	
    // this is a very wrong way to do this sort of thing.
/*
    public enum SearchType {
		WEB (BASE_SEARCH_URL + "web", new TypeToken<SearchResponse<goojax.search.WebSearchResult>>(){}.getType()),
		LOCAL (BASE_SEARCH_URL + "local", new TypeToken<SearchResponse<goojax.search.LocalSearchResult>>(){}.getType()),
		VIDEO (BASE_SEARCH_URL + "video", new TypeToken<SearchResponse<goojax.search.VideoSearchResult>>(){}.getType()),
		
		NEWS (BASE_SEARCH_URL + "news", new TypeToken<SearchResponse<goojax.search.NewsSearchResult>>(){}.getType()),
		BOOKS (BASE_SEARCH_URL + "books", new TypeToken<SearchResponse<goojax.search.BookSearchResult>>(){}.getType()),
		IMAGES (BASE_SEARCH_URL + "images", new TypeToken<SearchResponse<goojax.search.ImageSearchResult>>(){}.getType()),
		PATENT (BASE_SEARCH_URL + "patent", new TypeToken<SearchResponse<goojax.search.PatentSearchResult>>(){}.getType());
			
		public final String baseUrl;
		public final Type responseType;
		
		SearchType(String url, Type type) {
			this.baseUrl = url;
			this.responseType = type;
		}
	}
	*/
	
	public enum SafeSearch {
		HIGH    ("active"),
		MEDIUM  ("moderate"),
		NONE    ("off");
		
		public final String urlCode;
		SafeSearch(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	

	public enum Scoring {
		DATE              ("d"),
		ASCENDING_DATE    ("ad"), // only works with patent search, according to googdocs
		DATE_NEWEST_FIRST ("d"),
		DATE_OLDEST_FIRST ("ad"); // only works with patent search, according to googdocs
		
		public final String urlCode;
		Scoring (String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	// protected boolean debug = false;

	
	protected ResultSize size = null;
	protected int start = 0;
	public static final String BASE_SEARCH_URL = BASE_GOOJAX_URL + "search/" ;
	
	public String encodeStandardOpts() {
		String superRet = super.encodeStandardOpts();
		ArrayList<String> tokes = new ArrayList<String>();
		try {
			if(size != null)
				tokes.add("rsz=" + encode(size.urlCode, encoding));
			if(start > 0)
				tokes.add("start=" + start);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return "";
		}
		String ret = "";
		Iterator<String> iter = tokes.iterator();
		if (iter.hasNext()) 
			ret += iter.next();
		while(iter.hasNext())
			ret += "&" + iter.next();
		if((!ret.equals("")) && (!superRet.equals("")) )
			ret += superRet + "&" + ret;
		else
			ret = superRet + ret;
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public SearchResponse<T> search() throws MalformedURLException, IOException, SocketTimeoutException {
		Gson gson = new Gson();
		URL url = getURL(getBaseUrl(), encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);
		
		return (SearchResponse<T>) gson.fromJson(goojax, getResponseType());
	}
	
	public SearchResponse<T> search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return search();
	}
	
	abstract public String encodeExtraSearchOpts();

	abstract public String getBaseUrl();

	// GSON can't cope with generics all that well, so you've got to give it an explicit response type
	abstract public Type getResponseType();
	
	public ResultSize getSize() {
		return size;
	}

	public void setSize(ResultSize size) {
		this.size = size;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

}


