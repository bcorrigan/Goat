package goojax.search.local;

import goojax.search.SearchResult;

public class LocalSearchResult extends SearchResult {
	public Float lat;
	public Float lng;
	public String streetAddress;
	public String city;
	public String region;
	public String country;
	public PhoneNumber phoneNumbers[];
	public String ddurl;
	public String ddurlToHere;
	public String ddurlFromHere;
	public String staticMapUrl;
	public ListingType listingType;
	
	public enum ListingType {
		KML ("kml"),
		LOCAL ("local");
		
		String code;
		ListingType(String code) {
			this.code = code;
		}
	}
	
	LocalSearchResult() {
		super();
	}
}
