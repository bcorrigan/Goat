package goojax.search;

import static java.net.URLEncoder.encode;

import goojax.GooJAXFetcher;
import goojax.search.blog.BlogSearchResponse;
import goojax.search.book.BookSearchResponse;
import goojax.search.image.ImageSearchResponse;
import goojax.search.local.LocalSearchResponse;
import goojax.search.news.NewsSearchResponse;
import goojax.search.patent.PatentSearchResponse;
import goojax.search.video.VideoSearchResponse;
import goojax.search.web.WebSearchResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;

public abstract class AbstractSearcher extends GooJAXFetcher {
	
	public enum SearchType {
		WEB (BASE_SEARCH_URL + "web", WebSearchResponse.class),
		LOCAL (BASE_SEARCH_URL + "local", LocalSearchResponse.class),
		VIDEO (BASE_SEARCH_URL + "video", VideoSearchResponse.class),
		BLOGS (BASE_SEARCH_URL + "blogs", BlogSearchResponse.class),
		NEWS (BASE_SEARCH_URL + "news", NewsSearchResponse.class),
		BOOKS (BASE_SEARCH_URL + "books", BookSearchResponse.class),
		IMAGES (BASE_SEARCH_URL + "images", ImageSearchResponse.class),
		PATENT (BASE_SEARCH_URL + "patent", PatentSearchResponse.class);
			
		public final String baseUrl;
		public final Class<? extends SearchResponse> responseClass;
		
		SearchType(String url, Class<? extends SearchResponse> responseClass) {
			this.baseUrl = url;
			this.responseClass = responseClass;
		}
	}
	
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
	public <T extends SearchResponse> T search() throws MalformedURLException, IOException, SocketTimeoutException {
		Gson gson = new Gson();
		URL url = getURL(getSearchType().baseUrl, encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);
		T ret = (T) gson.fromJson(goojax, getSearchType().responseClass);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends SearchResponse> T search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return (T) search();
	}
	
	abstract public String encodeExtraSearchOpts();

	abstract public SearchType getSearchType();

	
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


