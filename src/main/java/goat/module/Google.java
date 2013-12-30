package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.CommandParser;
import goat.util.HTMLUtil;
import goat.util.StringUtil;
import goojax.GooJAXFetcher;
import goojax.search.AbstractSearchResult;
import goojax.search.BlogSearchResult;
import goojax.search.BlogSearcher;
import goojax.search.BookSearchResult;
import goojax.search.BookSearcher;
import goojax.search.NewsSearchResult;
import goojax.search.NewsSearcher;
import goojax.search.PatentSearchResult;
import goojax.search.PatentSearcher;
import goojax.search.SearchResponse;
import goojax.search.WebSearcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
// import java.util.Random ;
//import java.net.URL ;
//import java.net.MalformedURLException;

/**
 * Module to ask google about stuff.
 *
 * This module is over 1000 lines long now, that's probably too much.
 *
 * @author encontrado Created on 27-Feb-2006
 */
public class Google extends Module {

    public static final String encoding = "UTF-8";
    public static final String noResultString = "No results found";

    // private static Random random = new Random() ;

    /*
     * IRC methods
     */

    public Google() {
        GooJAXFetcher.setDefaultTimeout(3000);
        GooJAXFetcher.setDefaultHttpReferrer("http://goat-blog.blogspot.com/");
        GooJAXFetcher
                .setDefaultKey("ABQIAAAA3SYwJ1rsiLgTvuisAwhOWBSj2h-HwVayfKLTNoeW4qFtyKpsrhSAZlVe3nAKyDZbufib0rUbOQ-MvA");
    }

    @Override
    public String[] getCommands() {
        return new String[] {
            "google",
            "googlefight",
            // "pornometer",
            "sexiness", "gis", "yis", "wikipedia", "youtube", "imdb",
            "gayness", "flickr", "spergle",
            "gnews", "booksearch", "patentsearch", "blogsearch",
            "glink" };
    }

    @Override
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    @Override
    public void processChannelMessage(Message m) {
        // debug
        // System.out.println("PROCESSING:  " + m.modCommand) ;
        String command = StringUtil
                .removeFormattingAndColors(m.getModCommand());
        try {
            if ("google".equalsIgnoreCase(command))
                ircGoogle(m);
            else if ("gnews".equalsIgnoreCase(command))
                ircGoogleNews(m);
            else if ("booksearch".equalsIgnoreCase(command))
                ircGoogleBooks(m);
            else if ("patentsearch".equalsIgnoreCase(command))
                ircGooglePatents(m);
            else if ("blogsearch".equalsIgnoreCase(command))
                ircGoogleBlogs(m);
            else if ("glink".equalsIgnoreCase(command))
                ircCachedLink(m);
            else if ("googlefight".equalsIgnoreCase(command))
                ircGoogleFight(m);
            // else if ("pornometer".equalsIgnoreCase(m.modCommand))
            // ircPornometer(m);
            else if ("sexiness".equalsIgnoreCase(command))
                ircSexiness(m);
            else if ("gayness".equalsIgnoreCase(command))
                ircGayness(m);
            else if ("gis".equalsIgnoreCase(command)) {
                CommandParser cp = new CommandParser(m);
                if (cp.hasVar("provider")
                        && cp.get("provider").equalsIgnoreCase("my mommy"))
                    m.reply(imageGoogleUrl(cp.remaining()));
                else
                    m.reply(imageBingUrl(cp.remaining()));
            } else if ("yis".equalsIgnoreCase(command))
                m.reply(imageYahooUrl(m.getModTrailing()));
            else if ("wikipedia".equalsIgnoreCase(command))
                m.reply(wikipediaUrl(m.getModTrailing()));
            else if ("youtube".equalsIgnoreCase(command))
                m.reply(youtubeUrl(m.getModTrailing()));
            else if ("imdb".equalsIgnoreCase(command))
                m.reply(imdbUrl(m.getModTrailing()));
            else if ("flickr".equalsIgnoreCase(command))
                m.reply(flickrUrl(m.getModTrailing()));
            else if ("spergle".equalsIgnoreCase(command))
                ircSpergle(m);
            else
                m.reply("No one has gotten around to implementing " + command + ".");

        } catch (MalformedURLException mue) {
            m.reply("I'm retarded, I couldn't figure out how to make a goojax URL");
            mue.printStackTrace();
        } catch (SocketTimeoutException ste) {
            m.reply("I got bored waiting for Google to respond.");
        } catch (IOException f) {
            m.reply("Couldn't connect to google.");
            System.out.println(f.toString());
        }
    }

