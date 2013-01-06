package goojax.search;

import static java.net.URLEncoder.encode;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.reflect.TypeToken;


public class PatentSearcher extends AbstractSearcher<PatentSearchResult> {

	public boolean onlyFiled = false;
	public boolean onlyRegistered = false;
	public Scoring scoring = null;
		
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

    public String getBaseUrl() {
        return BASE_SEARCH_URL + "patent";
    }
    
    public Type getResponseType() {
        return new TypeToken<SearchResponse<goojax.search.PatentSearchResult>>(){}.getType();
    }
}
