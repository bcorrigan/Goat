package goat.module;

import goat.Goat;
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
	
	public void processChannelMessage(Message m) {
        ircQuote(m);
	}
	
	public void ircQuote(Message m) {
		TimeZone tz = null;
		if(Goat.getUsers().hasUser(m.sender))
			tz = Goat.getUsers().getUser(m.sender).getTimeZone();
		try {
			ArrayList<YahooStockQuote> quotes = YahooStockQuote.getQuotes(m.modTrailing);
			if(quotes.size() < 1) {
				m.createReply("You didn't give me any valid ticker symbols.  Look up symbols here:  http://finance.yahoo.com/lookup").send();
				return;
			} else if(quotes.size() > 5) {
				String reply = "";
				for(int i = 1; i < quotes.size(); i++)
					reply += "  \u00a4 " + shortQuote(quotes.get(i), tz);
				m.createPagedReply(reply).send();
			} else if(quotes.size() > 1) {
				String reply = mediumQuote(quotes.get(0), tz);
				for(int i = 1; i < quotes.size(); i++)
					reply += Message.BOLD + "  \u00a4\u00a4  " + Message.NORMAL + mediumQuote(quotes.get(i), tz);
				m.createPagedReply(reply).send();
			} else {
				m.createPagedReply(longQuote(quotes.get(0), tz)).send();
			}
		} catch (SocketTimeoutException ste) {
			m.createReply("I got bored waiting for yahoo to give me quotes.").send();
		} catch (YahooStockQuoteException ysqe) {
			m.createReply("I had a problem talking to Yahoo:  " + ysqe.getMessage());
			ysqe.printStackTrace();
		}
	}

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public static String[] getCommands() {
		return new String[]{"quote"};
	}
    
    private String longQuote(YahooStockQuote quote, TimeZone tz) {
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = quote.name + " (" + Message.BOLD + quote.symbol + Message.NORMAL + "):  ";
    	ret += nf.format(quote.lastTrade) + " ";
    	if(quote.change != 0) {
    		if(quote.change > 0)
    			ret += "+";
    		if(quote.change < 0)
    			ret += Message.RED;
    		ret += nf.format(quote.change);
    		if(quote.percentChange != 0)
    			ret += " (" + nf.format(quote.percentChange) + "%)";
    		if(quote.change < 0)
    			ret += Message.NORMAL;
    		ret += ", " + compactDate(quote.lastTradeTimestamp, tz);
    	}
    	if(quote.open != null)
    		ret += " " + Message.BOLD + "Open: " + Message.NORMAL + nf.format(quote.open);
    	if(quote.dayLow != null)
    		ret += " "  + Message.BOLD + "Range: " + Message.NORMAL + nf.format(quote.dayLow) + " - " + nf.format(quote.dayHigh);
     	if(quote.volume != 0)
    		ret += " " + Message.BOLD + "Volume: " + Message.NORMAL + abbreviateNumber(quote.volume);
    	if(quote.marketCap != null)
    		ret += " " + Message.BOLD + "Market cap: " + Message.NORMAL + abbreviateNumber(quote.marketCap);
    	if(quote.ebitda != null) 
    		ret += " " + Message.BOLD + "EBITDA: " + Message.NORMAL + abbreviateNumber(quote.ebitda);
    	if(quote.priceEarningsRatio != null)
    		ret += " " + Message.BOLD + "P/E: " + Message.NORMAL + nf.format(quote.priceEarningsRatio);
    	if(quote.bookValue != null)
    		ret += " " + Message.BOLD + "Book: " + Message.NORMAL + nf.format(quote.bookValue);
    	if(quote.yearHigh != 0) 
    		ret += " "  + Message.BOLD + "52-week Range: " + Message.NORMAL + nf.format(quote.yearLow) + " - " + nf.format(quote.yearHigh);
    	if(quote.floatShares != null)
    		ret += " " + Message.BOLD + "Float: " + Message.NORMAL + abbreviateNumber(quote.floatShares);
    	if(quote.shortRatio != null)
    		ret += " " + Message.BOLD + "Short Ratio: " + Message.NORMAL + nf.format(quote.shortRatio);
    	if(! quote.notes.equals("-"))
    		ret += " " + Message.BOLD + "Notes: " + Message.NORMAL + quote.notes;
    	return ret;
    }
    
    private String mediumQuote(YahooStockQuote quote, TimeZone tz) {
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = quote.name + " (" + Message.BOLD + quote.symbol + Message.NORMAL + "):  ";
    	ret += nf.format(quote.lastTrade) + " (";
    	if(quote.percentChange < 0)
    		ret += Message.RED;
    	else
    		ret += "+";
    	ret += quote.percentChange + "%";
    	if(quote.percentChange < 0)
    		ret += Message.NORMAL;
    	ret += "), " + compactDate(quote.lastTradeTimestamp, tz);
    	return ret;
    }
    
    private String shortQuote(YahooStockQuote quote, TimeZone tz) {
    	Date now = new Date();
       	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(2);
    	String ret = Message.BOLD + quote.symbol + Message.NORMAL;
    	ret += " " + nf.format(quote.lastTrade) + " (";
    	if(quote.percentChange < 0)
    		ret += Message.RED;
    	else ret += "+";
    	ret += quote.percentChange + "%";
    	if(quote.percentChange < 0)
    		ret += Message.NORMAL;
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
