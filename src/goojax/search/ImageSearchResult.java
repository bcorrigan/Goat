package goojax.search;


public class ImageSearchResult extends AbstractSearchResult {

	public String visibleUrl;
	public String originalContextUrl;
	public Integer width;
	public Integer height;
	public Integer tbWidth;
	public Integer tbHeight;
	public String tbUrl;
	public String contentNoFormatting;
	
	public ImageSearchResult() {
		super();
	}

	public String getVisibleUrl() {
		return visibleUrl;
	}

	public void setVisibleUrl(String visibleUrl) {
		this.visibleUrl = visibleUrl;
	}

	public String getOriginalContextUrl() {
		return originalContextUrl;
	}

	public void setOriginalContextUrl(String originalContextUrl) {
		this.originalContextUrl = originalContextUrl;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
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

	public String getContentNoFormatting() {
		return contentNoFormatting;
	}

	public void setContentNoFormatting(String contentNoFormatting) {
		this.contentNoFormatting = contentNoFormatting;
	}
}
