package goojax.search;

import static java.net.URLEncoder.encode;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.reflect.TypeToken;

public class LocalSearcher extends AbstractSearcher<LocalSearchResult> {

	public Float latitude = 91f;
	public Float longitude = 181f;

	public Float boundingBoxLatitude = 0f;
	public Float boundingBoxLongitude = 0f;
	
	public ResultType resultType = null;
		
	public enum ResultType {
		BLENDED ("blended"),
		KMLONLY ("kmlonly"),
		LOCALONLY ("localonly");
		
		private String urlCode;
		ResultType(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();
		try {
			if((-180f <= longitude) && (longitude <= 180f) && (-90f <= latitude) && (latitude <= 90f))
				tokes.add("sll=" + encode(latitude + "," + longitude, encoding));
			if(boundingBoxLatitude > 0f || boundingBoxLongitude > 0f)
				tokes.add("sspn=" + encode(boundingBoxLatitude + "," + boundingBoxLongitude, encoding));  // see http://www.google.com/coop/docs/cse/resultsxml.html#languageCollections
			if(resultType != null)
				tokes.add("mrt=" + resultType.urlCode);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return "";
		}
		String ret = "";
		Iterator<String> iter = tokes.iterator();
		if(iter.hasNext())
			ret += iter.next();
		while(iter.hasNext())
			ret += "&" + iter.next();
		return ret;
	}

    public String getBaseUrl() {
        return BASE_SEARCH_URL + "local";
    }
    
    public Type getResponseType() {
        return new TypeToken<SearchResponse<goojax.search.LocalSearchResult>>(){}.getType();
    }
    
	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}

	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}

	public Float getBoundingBoxLatitude() {
		return boundingBoxLatitude;
	}

	public void setBoundingBoxLatitude(Float boundingBoxLatitude) {
		this.boundingBoxLatitude = boundingBoxLatitude;
	}

	public Float getBoundingBoxLongitude() {
		return boundingBoxLongitude;
	}

	public void setBoundingBoxLongitude(Float boundingBoxLongitude) {
		this.boundingBoxLongitude = boundingBoxLongitude;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}
}
