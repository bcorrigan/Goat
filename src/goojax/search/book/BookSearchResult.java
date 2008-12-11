package goojax.search.book;

import goojax.search.SearchResult;

public class BookSearchResult extends SearchResult {
	public String authors;
	public String bookId;
	public String publishedYear;
	public Integer pageCount;
	public String thumbnailHtml;
	
	public BookSearchResult() {
		super();
	}
	
	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public String getBookId() {
		return bookId;
	}

	public void setBookId(String bookId) {
		this.bookId = bookId;
	}

	public Integer getPublishedYear() {
		Integer ret = null;
		if(! publishedYear.equalsIgnoreCase("unknown"))
			ret = Integer.parseInt(publishedYear);
		return ret;
	}

	public void setPublishedYear(Integer publishedYear) {
		this.publishedYear = publishedYear.toString();
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public String getThumbnailHtml() {
		return thumbnailHtml;
	}

	public void setThumbnailHtml(String thumbnailHtml) {
		this.thumbnailHtml = thumbnailHtml;
	}


}
