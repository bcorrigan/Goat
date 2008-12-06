package goat.module;

import java.lang.Math ;
// import java.util.Random ;
//import java.net.URL ;
//import java.net.MalformedURLException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import goat.core.Message ;
import goat.core.Module ;
import goat.util.HTMLUtil;
import goojax.*;
import goojax.search.SearchResponse;
import goojax.search.SearchResult;
import goojax.search.news.*;
import goojax.search.book.*;
import goojax.search.patent.*;
import goojax.search.patent.PatentSearchResult.PatentStatus;
import goojax.search.web.WebSearcher;

/**
 * Module to ask google about stuff.
 * 
 * @author encontrado
 *         Created on 27-Feb-2006
 */
public class Google extends Module {

	private final GooJAXFetcher.Language DEFAULT_GOAT_LANGUAGE = GooJAXFetcher.Language.EN;

	public static final String encoding = "UTF-8";
	public static final String noResultString = "No results found" ;
	//	private static Random random = new Random() ;

	/* IRC methods
	 */

	public Google() {
		GooJAXFetcher.setDefaultTimeout(3000);
		GooJAXFetcher.setDefaultHttpReferrer("http://goat-blog.blogspot.com/");
		GooJAXFetcher.setDefaultKey("ABQIAAAA3SYwJ1rsiLgTvuisAwhOWBSj2h-HwVayfKLTNoeW4qFtyKpsrhSAZlVe3nAKyDZbufib0rUbOQ-MvA");
	}

	public static String[] getCommands() {
		return new String[]{
				"google", "goatle", 
				"googlefight", 
				"pornometer", "pronometer", "pr0nometer",
				"sexiness", 
				"gis", 
				"yis", 
				"wikipedia", 
				"youtube", 
				"imdb",
				"gayness", 
				"flickr", 
				"gnews", "googlenews", 
				"newslink", "nlink", 
				"translate",
				"detectlanguage", "languagedetect", "detectlang", "langdetect", 
				"languages",
				"googlebooks", "booksgoogle", "bookgoogle", "booksearch",
				"booklink",
				"patentsearch", "patentgoogle", "googlepatents",
				"patentlink", "plink",
		"glink"};
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		//debug
		//System.out.println("PROCESSING:  " + m.modCommand) ;
		m.removeFormattingAndColors() ;
		try {
			if ("google".equalsIgnoreCase(m.modCommand) || 
					"goatle".equalsIgnoreCase(m.modCommand)) {
				ircGoogle(m) ;
			} else if ("gnews".equalsIgnoreCase(m.modCommand) || 
					"googlenews".equalsIgnoreCase(m.modCommand)) {
				ircGoogleNews(m);
			} else if ("nlink".equalsIgnoreCase(m.modCommand) || 
					"newslink".equalsIgnoreCase(m.modCommand)) {
				ircNewsLink(m);
			} else if ("googlebooks".equalsIgnoreCase(m.modCommand) || 
					"bookgoogle".equalsIgnoreCase(m.modCommand) ||
					"booksgoogle".equalsIgnoreCase(m.modCommand) ||
					"booksearch".equalsIgnoreCase(m.modCommand)) {
				ircGoogleBooks(m);
			} else if ("booklink".equalsIgnoreCase(m.modCommand)) {
				ircBookLink(m);
			} else if ("googlepatents".equalsIgnoreCase(m.modCommand) || 
					"patentgoogle".equalsIgnoreCase(m.modCommand) ||
					"patentsearch".equalsIgnoreCase(m.modCommand)) {
				ircGooglePatents(m);
			} else if ("patentlink".equalsIgnoreCase(m.modCommand)
					|| "plink".equalsIgnoreCase(m.modCommand)) {
				ircPatentLink(m);
			} else if ("glink".equalsIgnoreCase(m.modCommand)) {
				ircCachedLink(m);
			} else if ("translate".equalsIgnoreCase(m.modCommand)) {
				ircTranslate(m);
			} else if ("detectlang".equalsIgnoreCase(m.modCommand) ||
					"langdetect".equalsIgnoreCase(m.modCommand) ||
					"detectlanguage".equalsIgnoreCase(m.modCommand) ||
					"languagedetect".equalsIgnoreCase(m.modCommand)) {
				ircDetectLanguage(m);
			} else if ("languages".equalsIgnoreCase(m.modCommand)) {
				ircLanguages(m);
			} else if ("googlefight".equalsIgnoreCase(m.modCommand)) {
				ircGoogleFight(m) ;
				//			} else if ("pornometer".equalsIgnoreCase(m.modCommand) ||
				//					"pronometer".equalsIgnoreCase(m.modCommand) ||
				//					"pr0nometer".equalsIgnoreCase(m.modCommand)) {
				//				ircPornometer(m) ;
			} else if ("sexiness".equalsIgnoreCase(m.modCommand)) {
				ircSexiness(m) ;
			} else if ("gayness".equalsIgnoreCase(m.modCommand)) {
				ircGayness(m) ;
			} else if ("gis".equalsIgnoreCase(m.modCommand)) {
				m.createReply(imageGoogleUrl(m.modTrailing)).send() ;
			} else if ("yis".equalsIgnoreCase(m.modCommand)) {
				m.createReply(imageYahooUrl(m.modTrailing)).send() ;
			} else if ("wikipedia".equalsIgnoreCase(m.modCommand)) {
				m.createReply(wikipediaUrl(m.modTrailing)).send() ;
			} else if ("youtube".equalsIgnoreCase(m.modCommand)) {
				m.createReply(youtubeUrl(m.modTrailing)).send() ;
			} else if ("imdb".equalsIgnoreCase(m.modCommand)) {
				m.createReply(imdbUrl(m.modTrailing)).send() ;
			} else if ("flickr".equalsIgnoreCase(m.modCommand)) {
				m.createReply(flickrUrl(m.modTrailing)).send() ;
			} else {
				m.createReply("No one has gotten around to implementing " + m.modCommand + ".").send() ;
			}
		} catch (MalformedURLException mue) {
			m.createReply("I'm retarded, I couldn't figure out how to make a goojax URL").send();
			mue.printStackTrace();
		} catch (SocketTimeoutException ste) {
			m.createReply("I got bored waiting for Google to respond.").send();
		} catch (IOException f) {
			m.createReply("Couldn't connect to google.").send() ;
			System.out.println(f.toString()) ;
		}
	}

