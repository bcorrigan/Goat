package goojax;

import goojax.GooJAXFetcher.Language;

public class DetectLanguageResponseData {
	protected String language;
	protected boolean isReliable;
	protected Float confidence;
	
	DetectLanguageResponseData() {}
	
	public Language getLanguage() {
		return Language.fromCode(language);
	}
	public void setLanguage(Language language) {
		this.language = language.code;
	}
	public boolean isReliable() {
		return isReliable;
	}
	public void setReliable(boolean isReliable) {
		this.isReliable = isReliable;
	}
	public Float getConfidence() {
		return confidence;
	}
	public void setConfidence(Float confidence) {
		this.confidence = confidence;
	}
}
