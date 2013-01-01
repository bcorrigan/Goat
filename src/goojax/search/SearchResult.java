package goojax.search;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/* this would make a fine inner class, if GSON played nice with inner classes */

public class SearchResult {
	public String GsearchResultClass;
	public String url;
	public String unescapedUrl;
	public String title;
	public String titleNoFormatting;
	public String content;
	
	final public static String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
	private SimpleDateFormat sdf = null;
	
	public SearchResult() {}
	
	public String getGsearchResultClass() {
		return GsearchResultClass;
	}

	public void setGsearchResultClass(String gsearchResultClass) {
		GsearchResultClass = gsearchResultClass;
	}

	public Date parseDate(String dateString) {
		Date ret = null;
		if(null == sdf)
			sdf = new SimpleDateFormat(DATE_FORMAT);
		try {
			ret = sdf.parse(dateString);
		} catch (ParseException pe) {
			pe.printStackTrace();
		} catch (NullPointerException npe) {
		    // dateString is null
		}
		return ret;
	}
	
	public String formatDate(Date date) {
		if(null == sdf)
			sdf = new SimpleDateFormat(DATE_FORMAT);
		return sdf.format(date);
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUnescapedUrl() {
		return unescapedUrl;
	}

	public void setUnescapedUrl(String unescapedUrl) {
		this.unescapedUrl = unescapedUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitleNoFormatting() {
		return titleNoFormatting;
	}

	public void setTitleNoFormatting(String titleNoFormatting) {
		this.titleNoFormatting = titleNoFormatting;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
