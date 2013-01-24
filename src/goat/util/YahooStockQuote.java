package goat.util;


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import com.csvreader.CsvReader;

public class YahooStockQuote {

	/*
	 * Field request flags:

	a	 Ask	 
	a2	 Average Daily Volume	 
	a5	 Ask Size
	b	 Bid	 
	b2	 Ask (Real-time)	 
	b3	 Bid (Real-time)
	b4	 Book Value	 
	b6	 Bid Size	 
	c	 Change & Percent Change
	c1	 Change	 
	c3	 Commission	 
	c6	 Change (Real-time)
	c8	 After Hours Change (Real-time)	 
	d	 Dividend/Share	 
	d1	 Last Trade Date
	d2	 Trade Date	 
	e	 Earnings/Share	 
	e1	 Error Indication (returned for symbol changed / invalid)
	e7	 EPS Estimate Current Year	 
	e8	 EPS Estimate Next Year	 
	e9	 EPS Estimate Next Quarter
	f6	 Float Shares	 
	g	 Day's Low	 
	h	 Day's High
	j	 52-week Low	 
	k	 52-week High	 
	g1	 Holdings Gain Percent
	g3	 Annualized Gain	 
	g4	 Holdings Gain	 
	g5	 Holdings Gain Percent (Real-time)
	g6	 Holdings Gain (Real-time)	 
	i	 More Info	 
	i5	 Order Book (Real-time)
	j1	 Market Capitalization	 
	j3	 Market Cap (Real-time)	 
	j4	 EBITDA
	j5	 Change From 52-week Low	 
	j6	 Percent Change From 52-week Low	 
	k1	 Last Trade (Real-time) With Time
	k2	 Change Percent (Real-time)	 
	k3	 Last Trade Size	 
	k4	 Change From 52-week High
	k5	 Percent Change From 52-week High	 
	l	 Last Trade (With Time)	 
	l1	 Last Trade (Price Only)
	l2	 High Limit	 
	l3	 Low Limit	 
	m	 Day's Range
	m2	 Day's Range (Real-time)	 
	m3	 50-day Moving Average	 
	m4	 200-day Moving Average
	m5	 Change From 200-day Moving Average	 
	m6	 Percent Change From 200-day Moving Average	 
	m7	 Change From 50-day Moving Average
	m8	 Percent Change From 50-day Moving Average	 
	n	 Name	 
	n4	 Notes
	o	 Open	 
	p	 Previous Close	 
	p1	 Price Paid
	p2	 Change in Percent	 
	p5	 Price/Sales	 
	p6	 Price/Book
	q	 Ex-Dividend Date	 
	r	 P/E Ratio	 
	r1	 Dividend Pay Date
	r2	 P/E Ratio (Real-time)	 
	r5	 PEG Ratio	 
	r6	 Price/EPS Estimate Current Year
	r7	 Price/EPS Estimate Next Year	 
	s	 Symbol	 
	s1	 Shares Owned
	s7	 Short Ratio	 
	t1	 Last Trade Time	 
	t6	 Trade Links
	t7	 Ticker Trend	 
	t8	 1 yr Target Price	 
	v	 Volume
	v1	 Holdings Value	 
	v7	 Holdings Value (Real-time)	 
	w	 52-week Range
	w1	 Day's Value Change	 
	w4	 Day's Value Change (Real-time)	 
	x	 Stock Exchange
	y	 Dividend Yield	
	 */

	private static final String FIELD_FLAGS = "snl1d1t1c1p2oghvrj1e1s7n4j4b4jkf6";
	private static final String YAHOO_URL = "http://quote.yahoo.com/d/quotes.csv?f=" + FIELD_FLAGS + "&e=.csv&s=";

	private static final String ENCODING = "UTF-8";

	public String symbol;
	public String name;
	public Float lastTrade;
	public Date lastTradeTimestamp;
	public Float change;
	public Float percentChange;
	public Float open = null;
	public Float dayHigh = null;
	public Float dayLow = null;
	public Long volume = null;
	public Float priceEarningsRatio = null;
	public Double marketCap = null;
	public String error;
	public Float shortRatio = null;
	public String notes;
	public Double ebitda = null;
	public Double bookValue = null;
	public Float yearLow = 0F; // 52-week low
	public Float yearHigh = 0F; // 52-week high
	public Long floatShares = null;


	private YahooStockQuote() {};

	public static YahooStockQuote getQuote(String tickerSymbol) throws YahooStockQuoteException, SocketTimeoutException {
		YahooStockQuote ret = null;
		ArrayList<YahooStockQuote> quotes = getQuotes(tickerSymbol);
		if(quotes.size() > 0)
			ret = quotes.get(0);
		return ret;
	}
	
