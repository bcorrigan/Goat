package goojax;

public class TranslateResponse extends GooJAXResponse {
	private TranslateResponseData responseData;
	TranslateResponse() {
		super();
	}
	public TranslateResponseData getResponseData() {
		return responseData;
	}
	public GooJAXFetcher.Language getDetectedSourceLanguage() {
		GooJAXFetcher.Language ret = null;
		if(null != responseData && null != responseData.getDetectedSourceLanguage() && ! "".equals(responseData.getDetectedSourceLanguage()))
			ret = GooJAXFetcher.Language.fromCode(responseData.getDetectedSourceLanguage());
		return ret;
	}
	public String getTranslatedText() {
		String ret = null;
		if(responseData != null && responseData.getTranslatedText() != null)
			ret = responseData.getTranslatedText();
		return ret;
	}
}
