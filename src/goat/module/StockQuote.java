package goat.module;

import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.util.YahooStockQuote;
import goat.util.YahooStockQuoteException;

import java.net.SocketTimeoutException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class StockQuote extends Module {

	/**
	 * Returns the specified stock quote.
	 */
	
	public boolean isThreadSafe() {
		return false;
	}
	
	public void processChannelMessage(Message m) {
        ircQuote(m);
	}
	
	public void ircQuote(Message m) {
		// System.out.println("getting stock quote in channel " + m.channame);
		TimeZone tz = null;
		if(Goat.getUsers().hasUser(m.getSender()))
			tz = Goat.getUsers().getUser(m.getSender()).getTimeZone();
		try {
			ArrayList<YahooStockQuote> quotes = YahooStockQuote.getQuotes(m.getModTrailing());
			if(quotes.size() < 1) {
				m.reply("You didn't give me any valid ticker symbols.  Look up symbols here:  http://finance.yahoo.com/lookup");
				return;
			} else if(quotes.size() > 5) {
				String reply = "";
				for(int i = 1; i < quotes.size(); i++)
					reply += "  \u00a4 " + shortQuote(quotes.get(i), tz);
				m.pagedReply(reply);
			} else if(quotes.size() > 1) {
				String reply = mediumQuote(quotes.get(0), tz);
				for(int i = 1; i < quotes.size(); i++)
					reply += Constants.BOLD + "  \u00a4\u00a4  " + Constants.NORMAL + mediumQuote(quotes.get(i), tz);
				m.pagedReply(reply);
			} else {
				m.pagedReply(longQuote(quotes.get(0), tz));
			}
		} catch (SocketTimeoutException ste) {
			m.reply("I got bored waiting for yahoo to give me quotes.");
		} catch (YahooStockQuoteException ysqe) {
			m.reply("I had a problem talking to Yahoo:  " + ysqe.getMessage());
			ysqe.printStackTrace();
		}
		//System.out.println("Finished stock quote for channel " + m.channame);
	}

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
		return new String[]{"quote"};
	}
    
    private String longQuote(YahooStockQuote quote, TimeZone tz) {
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = quote.name + " (" + Constants.BOLD + quote.symbol + Constants.NORMAL + "):  ";
    	ret += nf.format(quote.lastTrade) + " ";
    	if(quote.change != 0) {
    		if(quote.change > 0)
    			ret += "+";
    		if(quote.change < 0)
    			ret += Constants.RED;
    		ret += nf.format(quote.change);
    		if(quote.percentChange != 0)
    			ret += " (" + nf.format(quote.percentChange) + "%)";
    		if(quote.change < 0)
    			ret += Constants.NORMAL;
    		ret += ", " + compactDate(quote.lastTradeTimestamp, tz);
    	}
    	if(quote.open != null)
    		ret += " " + Constants.BOLD + "Open: " + Constants.NORMAL + nf.format(quote.open);
    	if(quote.dayLow != null)
    		ret += " "  + Constants.BOLD + "Range: " + Constants.NORMAL + nf.format(quote.dayLow) + " - " + nf.format(quote.dayHigh);
     	if(quote.volume != 0)
    		ret += " " + Constants.BOLD + "Volume: " + Constants.NORMAL + abbreviateNumber(quote.volume);
    	if(quote.marketCap != null)
    		ret += " " + Constants.BOLD + "Market cap: " + Constants.NORMAL + abbreviateNumber(quote.marketCap);
    	if(quote.ebitda != null) 
    		ret += " " + Constants.BOLD + "EBITDA: " + Constants.NORMAL + abbreviateNumber(quote.ebitda);
    	if(quote.priceEarningsRatio != null)
    		ret += " " + Constants.BOLD + "P/E: " + Constants.NORMAL + nf.format(quote.priceEarningsRatio);
    	if(quote.bookValue != null)
    		ret += " " + Constants.BOLD + "Book: " + Constants.NORMAL + nf.format(quote.bookValue);
    	if(quote.yearHigh != 0) 
    		ret += " "  + Constants.BOLD + "52-week Range: " + Constants.NORMAL + nf.format(quote.yearLow) + " - " + nf.format(quote.yearHigh);
    	if(quote.floatShares != null)
    		ret += " " + Constants.BOLD + "Float: " + Constants.NORMAL + abbreviateNumber(quote.floatShares);
    	if(quote.shortRatio != null)
    		ret += " " + Constants.BOLD + "Short Ratio: " + Constants.NORMAL + nf.format(quote.shortRatio);
    	if(! quote.notes.equals("-"))
    		ret += " " + Constants.BOLD + "Notes: " + Constants.NORMAL + quote.notes;
    	return ret;
    }
    
    private String mediumQuote(YahooStockQuote quote, TimeZone tz) {
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = quote.name + " (" + Constants.BOLD + quote.symbol + Constants.NORMAL + "):  ";
    	ret += nf.format(quote.lastTrade) + " (";
    	if(quote.percentChange < 0)
    		ret += Constants.RED;
    	else
    		ret += "+";
    	ret += quote.percentChange + "%";
    	if(quote.percentChange < 0)
    		ret += Constants.NORMAL;
    	ret += "), " + compactDate(quote.lastTradeTimestamp, tz);
    	return ret;
    }
    
    private String shortQuote(YahooStockQuote quote, TimeZone tz) {
    	Date now = new Date();
       	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = Constants.BOLD + quote.symbol + Constants.NORMAL;
    	ret += " " + nf.format(quote.lastTrade) + " (";
    	if(quote.percentChange < 0)
    		ret += Constants.RED;
    	else ret += "+";
    	ret += quote.percentChange + "%";
    	if(quote.percentChange < 0)
    		ret += Constants.NORMAL;
    	ret += ")";
    	if (now.getTime() - quote.lastTradeTimestamp.getTime() > 1000*60*25) // only add time if quote is older than 25 min
    		ret += " " + compactDate(quote.lastTradeTimestamp, tz);
    	return ret;
    }
    
    private String compactDate(Date date, TimeZone tz) {
    	if (null == tz)
    		tz = TimeZone.getDefault();
    	Date now = new Date();
    	String formatString = "h:mmaa zzz";  // default format for recent quotes (less than one day)
    	if(now.getTime() - date.getTime() > 1000*60*60*24*365) // more than one year ago, roughly
    		formatString = "d MMM yyyy";
    	else if(now.getTime() - date.getTime() > 1000*60*60*24*2) // more than two days ago, less than a year
    		formatString = "d MMM zzz";
    	else if(now.getTime() - date.getTime() > 1000*60*60*24) // between one and two days ago
    		formatString = "d MMM haa zzz";
    	SimpleDateFormat sdf = new SimpleDateFormat(formatString);
    	sdf.setTimeZone(tz);
    	return sdf.format(date).replace("AM ", "am ").replace("PM ", "pm ");
    }
    
    private String abbreviateNumber(Long number) {
    	return abbreviateNumber((double) number);
    }
  
    private String abbreviateNumber(Double number) {
       	String suffix = "";
    	Double divisor = 1D;
    	if(Math.abs(number) > 1000000000000L) {
    		suffix = "T";
    		divisor = 1000000000000D;
    	} else if (Math.abs(number) > 1000000000) {
    		suffix = "B";
    		divisor = 1000000000D;
    	} else if (Math.abs(number) > 1000000) {
    		suffix = "M";
    		divisor = 1000000D;
    	} else if (Math.abs(number) > 1000) {
    		suffix = "K";
    		divisor = 1000D;
    	}
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	return nf.format(number / divisor) + suffix;
    }
   
}
