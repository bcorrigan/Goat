package goojax;

public abstract class GooJAXResponse {
	
	public int responseStatus;
	public String responseDetails;
	
	protected GooJAXResponse() {}
	
	public boolean statusNormal() {
		return responseStatus == 200;
	}

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public String getResponseDetails() {
		return responseDetails;
	}

	public void setResponseDetails(String responseDetails) {
		this.responseDetails = responseDetails;
	}
}
