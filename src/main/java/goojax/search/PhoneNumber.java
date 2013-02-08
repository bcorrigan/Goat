package goojax.search;

public class PhoneNumber {
	public String number;
	public String type;
	
	public PhoneNumber() {}
	
	public enum PhoneNumberType {
		MAIN	("main"),
		FAX		("fax"),
		MOBILE	("mobile"),
		DATA	("data"),
		NONE	("");
		
		String code;
		
		PhoneNumberType(String type) {
			this.code = type;
		}
		
		public static PhoneNumberType typeFromString(String string) {
			PhoneNumberType ret = null;
			for (PhoneNumberType type: PhoneNumberType.values())
				if(type.code.equalsIgnoreCase(string)) {
					ret = type;
					break;
				}
			return ret;
		}
	}
	
	public PhoneNumberType getType() {
		return PhoneNumberType.typeFromString(type);
	}
	
	public void setType(PhoneNumberType phoneNumberType) {
		this.type = phoneNumberType.code;
	}
}
