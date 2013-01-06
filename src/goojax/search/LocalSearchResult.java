package goojax.search;


public class LocalSearchResult extends AbstractSearchResult {
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
	public String listingType;
	
	public enum ListingType {
		KML ("kml"),
		LOCAL ("local");
		
		String code;
		ListingType(String code) {
			this.code = code;
		}
		public static ListingType fromCode(String code) {
			ListingType ret = null;
			for(ListingType type: ListingType.values())
				if(type.code.equalsIgnoreCase(code)) {
					ret = type;
					break;
				}
			return ret;
		}
	}
	
	public LocalSearchResult() {
		super();
	}

	public Float getLat() {
		return lat;
	}

	public void setLat(Float lat) {
		this.lat = lat;
	}

	public Float getLng() {
		return lng;
	}

	public void setLng(Float lng) {
		this.lng = lng;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public PhoneNumber[] getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(PhoneNumber[] phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public String getDdurl() {
		return ddurl;
	}

	public void setDdurl(String ddurl) {
		this.ddurl = ddurl;
	}

	public String getDdurlToHere() {
		return ddurlToHere;
	}

	public void setDdurlToHere(String ddurlToHere) {
		this.ddurlToHere = ddurlToHere;
	}

	public String getDdurlFromHere() {
		return ddurlFromHere;
	}

	public void setDdurlFromHere(String ddurlFromHere) {
		this.ddurlFromHere = ddurlFromHere;
	}

	public String getStaticMapUrl() {
		return staticMapUrl;
	}

	public void setStaticMapUrl(String staticMapUrl) {
		this.staticMapUrl = staticMapUrl;
	}

	public ListingType getListingType() {
		return ListingType.fromCode(listingType);
	}

	public void setListingType(ListingType listingType) {
		this.listingType = listingType.code;
	}
	
}
