package goojax.search.patent;

import static java.net.URLEncoder.encode;
import goojax.search.AbstractSearcher;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;


public class PatentSearcher extends AbstractSearcher<PatentSearchResponse> {

	public boolean onlyFiled = false;
	public boolean onlyRegistered = false;
	public Scoring scoring = null;
	
	public PatentSearcher() {
		super();
	}
	
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

}