	private void ircGoogleIncludeWikipedia (Message m) 
	throws IOException, SocketTimeoutException, MalformedURLException {
		m.removeFormattingAndColors() ;
		m.createReply(luckyString(m.modTrailing)).send() ;
	}

	private void ircGoogle (Message m) throws IOException, SocketTimeoutException, MalformedURLException {
		m.modTrailing += " -site:wikipedia.org";
		ircGoogleIncludeWikipedia(m);
	}

	private String lastCachedResultType = null;
	private HashMap<String,NewsSearchResponse> newsResponseCache = new HashMap<String,NewsSearchResponse>(); 

	private void ircGoogleNews(Message m) 
	throws IOException, SocketTimeoutException, MalformedURLException {
		NewsSearcher ns = new NewsSearcher();
		String query = Message.removeFormattingAndColors(m.modTrailing);
		NewsSearchResponse nsr = ns.search(query);
		if(null == nsr) {
			m.createReply("Something went horribly wrong in my GooJAX processor").send();
			return;
		}
		if(!nsr.statusNormal()) {
			m.createReply("Error at Google:  " + nsr.getResponseStatus() + ", " + nsr.getResponseDetails()).send();
			return;
		}
		if(nsr.getResponseData().getResults().length < 1) {
			m.createReply("I have no news of " + query).send();
			return;
		}
		NewsSearchResult results[] = nsr.getResponseData().getResults();
		String reply = "";

		for (int newsItem = 0; newsItem < results.length; newsItem++) {
			reply += Message.BOLD + (newsItem + 1) + ")" + Message.NORMAL;
			NewsSearchResult result = results[newsItem];
			reply += " " + getDateLine(result.getPublishedDate());
			reply += ", " + result.getLocation();
			reply += " " + Message.BOLD + "\u2014" + Message.NORMAL + " " + HTMLUtil.convertCharacterEntities(result.getTitleNoFormatting());
			reply += " (" + result.getPublisher() + ")  ";
		}
		//		reply += "   ";
		//		for (int newsItem = 0; newsItem < results.length; newsItem++)
		//			reply += Message.BOLD + (newsItem + 1) + ") " + Message.NORMAL + results[newsItem].getUnescapedUrl() + "  ";
		newsResponseCache.put(m.channame, nsr);
		lastCachedResultType = "news";
		m.createPagedReply(reply).send();
	}

