package goojax;

import static java.net.URLEncoder.encode;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import com.google.gson.Gson;

public class Translator extends GooJAXFetcher {
	public static final String TRANSLATE_URL = BASE_GOOJAX_URL + "language/translate";
	public static final String DETECT_URL = BASE_GOOJAX_URL + "language/detect";
	public static final Language DEFAULT_DEFAULT_LANGUAGE = Language.EN;
	
	public enum Format {
		TEXT ("text"),
		HTML ("html");
		String urlCode;
		Format(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	protected Format format = null;
	protected Language fromLanguage = null;
	protected Language toLanguage = null;
	
	protected Language defaultLanguage = null;
	
	public Translator() {
		super();
	}
	
	public TranslateResponse translate() throws IOException, SocketTimeoutException {
		if(null == toLanguage)
			toLanguage = defaultLanguage;
		String to = (null == toLanguage? DEFAULT_DEFAULT_LANGUAGE.code : toLanguage.code);
		String from = (null == fromLanguage? "" : fromLanguage.code);
		String opts = "";
		try {
			opts = "langpair=" + encode(from + "|" + to,encoding);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return null;
		}
		if(format != null)
			opts += "&format=" + format.urlCode;
		String json = getGoojax(TRANSLATE_URL, opts);
		Gson gson = new Gson();
		return gson.fromJson(json, TranslateResponse.class);
	}
	
	public TranslateResponse translate(String text) throws IOException, SocketTimeoutException {
		this.query = text;
		return translate();
	}
	
	public TranslateResponse translate(String text, Language fromLanguage, Language toLanguage) throws IOException, SocketTimeoutException {
		this.query = text;
		this.fromLanguage = fromLanguage;
		this.toLanguage = toLanguage;
		return translate();
	}
	
	public DetectLanguageResponse detect() throws IOException, SocketTimeoutException {
		String json = getGoojax(DETECT_URL, "");
		Gson gson = new Gson();
		return gson.fromJson(json, DetectLanguageResponse.class);
	}
	
	public DetectLanguageResponse detect(String text) throws IOException, SocketTimeoutException {
		this.query = text;
		return detect();
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public Language getFromLanguage() {
		return fromLanguage;
	}

	public void setFromLanguage(Language fromLanguage) {
		this.fromLanguage = fromLanguage;
	}

	public Language getToLanguage() {
		return toLanguage;
	}

	public void setToLanguage(Language toLanguage) {
		this.toLanguage = toLanguage;
	}

	public Language getDefaultLanguage() {
		return defaultLanguage;
	}

	public void setDefaultLanguage(Language defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}
	
}
