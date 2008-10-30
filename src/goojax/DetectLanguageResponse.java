package goojax;

public class DetectLanguageResponse extends GooJAXResponse {
	protected DetectLanguageResponseData responseData;
	protected DetectLanguageResponse() {
		super();
	}
	public DetectLanguageResponseData getResponseData() {
		return responseData;
	}
	public void setResponseData(DetectLanguageResponseData responseData) {
		this.responseData = responseData;
	}
}