	private void ircNewsLink(Message m) {
		if (! newsResponseCache.containsKey(m.channame)) {
			m.createReply("I'm sorry, I don't have any news for this channel.").send();
			return;
		}
		NewsSearchResponse nsr = newsResponseCache.get(m.channame);
		if (null == nsr) {
			m.createReply("something has gone horribly wrong; my news for this channel seems to be null.").send();
			return;
		}
		int resultNum = 0;
		try {
			if (m.modTrailing.matches("(^\\d+$|^\\d+ +.*)"))
				if(m.modTrailing.matches("^\\d+$"))
					resultNum = Integer.parseInt(m.modTrailing) - 1;
				else
					resultNum = Integer.parseInt(m.modTrailing.substring(0, m.modTrailing.indexOf(' '))) - 1;
		} catch (NumberFormatException nfe) {
			m.createReply("There's no need to be like that, qpt.").send();
		}
		if(resultNum > nsr.getResponseData().getResults().length) {	
			m.createReply("I don't have that many results :(").send();
			return;
		} else if(resultNum < 0) {
			m.createReply("Oh, come on.").send();
			return;
		} else {
			NewsSearchResult re = nsr.getResponseData().getResults()[resultNum];
			String reply = re.getUnescapedUrl()
			+ "  " + re.getTitleNoFormatting()
			+ "  " + getDateLine(re.getPublishedDate())
			+ ", " + re.getLocation()
			+ " (" + re.getPublisher() + ")  "
			+ "  " + Message.BOLD + "\u2014" + Message.NORMAL + "  "
			+ HTMLUtil.textFromHTML(re.getContent());

			m.createPagedReply(reply).send();
		}
	}

	private HashMap<String,BookSearchResponse> booksResponseCache = new HashMap<String,BookSearchResponse>(); 

	private void ircGoogleBooks(Message m) 
	throws IOException, SocketTimeoutException, MalformedURLException {
		BookSearcher bs = new BookSearcher();
		String query = Message.removeFormattingAndColors(m.modTrailing);
		BookSearchResponse bsr = bs.search(query);
		if(null == bsr) {
			m.createReply("Something went horribly wrong in my GooJAX processor").send();
			return;
		}
		if(!bsr.statusNormal()) {
			m.createReply("Error at Google:  " + bsr.getResponseStatus() + ", " + bsr.getResponseDetails()).send();
			return;
		}
		if(bsr.getResponseData().getResults().length < 1) {
			m.createReply("I found no books about " + query).send();
			return;
		}
		BookSearchResult results[] = bsr.getResponseData().getResults();
		String reply = "";

		for (int book = 0; book < results.length; book++) {
			reply += Message.BOLD + (book + 1) + ")" + Message.NORMAL;
			BookSearchResult result = results[book];
			reply += " " + Message.UNDERLINE + result.getTitleNoFormatting() + Message.NORMAL;
			reply += ", " + result.getAuthors();
			if(result.getPublishedYear() != null)
				reply += ", " + result.getPublishedYear();
			if(result.getPageCount() != 0)
				reply += ", " + result.getPageCount() + "pp";
			if(result.getBookId().startsWith("ISBN"))
				reply += ";  isbn " + result.getBookId().substring(4);
			else if (result.getBookId() != null)
				reply += ",  book id: " + result.getBookId();
			reply += "  ";
		}
		//	reply += "   ";
		//	for (int newsItem = 0; newsItem < results.length; newsItem++)
		//		reply += Message.BOLD + (newsItem + 1) + ") " + Message.NORMAL + results[newsItem].getUnescapedUrl() + "  ";
		booksResponseCache.put(m.channame, bsr);
		lastCachedResultType = "book";
		m.createPagedReply(reply).send();
	}



	private void ircBookLink(Message m) {
		if (! booksResponseCache.containsKey(m.channame)) {
			m.createReply("I'm sorry, I don't have any books cached for this channel.").send();
			return;
		}
		BookSearchResponse bsr = booksResponseCache.get(m.channame);
		if (null == bsr) {
			m.createReply("something has gone horribly wrong; my books cache for this channel seems to be null.").send();
			return;
		}
		int resultNum = 0;
		try {
			if (m.modTrailing.matches("(^\\d+$|^\\d+ +.*)"))
				if(m.modTrailing.matches("^\\d+$"))
					resultNum = Integer.parseInt(m.modTrailing) - 1;
				else
					resultNum = Integer.parseInt(m.modTrailing.substring(0, m.modTrailing.indexOf(' '))) - 1;
		} catch (NumberFormatException nfe) {
			m.createReply("There's no need to be like that, qpt.").send();
		}
		if(resultNum > bsr.getResponseData().getResults().length) {	
			m.createReply("I don't have that many results :(").send();
			return;
		} else if(resultNum < 0) {
			m.createReply("Oh, come on.").send();
			return;
		} else {
			BookSearchResult re = bsr.getResponseData().getResults()[resultNum];
			String reply = re.getUnescapedUrl()
			+ "  " + Message.UNDERLINE + re.getTitleNoFormatting() + Message.NORMAL
			+ ", " + re.getPublishedYear()
			+ ",  " + re.getAuthors(); 				
			m.createPagedReply(reply).send();
		}
	}