    private void ircGoogleIncludeWikipedia(Message m, String query)
            throws IOException, SocketTimeoutException, MalformedURLException {
        m.reply(luckyString(StringUtil.removeFormattingAndColors(query)));
    }

    private void ircGoogle(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$")) {
            m.reply("Er, what do you want me to google for you?");
            return;
        }
        ircGoogleIncludeWikipedia(m, query + " -site:wikipedia.org");
    }

    private void ircSpergle(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$"))
            m.reply("NO NO NO NO YOU MUST GIVE ME WORDS TO SEARCH FOR YOU ARE SO STUPID SO STUPID AND WRONG");
        else if (query.indexOf('"') >= 0)
            m.reply("I AM THE ONE WHO MAKES QUOTES YOU DO NOT MAKE QUOTES IF YOU WANT TO MAKE QUOTES YOU MUST TALK TO GOOGLE YOURSELF YOU ARE SO STUPID SO STUPID AND WRONG");
        else {
            String[] tokes = query.split("\\s+");
            StringBuilder sb = new StringBuilder("\"");
            for(int i=0; i<tokes.length; i++)
                sb.append(tokes[i] + "\" ");
            ircGoogleIncludeWikipedia(m, sb.toString() + "-site:wikipedia.org");
        }
    }

    private String lastCachedResultType = null;
    private final HashMap<String, SearchResponse<NewsSearchResult>> newsResponseCache = new HashMap<String, SearchResponse<NewsSearchResult>>();
    private boolean newsLinkMode = false;

    private void ircGoogleNews(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        // for em
        String mTrail = StringUtil
                .removeFormattingAndColors(m.getModTrailing()).toLowerCase()
                .replaceAll("\\s+", " ").trim();
        if (newsLinkMode && mTrail.matches("^\\d$")) {
            m.pagedReply(newsLinkReply(mTrail, m.getChanname()));
            return;
        } else if (mTrail.startsWith("m-x news-link")) {
            String r;
            if (mTrail.matches("^m-x news-link \\d\\s*")) {
                String nlink = m.getWord(m.getWords().size() - 1);
                nlink = StringUtil.removeFormattingAndColors(nlink).trim();
                r = newsLinkReply(nlink, m.getChanname());
            } else if (mTrail.equals("m-x news-link-mode")) {
                newsLinkMode = !newsLinkMode;
                r = "News-link mode ";
                if (newsLinkMode)
                    r += "en";
                else
                    r += "dis";
                r += "abled"; // wtf.
            } else if (mTrail.equals("m-x news-link")) {
                r = newsLinkReply("", m.getChanname());
            } else if (mTrail.startsWith("m-x news-link ")) {
                r = "Invalid argument.";
            } else {
                r = "[no match]";
            }
            m.pagedReply(r);
            return;
        }
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$")) {
            m.reply("What do you want news of?");
            return;
        }

        // fix news about duke nukem
        query = query.replaceAll("(?i)duke nukem", "gay muscles");

        // be mean to joey
        // if (true) {
        if (m.getHostmask().matches(".*\\.ca\\.comcast\\.net.*")) {
            if (query
                    .matches("(?i).*(apple|ipad|iphone|sonic|hedgehog|generations|nukem|eden|sega|lady gaga).*")) {
                String[] newquery1 = { "gay", "homosexual", "bisexual" };
                String[] newquery2 = { "autistic", "hispanic", "stammering" };
                if (query
                        .matches("(?i).*(sonic|hedgehog|generations|sega|nukem|eden).*")) {
                    newquery1 = newquery2;
                    newquery2 = new String[] { "pedophile", "paedophile",
                            "child molestor" };
                }
                Random r = new Random();
                query = newquery1[r.nextInt(newquery1.length)] + " "
                        + newquery2[r.nextInt(newquery2.length)];
            }
        }

        NewsSearcher ns = new NewsSearcher();
        SearchResponse<NewsSearchResult> nsr = ns.search(query);
        if (null == nsr) {
            m.reply("Something went horribly wrong in my GooJAX processor");
            return;
        }
        if (!nsr.statusNormal()) {
            m.reply("Error at Google:  " + nsr.getResponseStatus() + ", "
                    + nsr.getResponseDetails());
            return;
        }
        if (nsr.getResponseData().getResults().length < 1) {
            m.reply("I have no news of " + query);
            return;
        }
        NewsSearchResult results[] = nsr.getResponseData().getResults();
        String reply = "";

        for (int newsItem = 0; newsItem < results.length; newsItem++) {
            reply += Constants.BOLD + (newsItem + 1) + ")" + Constants.NORMAL;
            NewsSearchResult result = results[newsItem];
            Date date = result.getPublishedDate();
            if (date != null)
                reply += " " + getDateLine(date);
            reply += ", " + result.getLocation();
            reply += " "
                    + Constants.BOLD
                    + "\u2014"
                    + Constants.NORMAL
                    + " "
                    + HTMLUtil.convertCharacterEntities(result
                            .getTitleNoFormatting());
            reply += " (" + result.getPublisher() + ")  ";
        }
        // reply += "   ";
        // for (int newsItem = 0; newsItem < results.length; newsItem++)
        // reply += Message.BOLD + (newsItem + 1) + ") " + Message.NORMAL +
        // results[newsItem].getUnescapedUrl() + "  ";
        newsResponseCache.put(m.getChanname(), nsr);
        lastCachedResultType = "news";
        m.pagedReply(reply);
    }

    private String newsLinkReply(String modTrailing, String channel) {
        if (!newsResponseCache.containsKey(channel)) {
            return "I'm sorry, I don't have any news for this channel.";
        }
        SearchResponse<NewsSearchResult> nsr = newsResponseCache.get(channel);
        if (null == nsr) {
            return "something has gone horribly wrong; my news for this channel seems to be null.";
        }

        int resultNum = 0;
        try {
            String mTrail = StringUtil.removeFormattingAndColors(modTrailing)
                    .trim();
            if (mTrail.matches("^\\d+$"))
                resultNum = Integer.parseInt(mTrail) - 1;
        } catch (NumberFormatException nfe) {
            return "There's no need to be like that, qpt.";
        }

        String reply = "";
        if (resultNum > nsr.getResponseData().getResults().length) {
            reply = "I don't have that many results :(";
        } else if (resultNum < 0) {
            reply = "Oh, come on.";
        } else {
            NewsSearchResult re = nsr.getResponseData().getResults()[resultNum];
            reply = re.getUnescapedUrl() + "  " + re.getTitleNoFormatting();
            Date date = re.getPublishedDate();
            if (date != null)
                reply += "  " + getDateLine(re.getPublishedDate());
            reply += ", " + re.getLocation() + " (" + re.getPublisher() + ")  "
                    + "  " + Constants.BOLD + "\u2014" + Constants.NORMAL
                    + "  " + HTMLUtil.textFromHTML(re.getContent());
        }
        return reply;
    }

    private void ircNewsLink(Message m) {
        m.pagedReply(newsLinkReply(m.getModTrailing(), m.getChanname()));
        // if (! newsResponseCache.containsKey(m.getChanname())) {
        // m.reply("I'm sorry, I don't have any news for this channel.");
        // return;
        // }
        // NewsSearchResponse nsr = newsResponseCache.get(m.getChanname());
        // if (null == nsr) {
        // m.reply("something has gone horribly wrong; my news for this channel seems to be null.");
        // return;
        // }
        // int resultNum = 0;
        // try {
        // if (m.getModTrailing().matches("(^\\d+$|^\\d+ +.*)"))
        // if(m.getModTrailing().matches("^\\d+$"))
        // resultNum = Integer.parseInt(m.getModTrailing()) - 1;
        // else
        // resultNum = Integer.parseInt(m.getModTrailing().substring(0,
        // m.getModTrailing().indexOf(' '))) - 1;
        // } catch (NumberFormatException nfe) {
        // m.reply("There's no need to be like that, qpt.");
        // }
        // if(resultNum > nsr.getResponseData().getResults().length) {
        // m.reply("I don't have that many results :(");
        // return;
        // } else if(resultNum < 0) {
        // m.reply("Oh, come on.");
        // return;
        // } else {
        // NewsSearchResult re = nsr.getResponseData().getResults()[resultNum];
        // String reply = re.getUnescapedUrl()
        // + "  " + re.getTitleNoFormatting();
        // Date date = re.getPublishedDate();
        // if(date != null)
        // reply += "  " + getDateLine(re.getPublishedDate());
        // reply += ", " + re.getLocation()
        // + " (" + re.getPublisher() + ")  "
        // + "  " + Constants.BOLD + "\u2014" + Constants.NORMAL + "  "
        // + HTMLUtil.textFromHTML(re.getContent());
        //
        // m.pagedReply(reply);
        // }
    }

    private final HashMap<String, SearchResponse<BookSearchResult>> booksResponseCache = new HashMap<String, SearchResponse<BookSearchResult>>();

    private void ircGoogleBooks(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$")) {
            m.reply("Um, what do you want me to look for in all these books?");
            return;
        }
        BookSearcher bs = new BookSearcher();
        SearchResponse<BookSearchResult> bsr = bs.search(query);
        if (null == bsr) {
            m.reply("Something went horribly wrong in my GooJAX processor");
            return;
        }
        if (!bsr.statusNormal()) {
            m.reply("Error at Google:  " + bsr.getResponseStatus() + ", "
                    + bsr.getResponseDetails());
            return;
        }
        if (bsr.getResponseData().getResults().length < 1) {
            m.reply("I found no books about " + query);
            return;
        }
        BookSearchResult results[] = bsr.getResponseData().getResults();
        String reply = "";

        for (int book = 0; book < results.length; book++) {
            reply += Constants.BOLD + (book + 1) + ")" + Constants.NORMAL;
            BookSearchResult result = results[book];
            reply += " " + Constants.UNDERLINE + result.getTitleNoFormatting()
                    + Constants.NORMAL;
            reply += ", " + result.getAuthors();
            if (result.getPublishedYear() != null)
                reply += ", " + result.getPublishedYear();
            if (result.getPageCount() != 0)
                reply += ", " + result.getPageCount() + "pp";
            if (result.getBookId().startsWith("ISBN"))
                reply += ";  isbn " + result.getBookId().substring(4);
            else if (result.getBookId() != null)
                reply += ",  book id: " + result.getBookId();
            reply += "  ";
        }
        // reply += "   ";
        // for (int newsItem = 0; newsItem < results.length; newsItem++)
        // reply += Message.BOLD + (newsItem + 1) + ") " + Message.NORMAL +
        // results[newsItem].getUnescapedUrl() + "  ";
        booksResponseCache.put(m.getChanname(), bsr);
        lastCachedResultType = "book";
        m.pagedReply(reply);
    }

    private void ircBookLink(Message m) {
        if (!booksResponseCache.containsKey(m.getChanname())) {
            m.reply("I'm sorry, I don't have any books cached for this channel.");
            return;
        }
        SearchResponse<BookSearchResult> bsr = booksResponseCache.get(m.getChanname());
        if (null == bsr) {
            m.reply("something has gone horribly wrong; my books cache for this channel seems to be null.");
            return;
        }
        int resultNum = 0;
        try {
            if (m.getModTrailing().matches("(^\\d+$|^\\d+ +.*)"))
                if (m.getModTrailing().matches("^\\d+$"))
                    resultNum = Integer.parseInt(m.getModTrailing()) - 1;
                else
                    resultNum = Integer.parseInt(m.getModTrailing().substring(
                            0, m.getModTrailing().indexOf(' '))) - 1;
        } catch (NumberFormatException nfe) {
            m.reply("There's no need to be like that, qpt.");
        }
        if (resultNum > bsr.getResponseData().getResults().length) {
            m.reply("I don't have that many results :(");
            return;
        } else if (resultNum < 0) {
            m.reply("Oh, come on.");
            return;
        } else {
            BookSearchResult re = bsr.getResponseData().getResults()[resultNum];
            String reply = re.getUnescapedUrl() + "  " + Constants.UNDERLINE
                    + re.getTitleNoFormatting() + Constants.NORMAL + ", "
                    + re.getPublishedYear() + ",  " + re.getAuthors();
            m.pagedReply(reply);
        }
    }

    private final HashMap<String, SearchResponse<PatentSearchResult>> patentsResponseCache = new HashMap<String, SearchResponse<PatentSearchResult>>();

    private void ircGooglePatents(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$")) {
            m.reply("Yes, well, what sort of patents am I supposed to look for, then?");
            return;
        }
        PatentSearcher ps = new PatentSearcher();
        SearchResponse<PatentSearchResult> psr = ps.search(query);
        if (null == psr) {
            m.reply("Something went horribly wrong in my GooJAX processor");
            return;
        }
        if (!psr.statusNormal()) {
            m.reply("Error at Google:  " + psr.getResponseStatus() + ", "
                    + psr.getResponseDetails());
            return;
        }
        if (psr.getResponseData().getResults().length < 1) {
            m.reply("I found no patents on " + query);
            return;
        }
        PatentSearchResult results[] = psr.getResponseData().getResults();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy");
        String reply = "";

        for (int patent = 0; patent < results.length; patent++) {
            reply += Constants.BOLD + (patent + 1) + ")" + Constants.NORMAL;
            PatentSearchResult result = results[patent];
            reply += " #" + result.getPatentNumber();
            if (result.getPatentStatus().equals(PatentSearchResult.PatentStatus.PENDING))
                reply += " (pending)";
            if (result.getApplicationDate() != null)
                reply += ", " + sdf.format(result.getApplicationDate());
            reply += " :  " + result.getTitleNoFormatting() + "  ";

        }
        reply += "   ";
        patentsResponseCache.put(m.getChanname(), psr);
        lastCachedResultType = "patent";
        m.pagedReply(reply);
    }

    private void ircPatentLink(Message m) {
        if (!patentsResponseCache.containsKey(m.getChanname())) {
            m.reply("I'm sorry, I don't have any patents cached for this channel.");
            return;
        }
        SearchResponse<PatentSearchResult> psr = patentsResponseCache.get(m.getChanname());
        if (null == psr) {
            m.reply("something has gone horribly wrong; my patents cache for this channel seems to be null.");
            return;
        }
        int resultNum = 0;
        try {
            if (m.getModTrailing().matches("(^\\d+$|^\\d+ +.*)"))
                if (m.getModTrailing().matches("^\\d+$"))
                    resultNum = Integer.parseInt(m.getModTrailing()) - 1;
                else
                    resultNum = Integer.parseInt(m.getModTrailing().substring(
                            0, m.getModTrailing().indexOf(' '))) - 1;
        } catch (NumberFormatException nfe) {
            m.reply("There's no need to be like that, qpt.");
        }
        if (resultNum > psr.getResponseData().getResults().length) {
            m.reply("I don't have that many results :(");
            return;
        } else if (resultNum < 0) {
            m.reply("Oh, come on.");
            return;
        } else {
            PatentSearchResult re = psr.getResponseData().getResults()[resultNum];
            String reply = re.getUnescapedUrl() + " "
                    + re.getTitleNoFormatting();
            m.pagedReply(reply);
        }
    }

    private final HashMap<String, SearchResponse<BlogSearchResult>> blogsResponseCache = new HashMap<String, SearchResponse<BlogSearchResult>>();

    private void ircGoogleBlogs(Message m) throws IOException,
            SocketTimeoutException, MalformedURLException {
        String query = StringUtil.removeFormattingAndColors(m.getModTrailing());
        if (query.matches("^\\s*$")) {
            m.reply("What kind of blogging were you looking for?");
            return;
        }
        BlogSearcher bs = new BlogSearcher();
        SearchResponse<BlogSearchResult> bsr = bs.search(query);
        if (null == bsr) {
            m.reply("Something went horribly wrong in my GooJAX processor");
            return;
        }
        if (!bsr.statusNormal()) {
            m.reply("Error at Google:  " + bsr.getResponseStatus() + ", "
                    + bsr.getResponseDetails());
            return;
        }
        if (bsr.getResponseData().getResults().length < 1) {
            m.reply("I found no blogging about " + query);
            return;
        }
        BlogSearchResult results[] = bsr.getResponseData().getResults();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy");
        String reply = "";

        for (int post = 0; post < results.length; post++) {
            reply += Constants.BOLD + (post + 1) + ")" + Constants.NORMAL;
            BlogSearchResult result = results[post];
            reply += " " + sdf.format(result.getPublishedDate());
            reply += " " + HTMLUtil.textFromHTML(result.getTitle());
            reply += " (" + extractDomainName(result.getBlogUrl()) + ")";
            reply += "  ";
        }
        reply += "  ";
        blogsResponseCache.put(m.getChanname(), bsr);
        lastCachedResultType = "blog";
        m.pagedReply(reply);
    }

    private String extractDomainName(String url) {
        String ret = "";
        if (url.startsWith("http")) {
            int startOffset = 7; // position after "http://"
            if (url.startsWith("https"))
                startOffset++;
            if (url.length() <= startOffset)
                return ret;
            ret = url.substring(startOffset);
            if (ret.contains("/"))
                ret = ret.substring(0, ret.indexOf('/'));
        }
        return ret;
    }

    private void ircBlogLink(Message m) {
        if (!blogsResponseCache.containsKey(m.getChanname())) {
            m.reply("I'm sorry, I don't have any blogs cached for this channel.");
            return;
        }
        SearchResponse<BlogSearchResult> bsr = blogsResponseCache.get(m.getChanname());
        if (null == bsr) {
            m.reply("something has gone horribly wrong; my blogs cache for this channel seems to be null.");
            return;
        }
        int resultNum = 0;
        try {
            if (m.getModTrailing().matches("(^\\d+$|^\\d+ +.*)"))
                if (m.getModTrailing().matches("^\\d+$"))
                    resultNum = Integer.parseInt(m.getModTrailing()) - 1;
                else
                    resultNum = Integer.parseInt(m.getModTrailing().substring(
                            0, m.getModTrailing().indexOf(' '))) - 1;
        } catch (NumberFormatException nfe) {
            m.reply("There's no need to be like that, qpt.");
        }
        if (resultNum > bsr.getResponseData().getResults().length) {
            m.reply("I don't have that many results :(");
            return;
        } else if (resultNum < 0) {
            m.reply("Oh, come on.");
            return;
        } else {
            BlogSearchResult re = bsr.getResponseData().getResults()[resultNum];
            String reply = re.getPostUrl() + "  "
                    + HTMLUtil.textFromHTML(re.getContent());
            m.pagedReply(reply);
        }
    }

    private void ircCachedLink(Message m) {
        if (null == lastCachedResultType)
            m.reply("I'm afraid I don't have any results cached.");
        else if (lastCachedResultType.equalsIgnoreCase("book"))
            ircBookLink(m);
        else if (lastCachedResultType.equalsIgnoreCase("news"))
            ircNewsLink(m);
        else if (lastCachedResultType.equalsIgnoreCase("patent"))
            ircPatentLink(m);
        else if (lastCachedResultType.equalsIgnoreCase("blog"))
            ircBlogLink(m);
        else
            m.reply("I'm confused about my results cache, I need help.");
    }


    private String getDateLine(Date date, TimeZone zone) {
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("ha zzz");
        if (now.getTime() - date.getTime() > (1000L * 60L * 60L * 24L))
            df = new SimpleDateFormat("d MMM");
        else if (now.getTime() - date.getTime() > (1000L * 60L * 60L * 24L * 365L))
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


    private void ircSexiness(Message m) throws SocketTimeoutException,
            MalformedURLException, IOException {
        String query = quoteAndClean(m.getModTrailing());
        int sexyPercentage = Math.round(100 * sexiness(query));
        if (sexyPercentage < 0) {
            m.reply(query
                    + " does not exist, and therefore can not be appraised for sexiness.");
        } else {
            m.reply(query + " is " + sexyPercentage + "% sexy.");
        }
    }

    private void ircGayness(Message m) throws SocketTimeoutException,
            MalformedURLException, IOException {
        String query = quoteAndClean(m.getModTrailing());
        int sexyPercentage = Math.round(100 * gayness(query));
        if (sexyPercentage < 0) {
            m.reply(query
                    + " does not exist, and therefore can not be appraised for faggotry.");
        } else {
            m.reply(query + " is " + sexyPercentage + "% homosexual.");
        }
    }

    /*
     * private void ircPornometer(Message m) throws GoogleSearchFault { String
     * query = quoteAndClean(m.modTrailing) ; if (query.matches("^[\\\"\\s]*$"))
     * { m.reply(
     * "The pornometer is a sophisticated instrument, but it won't do anything unless you give it something to measure."
     * ) ; return ; } float pornometerReading = GoatGoogle.pornometer(query) ;
     * int pornPercent = Math.round((float) 100 * pornometerReading) ; if
     * (pornPercent < 0) { m.reply(query +
     * " could not be measured with the pornometer, due to a lack of actually existing."
     * ) ; } else if ((float) 0 == pornometerReading) { // a little fun here
     * String [] possibleReplies = { "Even Jesus would approve of " + query +
     * ".", query + " is so fresh and so " + Message.BOLD + "clean!", query +
     * " is safe for church.", query + " is 100% " + Message.BOLD + "BORING.",
     * query + " is as clean as a whistle." } ;
     * m.reply(possibleReplies[random.nextInt(possibleReplies.length)]); } else
     * if ((float) 1 == pornometerReading) { String [] possibleReplies = {
     * "I totally want to fuck " + query, query +
     * " is so totally going to Hell.", query + " is completely filthy.", query
     * + " is 100% " + Message.BOLD + "HOTTT." } ;
     * m.reply(possibleReplies[random.nextInt(possibleReplies.length)]) ; } else
     * { m.reply(query + " is " + pornPercent + "% pornographic.") ; } }
     */

    /**
     *
     */
    private void ircGoogleFight(Message m) throws SocketTimeoutException,
            MalformedURLException, IOException {
        String[] contestants = StringUtil.removeFormattingAndColors(
                m.getModTrailing()).split("\\s+[vV][sS]\\.?\\s+");
        if (contestants.length < 2) {
            m.reply("Usage:  \"googlefight \"dirty dogs\" vs. \"fat cats\" [vs. ...]\"");
            return;
        }
        for (int i = 0; i < contestants.length; i++)
            contestants[i] = contestants[i].trim();
        long[] scores = getResultCounts(contestants);
        int[] winners = getWinners(scores);
        switch (winners.length) {
        case 0: // no winner
            m.reply("There was no winner, only losers.  Try fighting with things that actually exist.");
            break;
        case 1: // normal
            m.reply("The winner is " + Constants.BOLD + contestants[winners[0]]
                    + Constants.BOLD + ", with a score of "
                    + scores[winners[0]] + "!");
            break;
        default: // tie
            String winnerString = Constants.BOLD + contestants[winners[0]]
                    + Constants.BOLD;
            for (int i = 1; i < winners.length; i++)
                winnerString += " and " + Constants.BOLD
                        + contestants[winners[i]] + Constants.BOLD;
            m.reply("We have a tie!  " + winnerString
                    + " tied with a score of " + scores[winners[0]]);
            break;
        }
    }

    /*
     * Actually-do-stuff methods
     */

    /**
     * Put a given string in quotes, and remove irc gunk.
     * <p/>
     * Also removes spaces between quotes, and reduces multiple quotes down to
     * one (so it's safe to pass in a string that's already in quotes).
     * <p/>
     * This probably belongs in a utility library somewheres, along with all of
     * the convenience methods in goat.core.Message
     */
    public String quoteAndClean(String s) {
        s = StringUtil.removeFormattingAndColors(s);
        s = "\"" + s + "\"";
        s = s.replaceAll("\\s*\"\\s*", "\""); // remove space around quotes
        s = s.replaceAll("\"+", "\""); // strip away multiple quotes
        return s;
    }

    public String imageBingUrl(String s) {
        s = StringUtil.removeFormattingAndColors(s);
        if (! s.matches(".*[\"'+].*"))
            s = "+" + s.trim().replaceAll("\\s+", " +");
        try {
            return "http://www.bing.com/images/search?adlt=off&q="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String imageGoogleUrl(String s) {
        s = StringUtil.removeFormattingAndColors(s);
        try {
            return "http://images.google.com/images?safe=off&q="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String imageYahooUrl(String s) {
        s = StringUtil.removeFormattingAndColors(s);
        try {
            return "http://images.search.yahoo.com/search/images?&p="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String wikipediaUrl(String s) {
        try {
            return "http://www.wikipedia.org/wiki/Special:Search?search="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String youtubeUrl(String s) {
        try {
            return "http://youtube.com/results?search_query="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String imdbUrl(String s) {
        try {
            return "http://imdb.com/find?s=all&q="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String flickrUrl(String s) {
        try {
            return "http://flickr.com/search/?q="
                    + URLEncoder.encode(s.trim(), encoding) + " "
                    + Constants.BOLD + " ";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Given an array of query strings, return an array of search-result counts.
     */

    public long[] getResultCounts(String[] queries) throws IOException,
            MalformedURLException, SocketTimeoutException {
        WebSearcher ws = new WebSearcher();
        ws.setSafeSearch(WebSearcher.SafeSearch.NONE);
        long[] counts = new long[queries.length];
        for (int i = 0; i < queries.length; i++) {
            if (queries[i].matches("\\s*")) { // if string is empty
                counts[i] = -1;
            } else {
                SearchResponse<?> srs = ws.search(queries[i]);
                if (srs.statusNormal())
                    counts[i] = srs.getEstimatedResultCount();
                else
                    counts[i] = -1; // error at google
            }
        }
        return counts;
    }

    /**
     * Given an array of long, return an array of int containing the index of the
     * largest element (or elements, in case of a tie).
     *
     */
    public int[] getWinners(long[] scores) {
        int[] indices = new int[scores.length];
        if (indices.length == 0)
            return indices;
        indices[0] = 0;
        int lastIndex = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0)
                if (scores[i] > scores[indices[0]]) { // new high
                    indices[0] = i;
                    lastIndex = 0;
                } else if (scores[i] == scores[indices[0]]) { // tie
                    indices[++lastIndex] = i;
                }
        }
        int[] ret = new int[lastIndex + 1];
        if (ret.length > 0)
            System.arraycopy(indices, 0, ret, 0, lastIndex + 1);
        return ret;
    }

    /*
     * String-emitting convenience methods.
     */

    /**
     * Takes a GoogleSearchResultElement and gives you a simple, irc-friendly
     * string representation of it.
     */
    public String simpleResultString(AbstractSearchResult re) {
        if (null == re)
            return noResultString;
        return boldConvert(re.getTitle()) + "  " + re.getUnescapedUrl();
    }

    /**
     * returns an irc-friendly string representation of first search result.
     */
    public String luckyString(String query, boolean safe) throws IOException,
            SocketTimeoutException, MalformedURLException {
        WebSearcher ws = new WebSearcher();
        if (!safe)
            ws.setSafeSearch(WebSearcher.SafeSearch.NONE);
        SearchResponse<?> srs = ws.search(query);
        String ret = "";
        if (srs.statusNormal())
            if (srs.getResponseData().getResults().length > 0)
                ret = simpleResultString(srs.getResponseData().getResults()[0]);
            else
                ret = noResultString + " for \""
                        + StringUtil.removeFormattingAndColors(query) + "\"";
        else
            ret = "Error at Google:  " + srs.getResponseStatus() + ", "
                    + srs.getResponseDetails();
        return ret;
    }

    public String luckyString(String query) throws IOException,
            SocketTimeoutException, MalformedURLException {
        return luckyString(query, false);
    }

    /**
     * Convert html &lt;b&gt; tags in a string to irc BOLD formatting characters
     */
    public String boldConvert(String s) {
        return s.replaceAll("<[/ ]*[bB] *>", Constants.BOLD);
    }

    /**
     * Return the estimated sexiness of the given string.
     * <p>
     * You should probably clean up your query and quote it before you pass it
     * to this method. Like with quoteAndClean(), say.
     *
     * @param query
     *            your search string.
     * @return a float between 0 and 1, usually, but sometimes more than 1. -1
     *         if google returns no results for your query.
     * @see quoteAndClean()
     */
    public static float sexiness(String query) throws IOException,
            MalformedURLException, SocketTimeoutException {

        WebSearcher ws = new WebSearcher();
        ws.setSafeSearch(WebSearcher.SafeSearch.NONE);

        SearchResponse<?> plainResult = ws.search(query);
        if (null == plainResult || !plainResult.statusNormal()
                || plainResult.getEstimatedResultCount() < 1)
            return -1;
        SearchResponse<?> sexResult = ws.search(query + " sex");
        // debug
        // System.out.println(sexResult.getEstimatedTotalResultsCount()) ;
        // System.out.println(plainResult.getEstimatedTotalResultsCount()) ;
        float ret = -1;
        if (sexResult != null && sexResult.statusNormal())
            ret = (float) sexResult.getEstimatedResultCount()
                    / (float) plainResult.getEstimatedResultCount();
        return ret;
    }

    public static float gayness(String query) throws MalformedURLException,
            IOException, SocketTimeoutException {
        WebSearcher ws = new WebSearcher();
        ws.setSafeSearch(WebSearcher.SafeSearch.NONE);

        SearchResponse<?> plainResult = ws.search(query);
        if (null == plainResult || plainResult.getEstimatedResultCount() < 1)
            return -1;
        SearchResponse<?> gayResult = ws.search(query + " gay");
        float ret = -1;
        if (null != gayResult && gayResult.statusNormal())
            ret = (float) gayResult.getEstimatedResultCount()
                    / (float) plainResult.getEstimatedResultCount();
        return ret;
    }

    /*
     * main() replaced with junit class in src/goat/test
     *
     * public static void main(String[] args) { Google g = new Google() ; try {
     * System.out.println(g.luckyString("goat fucker")) ;
     * System.out.println("\"goat\" is " + (int) (100.0 * g.sexiness("goat")) +
     * "% sexy") ; System.out.println("\"asdfsaidfyuwcv129038\" is " + (int)
     * (100.0 * g.sexiness("asdfsaidfyuwcv129038")) + "% sexy") ;
     * System.out.println(GoatGoogle.numSearchesToday() +
     * " searches in the past 24 hours.") ; } catch (GoogleSearchFault f) {
     * System.out.println(f.toString()) ; } }
     */
}
