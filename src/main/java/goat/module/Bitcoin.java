package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.Users;
import goat.util.CommandParser;
import goat.util.MtGox;

import java.io.*;
import java.net.* ;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.net.SocketTimeoutException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.DecimalFormat;
import java.util.HashMap;

public class Bitcoin extends Module {

  /**
	 * Returns the specified bitcoin quote.
	 * A hodgepodge of copied code and googled examples.
	 */
    
    // set up some constants
    private static ArrayList<String> columns = new ArrayList<String>();
    private static HashMap<String,String> symbols = new HashMap<String,String>();
    {
        columns.add("volume");
        columns.add("bid");
        columns.add("high");
        columns.add("currency_volume");
        columns.add("ask");
        columns.add("close");
        columns.add("avg");
        columns.add("low");

        // This list is based on popular exchanges as of 2012-12-17, will most likely need periodic updates.
        symbols.put("USD", new String("mtgoxUSD"));
        symbols.put("CAD", new String("virtexCAD"));
        symbols.put("AUD", new String("cryptoxAUD"));
        symbols.put("CHF", new String("ruxumCHF"));
        symbols.put("EUR", new String("mtgoxEUR"));
        symbols.put("GBP", new String("mtgoxGBP"));
        symbols.put("JPY", new String("mtgoxJPY"));
        symbols.put("NZD", new String("mtgoxNZD"));
        symbols.put("PLN", new String("mtgoxPLN"));
        symbols.put("SEK", new String("kptnSEK"));
        symbols.put("SLL", new String("virwoxSLL"));
    }

    
	private long lastCall = 0;
	private JSONArray lastQuotes;

	public boolean isThreadSafe() {
		return false;
	}

	public void processChannelMessage(Message m) {
	    if(m.getModTrailing().startsWith("help"))
	        m.reply("usage: goxlag|bitcoin [column=COLUMN] [currency=CURRENCY] [symbol=SYMBOL] {available columns: volume, bid, high, currency_volume, ask, close, avg, low; available currencies: AUD, CAD, CHF, EUR, GBP, JPY, NZD, PLN, SEK, SLL, USD; if both currency and symbol are specified, symbol overrides currency; results cached for 15 minutes}");
	    else if("goxlag".equalsIgnoreCase(m.getModCommand()))
	        m.reply(goxLag());
	    else
	        ircQuote(m);	    
	}

	private String goxLag() {
	    String ret = "My programmers are awful.";
	    MtGox gox = new MtGox();
	    try {
	        JSONObject lag = gox.insecureApiCall("generic/order/lag");
	        if (lag.has("result") && lag.getString("result").equals("success"))
	            ret = lag.getJSONObject("return").getString("lag_text");
	        else
	            ret = "MtGox error: " + lag.getString("error");
	    } catch (JSONException e) {
	        ret = "I had a JSON problem: " + e.getMessage();
	    }
	    return ret;
	}
	
	public void ircQuote(Message m) {
		//System.out.println("Entering bitcoin ircQuote");
		String userCurrency = "";
		Users users = goat.Goat.getUsers();
		if(users.hasUser(m.getSender()))
			userCurrency = users.getUser(m.getSender()).getCurrency();
		if (userCurrency.equals(""))
			userCurrency = "GBP"; // goat is UKian

        TimeZone tz = null;
        if(users.hasUser(m.getSender()))
            tz = users.getUser(m.getSender()).getTimeZone();

		CommandParser parser = new CommandParser(m.getModTrailing());
				
	    String column = "close";
		if (parser.hasVar("column") && columns.contains(parser.get("column")))
		    column = parser.get("column");

        String symbol = "mtgoxUSD";

		if (parser.hasVar("symbol"))
		    symbol = parser.get("symbol");
		else if (parser.hasVar("currency") && symbols.containsKey(parser.get("currency").toUpperCase()))
            symbol = symbols.get(parser.get("currency").toUpperCase());
        else if (symbols.containsKey(userCurrency))
            symbol = symbols.get(userCurrency);

		try {
		    JSONArray quotes;
		    if (tooSoon()) {
		        quotes = lastQuotes;
		    } else {
		        quotes = btchartQuotes();
		        if (quotes == null) {
		            m.reply("Problem getting bitcoincharts.com API");
		            return;
		        }
		        lastQuotes = quotes;
		        lastCall = System.currentTimeMillis();
		    }
		    JSONObject quote = null;

		    for (int i=0;i < quotes.length();i++) {
		        if (symbol.equals(quotes.getJSONObject(i).getString("symbol"))){
		            quote = quotes.getJSONObject(i);
		            break;
		        }
		    }
		    m.reply(formatQuote(quote, column, tz));
		} catch (JSONException e) {
		    m.reply("Someone didn't program the JSON properly:  " + e.getMessage());
		    e.printStackTrace();
		} catch (SocketTimeoutException e) {
            m.reply("I got bored waiting for the bitcoins to talk to me.");
		} catch (MalformedURLException e) {
		    m.reply("My programmers don't know how to write a URL.");
		    e.printStackTrace();
		} catch (ProtocolException e) {
	        m.reply("Network buggery:  " + e.getMessage());
		    e.printStackTrace();
		} catch (IOException e) {
	        m.reply("I/O is hard, let's go shopping.  " + e.getMessage());
		    e.printStackTrace();
		}
	}

	private String formatQuote(JSONObject quote, String column, TimeZone tz) throws JSONException {

	    String ret = "My programmers are horrible";

	    String symbol = quote.getString("symbol");
	    double price = quote.getDouble(column);
	    long trade_t = quote.getLong("latest_trade");

	    if (trade_t != 0) {
	        Date date = new Date();
	        date.setTime(trade_t*1000);
	        double price_fmt = Double.parseDouble(new DecimalFormat("#.##").format(price));
	        String time_fmt = compactDate(date,tz);
	        String lag = "";
	        if(symbol.startsWith("mtgox")) {
	            MtGox gox = new MtGox();
	            JSONObject lago = gox.insecureApiCall("generic/order/lag");
	            if(lago.has("result")
	                    && lago.getString("result").equals("success")
	                    && lago.getJSONObject("return").getDouble("lag_secs") > 2.0)
	                lag = "  (lag: " + lago.getJSONObject("return").getString("lag_text") + ")";
	        }
	        ret = time_fmt + " " + symbol + " " + price_fmt + lag;
	    } else {
	        ret = "Unable to locate that symbol";
	    }
	    return ret;
	}
	
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
		return new String[]{"bitcoin", "buttcoin", "goxlag"};
	}

    private boolean tooSoon() {
	if ((System.currentTimeMillis() - lastCall) < 30000)
		return true;
	else
		return false;
	}

    private JSONArray btchartQuotes() 
            throws SocketTimeoutException, MalformedURLException, 
            ProtocolException, IOException, JSONException {
        
        HttpURLConnection connection = null;

        URL bitcoincharts = new URL("http://bitcoincharts.com/t/markets.json");
        connection = (HttpURLConnection) bitcoincharts.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new ProtocolException("bitcoincharts.com HTTP error: " + connection.getResponseCode());
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while((line = reader.readLine())!=null)
            builder.append(line);

        String content = builder.toString();

        if (connection!=null) 
            connection.disconnect();

        return new JSONArray(content);
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
