package goojax;

public class TranslateResponseData {
	protected String translatedText;
	protected String detectedSourceLanguage;
	
	TranslateResponseData() {}

	public String getTranslatedText() {
		return translatedText;
	}

	public void setTranslatedText(String translatedText) {
		this.translatedText = translatedText;
	}

	public String getDetectedSourceLanguage() {
		return detectedSourceLanguage;
	}

	public void setDetectedSourceLanguage(String detectedSourceLanguage) {
		this.detectedSourceLanguage = detectedSourceLanguage;
	}
}
