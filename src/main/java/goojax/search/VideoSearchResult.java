package goojax.search;


import java.util.Date;

public class VideoSearchResult extends AbstractSearchResult {
	public String published;
	public String publisher;
	public Integer duration;
	public Integer tbWidth;
	public Integer tbHeight;
	public String tbUrl;
	public String playUrl;
	public String author;
	public String viewCount;
	public Float rating;
	
	public VideoSearchResult() {
		super();
	}

	public Date getPublished() {
		Date ret = null;
		try {
			ret = parseDate(published);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public void setPublished(Date published) {
		this.published = formatDate(published);
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	
	public String getDurationString() {
		String ret = "";
		Integer minutes, seconds, hours;
		seconds = duration % 60;
		minutes = duration / 60;
		hours = minutes / 60;
		minutes = minutes % 60;
		ret = seconds.toString();
		if(duration >= 60)
			ret = minutes + ":" + ret;
		if(duration >= 60*60)
			ret = hours + ":" + ret;
		return ret;
	}

	public Integer getTbWidth() {
		return tbWidth;
	}

	public void setTbWidth(Integer tbWidth) {
		this.tbWidth = tbWidth;
	}

	public Integer getTbHeight() {
		return tbHeight;
	}

	public void setTbHeight(Integer tbHeight) {
		this.tbHeight = tbHeight;
	}

	public String getTbUrl() {
		return tbUrl;
	}

	public void setTbUrl(String tbUrl) {
		this.tbUrl = tbUrl;
	}

	public String getPlayUrl() {
		return playUrl;
	}

	public void setPlayUrl(String playUrl) {
		this.playUrl = playUrl;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getViewCount() {
		return viewCount;
	}

	public void setViewCount(String viewCount) {
		this.viewCount = viewCount;
	}

	public Float getRating() {
		return rating;
	}

	public void setRating(Float rating) {
		this.rating = rating;
	}
	
}