	public static ArrayList<YahooStockQuote> getQuotes(String tickerSymbols) throws YahooStockQuoteException, SocketTimeoutException {
		HttpURLConnection connection = null;
		CsvReader cr = null;
		ArrayList<YahooStockQuote> ret = new ArrayList<YahooStockQuote>();
		try {
			URL url = new URL(YAHOO_URL + tickerSymbols.toUpperCase().replaceAll("\\s+", " ").replaceAll(" ", "+"));
			connection = (HttpURLConnection) url.openConnection();
			// incompatible with 1.4
			connection.setConnectTimeout(3000);  //just three seconds to connect, we can't hang around
			connection.setReadTimeout(5000); //only wait 5 seconds to read data
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new YahooStockQuoteException("Invalid ticker symbol(s)");
			}
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new YahooStockQuoteException("Yahoo Finance Server error: " + connection.getResponseCode() + ", " + connection.getResponseMessage());
			}
			cr = new CsvReader(new InputStreamReader(connection.getInputStream(), ENCODING));
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mmaa");
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			while (cr.readRecord()) {
				// System.out.println(cr.getRawRecord());
				final String[] fields = cr.getValues();
				YahooStockQuote quote = new YahooStockQuote();
				try {
					// ideally, these should all be wrapped in their own try/catch blocks to minimize data
					// loss, but eh.
					quote.symbol = fields[0];
					
					quote.error = fields[13];
					
					// When yahoo supplies a symbol-change error, it does so using invalid CSV,
					// so we look at the raw record and deal with it that way.
					if(cr.getRawRecord().contains("Ticker symbol has changed to:")) {
						String symbol = cr.getRawRecord();
						//System.out.println(symbol);
						symbol = symbol.substring(symbol.indexOf("\">") + 2, symbol.indexOf("</a>"));
						// System.out.println(quote.symbol + " has been changed to " + symbol);
						quote = getQuote(symbol);
						if(quote != null)
							ret.add(quote);
						continue;
					}
					quote.name = fields[1];
					quote.lastTrade = Float.parseFloat(fields[2]);
					quote.lastTradeTimestamp = sdf.parse(fields[3] + " " + fields[4].toUpperCase());
					quote.change = Float.parseFloat(fields[5]);
					quote.percentChange = Float.parseFloat(fields[6].replace("%", ""));
					if(! fields[7].equals("N/A"))
						quote.open = Float.parseFloat(fields[7]);
					if(! fields[8].equals("N/A"))
						quote.dayLow = Float.parseFloat(fields[8]);
					if(! fields[9].equals("N/A"))
						quote.dayHigh = Float.parseFloat(fields[9]);
					if(! fields[10].equals("N/A"))
						quote.volume = Long.parseLong(fields[10]);
					if(! fields[11].equals("N/A"))
						quote.priceEarningsRatio = Float.parseFloat(fields[11]);
					if(! fields[12].equals("N/A"))
						quote.marketCap = parseAbbreviatedNumber(fields[12]);
					// moved fields[13] (error) up to be looked at earlier
					if(! fields[14].equals("N/A"))
						quote.shortRatio = Float.parseFloat(fields[14]);
					quote.notes = fields[15];
					if(! fields[16].equals("N/A"))
						quote.ebitda = parseAbbreviatedNumber(fields[16]);
					if(! fields[17].equals("N/A"))
						quote.bookValue = Double.parseDouble(fields[17]);
					if(!fields[18].equals("N/A"))
						quote.yearLow = Float.parseFloat(fields[18]);
					if(!fields[19].equals("N/A"))
					quote.yearHigh = Float.parseFloat(fields[19]);
					if(! fields[20].equals("N/A")) {
						String shares = fields[20];
						// this is ugly.  Yahoo! finance supplies the float as a comma-formatted number, 
						// but they sort of neglect to delimit it as a string by using quotes
						// so any sane CSV parser will think it has, for example, four fields when 
						// it gets something like 7,868,851,000.
						//
						// to get around this, we make the float come last, and then read further
						// fields onto the end of our string until there's no fields left.
						for (int i = 21; i < fields.length; i++)
							shares += fields[i];
						quote.floatShares = Long.parseLong(shares);
					}
					ret.add(quote);
				} catch (NumberFormatException nfe) {
					System.err.println(quote.symbol);
					nfe.printStackTrace();
				} catch (java.text.ParseException pe) {
					System.err.println(quote.symbol);
					pe.printStackTrace();
				}
			}
			cr.close();
			connection.disconnect();
		}
		catch (IOException e) {
			throw new YahooStockQuoteException(e.getMessage());
		}	
		return ret;
	}
	
	
	private static Double parseAbbreviatedNumber(String str) throws NumberFormatException {
		Double multiplier = 1D;
		Double ret = 0D;
		if(str.endsWith("B")) {
			multiplier = 1000000000D;
			str = str.replace("B", "");
		} else if(str.endsWith("M")) {
			multiplier = 1000000D;
			str = str.replace("M", "");
		}
		return multiplier * Double.parseDouble(str);
	}

	
}
