package goat.module;

import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.util.CommandParser;

import java.io.*;
import java.net.* ;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.* ;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.DecimalFormat;

public class Bitcoin extends Module {

  /**
	 * Returns the specified bitcoin quote.
	 * A hodgepodge of copied code and googled examples.
	 */
	private long lastCall = 0;
	private JSONArray lastQuote;

	public boolean isThreadSafe() {
		return false;
	}
	
	public void processChannelMessage(Message m) {
        ircQuote(m);
	}
	
	public void ircQuote(Message m) {
		//System.out.println("Entering bitcoin ircQuote");
                TimeZone tz = null;
                if(Goat.getUsers().hasUser(m.getSender()))
                        tz = Goat.getUsers().getUser(m.getSender()).getTimeZone();
		CommandParser parser = new CommandParser(m.getModTrailing());
		if ("help".equalsIgnoreCase(parser.command())){
			m.reply("usage: bitcoin [column=COLUMN] {available columns: volume, bid, high, currency_volume, ask, close, avg, low} results cached for 15 minutes");
			return;
		}
		String symbol = "mtgoxUSD";
		String column = "avg";
		JSONArray quote;
		double price = 0;
		long trade_t = 0;
		ArrayList<String> columns = new ArrayList<String>();
		columns.add("volume");
		columns.add("bid");
		columns.add("high");
		columns.add("currency_volume");
		columns.add("ask");
		columns.add("close");
		columns.add("avg");
		columns.add("low");
		if(parser.hasVar("column")) {
			if (columns.contains(parser.get("column")))
				column=parser.get("column");
		}
			
		if (tooSoon()) {
			quote = lastQuote;
			//System.out.println("Getting last quote");
		} else {
			//System.out.println("Getting new quote");
			quote = btchartQuote();
			lastQuote = quote;
			lastCall = System.currentTimeMillis();
		}
		try {
			for (int i=0;i < quote.length();i++) {
				JSONObject x = quote.getJSONObject(i);
				//System.out.println(x.toString());
				//System.out.println(x.getString("symbol");
				if (symbol.equals(x.getString("symbol"))){
					price = x.getDouble(column);
					trade_t = x.getLong("latest_trade");
				}
			}
			Date date = new Date();
			date.setTime(trade_t*1000);
			double price_fmt = Double.parseDouble(new DecimalFormat("#.##").format(price));
			String time_fmt = compactDate(date,tz);
			m.reply(time_fmt+" mtgoxUSD: $"+price_fmt);
			
		} catch (JSONException e) {
			m.reply("JSON Error!");
		}
	//	System.out.println("Finished stock quote for channel " + m.channame);
	}

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
		return new String[]{"bitcoin"};
	}

    private boolean tooSoon() {
	if ((System.currentTimeMillis() - lastCall) < 900000)
		return true;
	else
		return false;
	}

    private JSONArray btchartQuote() {
	JSONArray ret;
	HttpURLConnection connection = null;
	try {
		URL bitcoincharts = new URL("http://bitcoincharts.com/t/markets.json");
		connection = (HttpURLConnection) bitcoincharts.openConnection();
		connection.setConnectTimeout(3000);
		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			System.out.println("HTTP not OK");
			return null;
		}
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while((line = reader.readLine())!=null) {
			builder.append(line);
		}
		String content = builder.toString();
		try {
			ret = new JSONArray(content);
		} catch (JSONException je) {
			return null;
		}
		if (connection!=null) connection.disconnect();
	} catch (MalformedURLException e) {
		System.out.println(e.getMessage());
		return null;
	} catch (ProtocolException e) {
		System.out.println(e.getMessage());
		return null;
	} catch (IOException e) {
		System.out.println(e.getMessage());
		return null;
	}
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
    public static void main(String[] args) {
    }
}
