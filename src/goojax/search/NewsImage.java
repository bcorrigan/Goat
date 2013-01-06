package goojax.search;

public class NewsImage {
	public String url;
	public String originalContextUrl;
	public String publisher;
	public Integer tbHeight;
	public Integer tbWidth;
	public String tbUrl;
	
	public NewsImage() {}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getOriginalContextUrl() {
		return originalContextUrl;
	}
	public void setOriginalContextUrl(String originalContextUrl) {
		this.originalContextUrl = originalContextUrl;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public int getTbHeight() {
		return tbHeight;
	}
	public void setTbHeight(int tbHeight) {
		this.tbHeight = tbHeight;
	}
	public int getTbWidth() {
		return tbWidth;
	}
	public void setTbWidth(int tbWidth) {
		this.tbWidth = tbWidth;
	}
	public String getTbUrl() {
		return tbUrl;
	}
	public void setTbUrl(String tbUrl) {
		this.tbUrl = tbUrl;
	}
}
