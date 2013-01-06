package goojax.search;

import static java.net.URLEncoder.encode;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.reflect.TypeToken;

public class VideoSearcher extends AbstractSearcher<VideoSearchResult> {

	public Scoring scoring = null;
		
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();

		try {
			if(scoring != null)
				tokes.add("scoring=" + encode(scoring.urlCode, encoding));
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
	
    public String getBaseUrl() {
        return BASE_SEARCH_URL + "video";
    }
    
    public Type getResponseType() {
        return new TypeToken<SearchResponse<goojax.search.VideoSearchResult>>(){}.getType();
    }
    
	public Scoring getScoring() {
		return scoring;
	}

	public void setScoring(Scoring scoring) {
		this.scoring = scoring;
	}
}
