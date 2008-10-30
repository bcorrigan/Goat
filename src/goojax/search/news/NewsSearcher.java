package goojax.search.news;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
// used in debugging date format
// import java.util.Locale;
// import java.text.DateFormat;
// import java.text.SimpleDateFormat;
// import java.util.Date;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

public class NewsSearcher extends AbstractSearcher {
	
	protected Scoring scoring = null;
	protected String geo = null;
	protected String quoteSourceId = null;

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

	public NewsSearchResponse search() throws MalformedURLException, IOException, SocketTimeoutException {
		GsonBuilder gb = new GsonBuilder();
		String dateFormatString = "EEE, d MMM yyyy H:m:s Z";
		// debugging bit
		// SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString, Locale.US);
		// System.out.println(sdf.format(new Date()));
		gb.setDateFormat(dateFormatString);
		Gson gson = gb.create();
		URL url = getURL(getSearchType().baseUrl, encodeStandardOpts(), encodeExtraSearchOpts());
		String goojax = getGoojax(url);
		
		return gson.fromJson(goojax, NewsSearchResponse.class);
	}
	
	public NewsSearchResponse search(String query) throws MalformedURLException, IOException, SocketTimeoutException {
		this.query = query;
		return search();
	}
	
	public SearchType getSearchType() {
		return SearchType.NEWS;
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
