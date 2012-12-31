package goojax.search.blog;

import goojax.search.AbstractSearcher;
import static java.net.URLEncoder.encode;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;


public class BlogSearcher extends AbstractSearcher<BlogSearchResponse> {

	public Scoring scoring = null;
	
	public BlogSearcher() {
		super();
	}
	
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

	public SearchType getSearchType() {
		return SearchType.BLOGS;
	}

	public Scoring getScoring() {
		return scoring;
	}

	public void setScoring(Scoring scoring) {
		this.scoring = scoring;
	}
}
