package goojax;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import static java.net.URLEncoder.encode;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

//uncomment (and make sure you've got the org.json.* libs available) for debugging
//import org.json.JSONException;
//import org.json.JSONObject;

public abstract class GooJAXFetcher {
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	public static final String BASE_GOOJAX_URL = "http://ajax.googleapis.com/ajax/services/" ;
	public static final String GOOJAX_VERSION = "1.0";
	
	protected String encoding = defaultEncoding;
	protected int connectTimeout = defaultConnectTimeout;
	protected int readTimeout = defaultReadTimeout;	
	
	protected String query = null;
	protected String version = "1.0";
	protected Language hostLanguage = null;
	protected String key = defaultKey;
	protected String httpReferer = defaultHttpReferrer;
	
	private static String defaultEncoding = DEFAULT_ENCODING;
	private static int defaultConnectTimeout = 0;
	private static int defaultReadTimeout = 0;
	private static String defaultKey = null;
	private static String defaultHttpReferrer = null;
	
	
	public enum ResultSize {
		SMALL ("small"), // small, or 4 results
		LARGE ("large"); // large, or 8 results
		
		public final String urlCode;
		ResultSize(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	public enum Language {
		AF ("af", "afrikaans"),
		SQ ("sq", "albanian"),
		AM ("am", "amharic"),
		AR ("ar", "arabic", true),
		HY ("hy", "armenian"),
		AZ ("az", "azerbaijani"),
		EU ("eu", "basque"),
		BE ("be", "belarusian"),
		BN ("bn", "bengali"),
		BH ("bh", "bihari"),
		BG ("bg", "bulgarian", true),
		MY ("my", "burmese"),
		CA ("ca", "catalan", true),
		CHR ("chr", "cherokee"),
		ZH ("zh", "chinese", true),
		ZHCN ("zh-CN", "chinese_simplified", true),
		ZHTW ("zh-TW", "chinese_traditional", true),
		HR ("hr", "croatian", true),
		CS ("cs", "czech", true),
		DA ("da", "danish", true),
		DV ("dv", "dhivehi"),
		NL ("nl", "dutch", true),
		EN ("en", "english", true),
		EO ("eo", "esperanto"),
		ET ("et", "estonian"),
		FI ("fi", "finnish", true),
		FR ("fr", "french", true),
		GL ("gl", "galician"),
		KA ("ka", "georgian"),
		DE ("de", "german", true),
		EL ("el", "greek", true),
		GN ("gn", "guarani"),
		GU ("gu", "gujarati"),
		IW ("iw", "hebrew", true),
		HI ("hi", "hindi", true),
		HU ("hu", "hungarian"),
		IS ("is", "icelandic"),
		ID ("id", "indonesian", true),
		IU ("iu", "inuktitut"),
		IT ("it", "italian", true),
		JA ("ja", "japanese", true),
		KN ("kn", "kannada"),
		KK ("kk", "kazakh"),
		KM ("km", "khmer"),
		KO ("ko", "korean", true),
		KU ("ku", "kurdish"),
		KY ("ky", "kyrgyz"),
		LO ("lo", "laothian"),
		LV ("lv", "latvian", true),
		LT ("lt", "lithuanian", true),
		MK ("mk", "macedonian"),
		MS ("ms", "malay"),
		ML ("ml", "malayalam"),
		MT ("mt", "maltese"),
		MR ("mr", "marathi"),
		MN ("mn", "mongolian"),
		NE ("ne", "nepali"),
		NO ("no", "norwegian", true),
		OR ("or", "oriya"),
		PS ("ps", "pashto"),
		FA ("fa", "persian"),
		PL ("pl", "polish", true),
		PTPT ("pt-PT", "portuguese", true),
		PA ("pa", "punjabi"),
		RO ("ro", "romanian", true),
		RU ("ru", "russian", true),
		SA ("sa", "sanskrit"),
		SR ("sr", "serbian", true),
		SD ("sd", "sindhi"),
		SI ("si", "sinhalese"),
		SK ("sk", "slovak", true),
		SL ("sl", "slovenian", true),
		ES ("es", "spanish", true),
		SW ("sw", "swahili"),
		SV ("sv", "swedish", true),
		TG ("tg", "tajik"),
		TA ("ta", "tamil"),
		TL ("tl", "tagalog", true),
		TE ("te", "telugu"),
		TH ("th", "thai"),
		BO ("bo", "tibetan"),
		TR ("tr", "turkish"),
		UK ("uk", "ukrainian", true),
		UR ("ur", "urdu"),
		UZ ("uz", "uzbek"),
		UG ("ug", "uighur"),
		VI ("vi", "vietnamese", true);
		
		String code;
		String englishName;
		boolean isTranslateable = false;
		
		Language(String urlCode, String name) {
			this.code = urlCode;
			this.englishName = name;
		}
		
		Language(String urlCode, String name, boolean isTranslateable) {
			this.code = urlCode;
			this.englishName = name;
			this.isTranslateable = isTranslateable;
		}
		
		public static Language fromCode(String code) {
			for(Language lang : Language.values()) {
				if(lang.code.equalsIgnoreCase(code))
					return lang;
			}
			return null;
		}
		public static Language fromEnglishName(String englishName) {
			for(Language lang : Language.values()) {
				if(lang.englishName.equalsIgnoreCase(englishName))
					return lang;
			}
			return null;
		}
		public String getCode() {
			return code;
		}
		public String getEnglishName() {
			return englishName;
		}

		public boolean isTranslateable() {
			return isTranslateable;
		}
	}
	
	public String encodeStandardOpts () {
		ArrayList<String> tokes = new ArrayList<String>();
		try {
			if(query != null && ! query.matches("\\s*"))
				tokes.add("q=" + encode(query, encoding));
			if(version != null && ! version.matches("\\s*"))
				tokes.add("v=" + encode(version, encoding));
			else
				tokes.add("v=" + GOOJAX_VERSION);
			if(hostLanguage != null)
				tokes.add("hl=" + hostLanguage.code);
			if(key != null && ! key.matches("\\s*"))
				tokes.add("key=" + encode(key, encoding));
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return "";
		}
		String ret = "";
		Iterator<String> iter = tokes.iterator();
		if (iter.hasNext()) 
			ret += iter.next();
		while(iter.hasNext())
			ret += "&" + iter.next();
		return ret;
	}

	public URL getURL(String baseUrl, String standardOpts, String otherOpts) throws MalformedURLException {
		String queryString = "";
		boolean haveStand = (standardOpts != null && ! standardOpts.matches("\\s*"));
		boolean haveOther = (otherOpts != null && ! otherOpts.matches("\\s*"));
		if(haveStand || haveOther)
			queryString += "?";
		if(haveStand)
			queryString += standardOpts;
		if(haveStand && haveOther)
			queryString += "&";
		if(haveOther)
			queryString += otherOpts;
		return new URL(baseUrl + queryString);			
	}
	
	public String getGoojax(URL url) throws IOException, SocketTimeoutException {
		URLConnection connection = url.openConnection();
		connection.addRequestProperty("Referer", (null == httpReferer ? "" : httpReferer));
		connection.setReadTimeout(readTimeout); 
		connection.setConnectTimeout(connectTimeout);

		String line;
		StringBuilder builder = new StringBuilder();
		
		connection.connect();  // do this explicitly so we can see where it happens in our logic
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
		while((line = reader.readLine()) != null) {
			builder.append(line);
		}
		String ret = builder.toString();
		
		/* uncomment to see all JSON on the console for debugging
		try {
			JSONObject job = new JSONObject(ret);
			System.out.println(job.toString(3));
		} catch (JSONException je) {
			je.printStackTrace();
		}
		*/
		
		return ret;
	}
	
	public String getGoojax(String baseUrl, String nonStandardOptions) throws IOException, SocketTimeoutException {
		return getGoojax(getURL(baseUrl, encodeStandardOpts(), nonStandardOptions));
	}
	
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	public Language getHostLanguage() {
		return hostLanguage;
	}

	public void setHostLanguage(Language hostLanguage) {
		this.hostLanguage = hostLanguage;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public String getHttpReferrer() {
		return httpReferer;
	}

	public void setHttpReferrer(String httpReferrer) {
		this.httpReferer = httpReferrer;
	}

	public static String getDefaultEncoding() {
		return defaultEncoding;
	}

	public static void setDefaultEncoding(String defaultEncoding) {
		GooJAXFetcher.defaultEncoding = defaultEncoding;
	}

	public static int getDefaultConnectTimeout() {
		return defaultConnectTimeout;
	}

	public static void setDefaultConnectTimeout(int defaultConnectTimeout) {
		GooJAXFetcher.defaultConnectTimeout = defaultConnectTimeout;
	}

	public static int getDefaultReadTimeout() {
		return defaultReadTimeout;
	}

	public static void setDefaultReadTimeout(int defaultReadTimeout) {
		GooJAXFetcher.defaultReadTimeout = defaultReadTimeout;
	}

	public static void setDefaultTimeout(int defaultTimeout) {
		GooJAXFetcher.defaultReadTimeout = defaultTimeout;
		GooJAXFetcher.defaultConnectTimeout = defaultTimeout;
	}

	public static String getDefaultKey() {
		return defaultKey;
	}

	public static void setDefaultKey(String defaultKey) {
		GooJAXFetcher.defaultKey = defaultKey;
	}

	public static String getDefaultHttpReferrer() {
		return defaultHttpReferrer;
	}

	public static void setDefaultHttpReferrer(String defaultHttpReferrer) {
		GooJAXFetcher.defaultHttpReferrer = defaultHttpReferrer;
	}
}
