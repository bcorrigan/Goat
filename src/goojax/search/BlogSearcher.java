package goojax.search;

import static java.net.URLEncoder.encode;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


public class BlogSearcher extends AbstractSearcher<BlogSearchResult> {

	public Scoring scoring = null;
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();

		try {
			if(scoring != null)
				tokes.add("scoring=" + encode(scoring.urlCode, encoding));
				/* add stuff here if google's blog search ever gets more sophisticated */
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
		return ret;	}

	public String getBaseUrl() {
	    return BASE_SEARCH_URL + "blogs";
	}
	
	public Type getResponseType() {
	    return new TypeToken<SearchResponse<goojax.search.BlogSearchResult>>(){}.getType();
	}
	
	public Scoring getScoring() {
		return scoring;
	}

	public void setScoring(Scoring scoring) {
		this.scoring = scoring;
	}
}