	private HashMap<String,PatentSearchResponse> patentsResponseCache = new HashMap<String,PatentSearchResponse>(); 

	private void ircGooglePatents(Message m) 
	throws IOException, SocketTimeoutException, MalformedURLException {
		PatentSearcher ps = new PatentSearcher();
		String query = Message.removeFormattingAndColors(m.modTrailing);
		PatentSearchResponse psr = ps.search(query);
		if(null == psr) {
			m.createReply("Something went horribly wrong in my GooJAX processor").send();
			return;
		}
		if(!psr.statusNormal()) {
			m.createReply("Error at Google:  " + psr.getResponseStatus() + ", " + psr.getResponseDetails()).send();
			return;
		}
		if(psr.getResponseData().getResults().length < 1) {
			m.createReply("I found no patents on " + query).send();
			return;
		}
		PatentSearchResult results[] = psr.getResponseData().getResults();
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy");
		String reply = "";

		for (int patent = 0; patent < results.length; patent++) {
			reply += Message.BOLD + (patent + 1) + ")" + Message.NORMAL;
			PatentSearchResult result = results[patent];
			reply += " #" + result.getPatentNumber();
			if(result.getPatentStatus().equals(PatentStatus.PENDING))
				reply += " (pending)";
			if(result.getApplicationDate() != null)
			reply += ", " + sdf.format(result.getApplicationDate());
			reply += " :  " + result.getTitleNoFormatting() + "  ";

		}
		//	reply += "   ";
		//	for (int newsItem = 0; newsItem < results.length; newsItem++)
		//		reply += Message.BOLD + (newsItem + 1) + ") " + Message.NORMAL + results[newsItem].getUnescapedUrl() + "  ";
		patentsResponseCache.put(m.channame, psr);
		lastCachedResultType = "patent";
		m.createPagedReply(reply).send();
	}

	private void ircPatentLink(Message m) {
		if (! patentsResponseCache.containsKey(m.channame)) {
			m.createReply("I'm sorry, I don't have any patents cached for this channel.").send();
			return;
		}
		PatentSearchResponse psr = patentsResponseCache.get(m.channame);
		if (null == psr) {
			m.createReply("something has gone horribly wrong; my patents cache for this channel seems to be null.").send();
			return;
		}
		int resultNum = 0;
		try {
			if (m.modTrailing.matches("(^\\d+$|^\\d+ +.*)"))
				if(m.modTrailing.matches("^\\d+$"))
					resultNum = Integer.parseInt(m.modTrailing) - 1;
				else
					resultNum = Integer.parseInt(m.modTrailing.substring(0, m.modTrailing.indexOf(' '))) - 1;
		} catch (NumberFormatException nfe) {
			m.createReply("There's no need to be like that, qpt.").send();
		}
		if(resultNum > psr.getResponseData().getResults().length) {	
			m.createReply("I don't have that many results :(").send();
			return;
		} else if(resultNum < 0) {
			m.createReply("Oh, come on.").send();
			return;
		} else {
			PatentSearchResult re = psr.getResponseData().getResults()[resultNum];
			String reply = re.getUnescapedUrl()
			+ ", " + re.getTitleNoFormatting();
			m.createPagedReply(reply).send();
		}
	}

	private void ircCachedLink(Message m) {
		if(null == lastCachedResultType)
			m.createReply("I'm afraid I don't have any results cached.").send();
		if(lastCachedResultType.equalsIgnoreCase("book"))
			ircBookLink(m);
		else if(lastCachedResultType.equalsIgnoreCase("news"))
			ircNewsLink(m);
		else if(lastCachedResultType.equalsIgnoreCase("patent"))
			ircPatentLink(m);
		else
			m.createReply("I'm confused about my results cache, I need help.").send();
	}

	private String getDateLine(Date date, TimeZone zone) {
		Date now = new Date();
		DateFormat df = new SimpleDateFormat("ha zzz");
		if(now.getTime() - date.getTime() > (1000L * 60L * 60L * 24L))
			df = new SimpleDateFormat("d MMM");
		else if(now.getTime() - date.getTime() > (1000L * 60L * 60L * 24L * 365L))
			df = new SimpleDateFormat("d MMM, yyyy");
		df.setTimeZone(zone);
		String ret = df.format(date);
		ret = ret.replace("AM ", "am ");
		ret = ret.replace("PM ", "pm ");
		return ret;
	}

