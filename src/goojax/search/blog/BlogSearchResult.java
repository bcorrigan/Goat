package goojax.search.blog;

import goojax.search.SearchResult;

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

	public Date getPublishedDate() {
		return parseDate(publishedDate);
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = formatDate(publishedDate);
	}
}
