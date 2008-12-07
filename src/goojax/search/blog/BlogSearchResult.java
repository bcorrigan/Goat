package goojax.search.blog;

import goojax.search.SearchResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlogSearchResult extends SearchResult {
	public String postUrl;
	public String author;
	public String blogUrl;
	public String publishedDate;

	public BlogSearchResult() {
		super();
	}

	public String getPostUrl() {
		return postUrl;
	}

	public void setPostUrl(String postUrl) {
		this.postUrl = postUrl;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getBlogUrl() {
		return blogUrl;
	}

	public void setBlogUrl(String blogUrl) {
		this.blogUrl = blogUrl;
	}

	final static String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
	final static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

	public Date getPublishedDate() {
		Date ret = null;
		if(publishedDate != null)
			try {
				ret = sdf.parse(publishedDate);
			} catch (ParseException pe) {
				pe.printStackTrace();
			}
			return ret;
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = sdf.format(publishedDate);
	}
}
