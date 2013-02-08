package goojax.search;

import goojax.GooJAXFetcher.Language;

import java.util.Date;

public class NewsSearchResult extends AbstractSearchResult {
	
	public String clusterUrl;
	public NewsImage image;
	public String language;
	public String location;
	public String publishedDate;
	public String publisher;
	public String signedRedirectUrl;
	public NewsSearchResult relatedStories[];
	
	public String author; //used only for searches where qsid= is used, result is person quoted.
		
	public NewsSearchResult() { super(); }

	public String getClusterUrl() {
		return clusterUrl;
	}

	public void setClusterUrl(String clusterUrl) {
		this.clusterUrl = clusterUrl;
	}

	public NewsImage getImage() {
		return image;
	}

	public void setImage(NewsImage image) {
		this.image = image;
	}

	public Language getLanguage() {
		return Language.fromCode(language);
	}

	public void setLanguage(Language language) {
		this.language = language.getCode();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Date getPublishedDate() {
		return parseDate(publishedDate);
	}

	public void setPublishedDate(Date publishedDate) {
		this.publishedDate = formatDate(publishedDate);
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getSignedRedirectUrl() {
		return signedRedirectUrl;
	}

	public void setSignedRedirectUrl(String signedRedirectUrl) {
		this.signedRedirectUrl = signedRedirectUrl;
	}

	public NewsSearchResult[] getRelatedStories() {
		return relatedStories;
	}

	public void setRelatedStories(NewsSearchResult[] relatedStories) {
		this.relatedStories = relatedStories;
	}
}
