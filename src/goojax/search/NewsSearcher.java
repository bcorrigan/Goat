package goojax.search;

import static java.net.URLEncoder.encode;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.reflect.TypeToken;

public class NewsSearcher extends AbstractSearcher<NewsSearchResult> {
	
	public Scoring scoring = null;
	public String geo = null;
	public String quoteSourceId = null;
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();
		try {
			if(scoring != null)
				tokes.add("scoring=" + scoring.urlCode);
			if(geo != null && ! geo.matches("\\s*"))
				tokes.add("geo=" + encode(geo, encoding));
			if(quoteSourceId != null && ! quoteSourceId.matches("\\s*"))
				tokes.add("qsid=" + encode(quoteSourceId, encoding));
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

    public String getBaseUrl() {
        return BASE_SEARCH_URL + "news";
    }
    
    public Type getResponseType() {
        return new TypeToken<SearchResponse<goojax.search.NewsSearchResult>>(){}.getType();
    }
    
	public Scoring getScoring() {
		return scoring;
	}

	public void setScoring(Scoring scoring) {
		this.scoring = scoring;
	}

	public String getGeo() {
		return geo;
	}

	public void setGeo(String geo) {
		this.geo = geo;
	}

	public String getQuoteSourceId() {
		return quoteSourceId;
	}

	public void setQuoteSourceId(String quoteSourceId) {
		this.quoteSourceId = quoteSourceId;
	}
}