	private String getDateLine(Date date) {
		return getDateLine(date, TimeZone.getDefault());
	}

	private void ircTranslate(Message m) throws MalformedURLException, SocketTimeoutException, IOException {
		String text = Message.removeFormattingAndColors(m.modTrailing);
		GooJAXFetcher.Language toLanguage = DEFAULT_GOAT_LANGUAGE; 
		GooJAXFetcher.Language fromLanguage = null;

		int toFrom = 0;
		while(toFrom < 2 && (text.toLowerCase().startsWith("to ") || text.toLowerCase().startsWith("from "))) {
			if (text.toLowerCase().startsWith("to ")) {
				if(text.length() < 4) {
					m.createReply("translate to...?").send();
					return;
				}
				text = text.substring(3).trim();
				int spacepos = text.indexOf(' ');
				if(-1 == spacepos) {
					m.createReply("uh, I need at least two words after that \"to\" of yours").send();
					return;
				}
				String langString = text.substring(0, spacepos).trim();
				text = text.substring(spacepos).trim();
				GooJAXFetcher.Language tempLang = GooJAXFetcher.Language.fromCode(langString);
				if(null == tempLang)
					tempLang = GooJAXFetcher.Language.fromEnglishName(langString);
				if(null == tempLang) {
					m.createReply("Sorry, I don't speak \"" + langString + "\".  Type \"languages\", and I'll tell you which ones I know.").send();
					return;
				}
				toLanguage = tempLang;
				if(text.matches("\\s*") ) {
					m.createReply("Er, what do you want me to translate to " + toLanguage.getEnglishName()).send();
					return;
				}
			} else if(text.toLowerCase().startsWith("from ")) {
				if(text.length() < 6) {
					m.createReply("translate from...?").send();
					return;
				}
				text = text.substring(5).trim();
				int spacepos = text.indexOf(' ');
				if(-1 == spacepos) {
					m.createReply("uh, I need at least two words after that \"from\" of yours").send();
					return;
				}
				String langString = text.substring(0, spacepos).trim();
				text = text.substring(spacepos).trim();
				GooJAXFetcher.Language tempLang = GooJAXFetcher.Language.fromCode(langString);
				if(null == tempLang)
					tempLang = GooJAXFetcher.Language.fromEnglishName(langString);
				if(null == tempLang) {
					m.createReply("Sorry, I don't speak \"" + langString + "\".  Type \"languages\", and I'll tell you which ones I know.").send();
					return;
				}
				fromLanguage = tempLang;
				if(text.matches("\\s*") ) {
					m.createReply("Er, what do you want me to translate from " + fromLanguage.getEnglishName()).send();
					return;
				}
			}
			toFrom++;
		}
		if(text.matches("\\s*")) {
			m.createReply("Er, translate what, exactly?").send();
			return;
		}
		if(! toLanguage.isTranslateable()) {
			m.createReply("Sorry, but I'm not fluent in " + toLanguage.getEnglishName() + ".").send();
			return;
		}
		Translator tranny = new Translator();
		TranslateResponse trs = tranny.translate(text, fromLanguage, toLanguage);
		if(null == trs) {
			// should never get here
			m.createReply("something went horribly wrong when I tried to translate.").send();
			return;
		}
		if(!trs.statusNormal()) {
			m.createReply("problem at Google:  " + trs.getResponseStatus() + ", " + trs.getResponseDetails()).send();
			return;
		}
		if(fromLanguage == null && (null == trs.getDetectedSourceLanguage() || "".equals(trs.getDetectedSourceLanguage())))
			m.createReply("The Google couldn't figure out what language you're speaking, there.").send();
		else if(null == trs.getTranslatedText())
			m.createReply("Translated text is null.  Like, whoa.").send();
		else if(toLanguage.equals(trs.getDetectedSourceLanguage()))
			m.createReply("I'm not going to translate that into the language it's already in.  Jerk.").send();
		else if (fromLanguage == null)
			m.createReply("(from " + trs.getDetectedSourceLanguage().getEnglishName() + ")   " + trs.getTranslatedText()).send();
		else 
			m.createReply(trs.getTranslatedText()).send();
	}

	private void ircDetectLanguage(Message m) {
		Translator tranny = new Translator();
		if(m.modTrailing.matches("^\\s*$")) {
			m.createReply("I detect a " + Message.BOLD + "jerk" + Message.NORMAL + ", with a confidence of 1.0").send();
			return;
		}
		try {
			DetectLanguageResponse dls = tranny.detect(m.modTrailing);
			if(! dls.statusNormal()) {
				m.createPagedReply("I had a problem talking to Google:  " 
						+ dls.getResponseStatus() + ", " 
						+ dls.getResponseDetails()).send();
				return;
			}
			if(dls.getResponseData().isReliable())
				m.createReply("I think that's " + Message.BOLD 
						+ dls.getResponseData().getLanguage().getEnglishName() + Message.NORMAL
						+ ", with a confidence of " 
						+ dls.getResponseData().getConfidence()).send();
			else if(dls.getResponseData().getLanguage() != null)
				m.createReply("That might be " + Message.BOLD 
						+ dls.getResponseData().getLanguage().getEnglishName() + Message.NORMAL
						+ ", but I'm not sure, my confidence is only " 
						+ dls.getResponseData().getConfidence()).send();
			else
				m.createReply("I have no idea what kind of gibber-jabber that might be.").send();

		} catch (SocketTimeoutException ste) {
			m.createReply("I got bored waiting for Google to figure out what language you were using").send();
			ste.printStackTrace();
		} catch (IOException ioe) {
			m.createReply("Something went wrong when I tried to talk to Google").send();
			ioe.printStackTrace();
		}
	}

	private void ircLanguages(Message m) {
		String msg = "I am fluent in:  ";
		for(int i=0; i < GooJAXFetcher.Language.values().length; i++)
			if(GooJAXFetcher.Language.values()[i].isTranslateable()) 
				msg += GooJAXFetcher.Language.values()[i].getEnglishName() + 
				" (" + GooJAXFetcher.Language.values()[i].getCode() + "), ";
		msg = msg.substring(0, msg.lastIndexOf(","));
		String tmp = msg.substring(msg.lastIndexOf(",") + 1);
		msg = msg.substring(0, msg.lastIndexOf(","));
		msg += " and" + tmp + ".";
		//GooJAXmsg += "and " + lastLang.getEnglishName() + " (" + lastLang.getCode() + ")"; 
		m.createPagedReply(msg).send();
	}

	private void ircSexiness (Message m) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		String query = quoteAndClean(m.modTrailing) ;
		int sexyPercentage = Math.round((float) 100 * sexiness(query)) ;
		if (sexyPercentage < 0) {
			m.createReply(query + " does not exist, and therefore can not be appraised for sexiness.").send() ;
		} else {
			m.createReply(query + " is " + sexyPercentage + "% sexy.").send() ;
		}
	}

	private void ircGayness (Message m) 
	throws SocketTimeoutException, MalformedURLException, IOException {
		String query = quoteAndClean(m.modTrailing) ;
		int sexyPercentage = Math.round((float) 100 * gayness(query)) ;
		if (sexyPercentage < 0) {
			m.createReply(query + " does not exist, and therefore can not be appraised for faggotry.").send() ;
		} else {
			m.createReply(query + " is " + sexyPercentage + "% homosexual.").send() ;
		}
	}

	/*
	private void ircPornometer(Message m) 
		throws GoogleSearchFault {
		String query = quoteAndClean(m.modTrailing) ;
		if (query.matches("^[\\\"\\s]*$")) {
			m.createReply("The pornometer is a sophisticated instrument, but it won't do anything unless you give it something to measure.").send() ;
			return ;
		}
		float pornometerReading = GoatGoogle.pornometer(query) ;
		int pornPercent = Math.round((float) 100 * pornometerReading) ;
		if (pornPercent < 0) {
			m.createReply(query + " could not be measured with the pornometer, due to a lack of actually existing.").send() ;
		} else if ((float) 0 == pornometerReading) {
			// a little fun here
			String [] possibleReplies = {
				"Even Jesus would approve of " + query + ".",
				query + " is so fresh and so " + Message.BOLD + "clean!",
				query + " is safe for church.",
				query + " is 100% " + Message.BOLD + "BORING.",
				query + " is as clean as a whistle."
			} ;
			m.createReply(possibleReplies[random.nextInt(possibleReplies.length)]).send();
		} else if ((float) 1 == pornometerReading) {
			String [] possibleReplies = {
				"I totally want to fuck " + query,
				query + " is so totally going to Hell.",
				query + " is completely filthy.",
				query + " is 100% " + Message.BOLD + "HOTTT."
			} ;
			m.createReply(possibleReplies[random.nextInt(possibleReplies.length)]).send() ;
		} else {
			m.createReply(query + " is " + pornPercent + "% pornographic.").send() ;
		}
	}
	 */

	/**
	 * Half of this probably belongs in its own method.
	 */
	private void ircGoogleFight (Message m)
	throws SocketTimeoutException, MalformedURLException, IOException {
		m.removeFormattingAndColors() ;
		String [] contestants = m.modTrailing.split("\\s+[vV][sS]\\.?\\s+") ;
		if (contestants.length < 2) {
			m.createReply("Usage:  \"googlefight \"dirty dogs\" vs. \"fat cats\" [vs. ...]\"").send() ;
			return ;
		}
		for (int i = 0 ; i < contestants.length ; i++) 
			contestants[i] = contestants[i].trim() ;
		int [] scores = getResultCounts(contestants) ;
		int [] winners = getWinners(scores) ;
		switch(winners.length) {
		case 0 : // no winner
			m.createReply("There was no winner, only losers.  Try fighting with things that actually exist.") ;
			break;
		case 1 : // normal
			m.createReply("The winner is " + Message.BOLD + contestants[winners[0]] + Message.BOLD + ", with a score of " + scores[winners[0]] + "!").send() ;
			break;
		default : // tie
			String winnerString = Message.BOLD + contestants[winners[0]] + Message.BOLD ;
		for (int i=1 ; i < winners.length ; i++)
			winnerString += " and " + Message.BOLD + contestants[winners[i]] + Message.BOLD ;
		m.createReply("We have a tie!  " + winnerString + " tied with a score of " + scores[winners[0]]).send() ;
		}
	}

	/* Actually-do-stuff methods
	 */

	/**
	 * Put a given string in quotes, and remove irc gunk.
	 * <p/>
	 * Also removes spaces between quotes, and reduces multiple quotes
	 * down to one (so it's safe to pass in a string that's already
	 * in quotes).
	 * <p/>
	 *	This probably belongs in a utility library somewheres, along with
	 *	all of the convenience methods in goat.core.Message
	 */
	public String quoteAndClean(String s) {
		s = Message.removeFormattingAndColors(s) ;
		s = "\"" + s + "\"" ;
		s = s.replaceAll("\\s*\"\\s*", "\"") ; //remove space around quotes
		s = s.replaceAll("\"+", "\"") ; //strip away multiple quotes
		return s ;
	}

	public String imageGoogleUrl(String s) {
		try {
			return "http://images.google.com/images?safe=off&q=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		/*
		s = Message.removeFormattingAndColors(s) ;
		URL u;
		try {
			u = new URL("http://images.google.com/images?safe=off&q=" + s) ;
			return u.toString();
		} catch(MalformedURLException e) {
			e.printStackTrace() ;
		}
		 */
		return "";
	}

	public String imageYahooUrl(String s) {
		try {
			return "http://images.search.yahoo.com/search/images?&p=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String wikipediaUrl(String s) {
		try {
			return "http://www.wikipedia.org/wiki/Special:Search?search=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String youtubeUrl(String s) {
		try {
			return "http://youtube.com/results?search_query=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String imdbUrl(String s) {
		try {
			return "http://imdb.com/find?s=all&q=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String flickrUrl(String s) {
		try {
			return "http://flickr.com/search/?q=" + URLEncoder.encode(s.trim(), encoding) + " " + Message.BOLD + " "  ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Given an array of query strings, return an array of search-result counts.
	 */

	public int[] getResultCounts(String[] queries)
	throws IOException, MalformedURLException, SocketTimeoutException {
		WebSearcher ws = new WebSearcher();
		ws.setSafeSearch(WebSearcher.SafeSearch.NONE);
		int [] counts = new int[queries.length] ;
		for (int i = 0 ; i < queries.length; i++) {
			if (queries[i].matches("\\s*")) { // if string is empty
				counts[i] = -1 ;
			} else {
				SearchResponse srs = ws.search(queries[i]);
				if(srs.statusNormal())
					counts[i] = srs.getEstimatedResultCount();
				else
					counts[i] = -1; // error at google
			}
		}
		return counts ;
	}

	/**
	 * Given an array of int, return an array of int containing the index of the largest element (or elements, in case of a tie).
	 *
	 */
	public int [] getWinners(int [] scores) {
		int[] indices = new int[scores.length] ;
		if (indices.length == 0) 
			return indices ;
		indices[0] = 0 ;
		int lastIndex = 0 ;
		for (int i = 1 ; i < scores.length ; i++ ) {
			if (scores[i] > scores[indices[0]]) { // new high
				indices[0] = i ;
				lastIndex = 0 ;
			} else if (scores[i] == scores[indices[0]]) { // tie
				indices[++lastIndex] = i ;
			}
		}
		int [] ret = new int[lastIndex + 1] ;
		System.arraycopy(indices, 0, ret, 0, lastIndex + 1) ;
		return ret ;
	}


	/* String-emitting convenience methods.
	 */

	/**
	 * Takes a GoogleSearchResultElement and gives you a simple, irc-friendly string representation of it.
	 */
	public String simpleResultString (SearchResult re) {
		if (null == re)
			return noResultString ;
		return boldConvert(re.getTitle()) + "  " + re.getUnescapedUrl() ;
	}

	/**
	 * returns an irc-friendly string representation of first search result.
	 */
	public String luckyString (String query, boolean safe)
	throws IOException, SocketTimeoutException, MalformedURLException {
		WebSearcher ws = new WebSearcher();
		if(!safe)
			ws.setSafeSearch(WebSearcher.SafeSearch.NONE);
		SearchResponse srs = ws.search(query);
		String ret = "";
		if(srs.statusNormal())
			if(srs.getResponseData().getResults().length > 0)
				ret = simpleResultString(srs.getResponseData().getResults()[0]);
			else
				ret = noResultString + " for \"" + Message.removeFormattingAndColors(query) + "\"";
		else
			ret = "Error at Google:  " + srs.getResponseStatus() + ", " + srs.getResponseDetails();
		return ret ;
	}

	public String luckyString (String query)
	throws IOException, SocketTimeoutException, MalformedURLException {
		return luckyString(query, false) ;
	}

	/**
	 * Convert html &lt;b&gt; tags in a string to irc BOLD formatting characters
	 */
	public String boldConvert (String s) {
		return s.replaceAll("<[/ ]*[bB] *>", Message.BOLD) ;
	}

	/**
	 * Return the estimated sexiness of the given string.
	 * <p>
	 * You should probably clean up your query and quote it
	 * before you pass it to this method. Like with quoteAndClean(), say.
	 *
	 * @param   query your search string.
	 * @return        a float between 0 and 1, usually, but sometimes
	 *                more than 1.  -1 if google returns no results for
	 *                your query.
	 * @see     quoteAndClean()
	 */
	public static float sexiness (String query)
	throws IOException, MalformedURLException, SocketTimeoutException {

		WebSearcher ws = new WebSearcher();
		ws.setSafeSearch(WebSearcher.SafeSearch.NONE);

		SearchResponse plainResult = ws.search(query) ;
		if (null == plainResult || ! plainResult.statusNormal() || plainResult.getEstimatedResultCount() < 1)
			return -1 ;
		SearchResponse sexResult = ws.search(query + " sex");
		//debug
		//System.out.println(sexResult.getEstimatedTotalResultsCount()) ;
		//System.out.println(plainResult.getEstimatedTotalResultsCount()) ;
		float ret = -1;
		if(sexResult != null && sexResult.statusNormal())
			ret =  (float) sexResult.getEstimatedResultCount() /
			(float) plainResult.getEstimatedResultCount() ;
		return ret;
	}

	public static float gayness (String query)
	throws MalformedURLException, IOException, SocketTimeoutException {
		WebSearcher ws = new WebSearcher();
		ws.setSafeSearch(WebSearcher.SafeSearch.NONE);

		SearchResponse plainResult = ws.search(query) ;
		if (null == plainResult || plainResult.getEstimatedResultCount() < 1)
			return -1 ;
		SearchResponse gayResult = ws.search(query + " gay");
		float ret = -1;
		if(null != gayResult && gayResult.statusNormal())
			ret = (float) gayResult.getEstimatedResultCount() /
			(float) plainResult.getEstimatedResultCount() ;
		return ret;
	}

	/*
	 * main() replaced with junit class in src/goat/test
	 * 
	public static void main(String[] args) {
		Google g = new Google() ;
		try {
			System.out.println(g.luckyString("goat fucker")) ;
			System.out.println("\"goat\" is " + (int) (100.0 * g.sexiness("goat")) + "% sexy") ;
			System.out.println("\"asdfsaidfyuwcv129038\" is " + (int) (100.0 * g.sexiness("asdfsaidfyuwcv129038")) + "% sexy") ;
			System.out.println(GoatGoogle.numSearchesToday() + " searches in the past 24 hours.") ;
		} catch (GoogleSearchFault f) {
			System.out.println(f.toString()) ;
		}
	}
	 */
}

