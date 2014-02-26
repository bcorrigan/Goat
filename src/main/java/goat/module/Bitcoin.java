package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.Users;
import goat.core.Constants;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Set;

public class Bitcoin extends Module {

    /**
     * Returns the specified bitcoin quote.
     * A hodgepodge of copied code and googled examples.
     */

    // set up some constants
    private static ArrayList<String> columns = new ArrayList<String>();
    private static LinkedHashMap<String,String> symbols = new LinkedHashMap<String,String>();
    {
        columns.add("volume");
        columns.add("bid");
        columns.add("high");
        columns.add("currency_volume");
        columns.add("ask");
        columns.add("close");
        columns.add("avg");
        columns.add("low");

        // This list last updated 2013-04-28
        symbols.put("AUD", "mtgoxAUD");
        symbols.put("CAD", "virtexCAD");
        symbols.put("CHF", "mtgoxCHF");
        symbols.put("CNY", "btcnCNY");
        symbols.put("DKK", "mtgoxDKK");
        symbols.put("EUR", "mtgoxEUR");
        symbols.put("GBP", "mtgoxGBP");
        symbols.put("HKD", "mtgoxHKD");
        symbols.put("ILS", "bit2cILS");
        symbols.put("JPY", "mtgoxJPY");
        symbols.put("NZD", "mtgoxNZD");
        symbols.put("PLN", "bitcurexPLN");
        symbols.put("RUB", "btceRUR");
        symbols.put("SEK", "kptnSEK");
        symbols.put("SGD", "mtgoxSGD");
        symbols.put("SLL", "virwoxSLL");
        symbols.put("THB", "mtgoxTHB");
        symbols.put("USD", "mtgoxUSD");
        symbols.put("XRP", "rippleXRP");
    }


    private long lastCall = 0;
    private JSONArray lastQuotes;

    public boolean isThreadSafe() {
        return false;
    }

    public void processChannelMessage(Message m) {
        if(m.getModTrailing().startsWith("help"))
            m.reply("usage: goxlag|bitcoin [help]  " +
                    "[column={volume, bid, high, currency_volume, ask, close, avg, low}]  " +
                    "[currency={"+ keysToOrderedString(symbols.keySet()) +"}] " +
                    "[symbol={see http://bitcoincharts.com/markets for list}]  " +
                    "  If both currency and symbol are specified, symbol overrides currency. " +
                    "Non-mtgox results cached for 30 seconds");
        else if("goxlag".equalsIgnoreCase(m.getModCommand()))
            m.reply(goxLag());
        else
            ircQuote(m);
    }

    private String keysToOrderedString(Set<String> symbols) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = symbols.iterator();
        while (iter.hasNext())
            sb.append(iter.next() + " ");
        return sb.toString();
    }

    private String goxLag() {
        String ret = "My programmers are awful.";
        MtGox gox = new MtGox();
        try {
            JSONObject lag = gox.apiCall("generic/order/lag");
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
        //Make bitcoinaverage.com default source.
        //String symbol = "mtgoxUSD";
        String symbol = "btcavg";

        // Only use symbol if specifically requested otherwise fall back to currency and BitcoinAverage quote.
        if (parser.hasVar("symbol"))
            symbol = parser.get("symbol");
        else if (parser.hasVar("currency") && symbols.containsKey(parser.get("currency").toUpperCase()))
            userCurrency = parser.get("currency").toUpperCase();
        /*
        else if (symbols.containsKey(userCurrency))
            symbol = symbols.get(userCurrency);
        */

        try {
            if (symbol.startsWith("mtgox")) {
                String currency = symbol.substring(5);
                JSONObject quote = new MtGox().apiCall("BTC" + currency + "/ticker");
                if (quote.has("result") && quote.getString("result").equals("success"))
                    m.reply(formatGoxQuote(quote.getJSONObject("return"), column, tz));
                else
                    m.reply("MtGox error: " + quote.getString("error"));
            } else if (symbol.startsWith("btcavg")) {
                JSONObject quotes = btcavgQuotes();
                JSONObject quote = null;
                try {
                    quote = quotes.getJSONObject(userCurrency);
                } catch (JSONException e) {
                    try {
                        quote = quotes.getJSONObject("GBP");
                    } catch (JSONException e2) {
                        m.reply("Can't find currency in BitcoinAverage.com JSON");
                    }
                }
                //m.reply(quote.toString());
                m.reply(formatBtcAvgQuote(quote, tz, userCurrency));
            } else {
                JSONArray quotes;
                if (tooSoon(symbol)) {
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
            }
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
            Date date = new Date(trade_t * 1000);
            double price_fmt = Double.parseDouble(new DecimalFormat("#.##").format(price));
            String time_fmt = compactDate(date,tz);
            ret = time_fmt + " " + symbol + " " + price_fmt + " (via bitcoincharts)";
        } else {
            ret = "Unable to locate that symbol";
        }
        return ret;
    }

    private String formatGoxQuote(JSONObject quote, String column, TimeZone tz) throws JSONException {

        String ret = "My programmers are horrible";


        MtGox gox = new MtGox();

        String price_fmt;
        switch(column) {
        case "close": price_fmt = quote.getJSONObject("last_local").getString("display");
            break;
        case "volume": price_fmt = "volume: " + quote.getJSONObject("vol").getString("display");
            break;
        case "high": price_fmt = "high: " + quote.getJSONObject("high").getString("display");
            break;
        case "low": price_fmt = "low: " + quote.getJSONObject("low").getString("display");
            break;
        case "bid": price_fmt = "bid: " + quote.getJSONObject("buy").getString("display");
            break;
        case "ask": price_fmt = "ask: " + quote.getJSONObject("sell").getString("display");
            break;
        case "avg": price_fmt = "avg: " + quote.getJSONObject("avg").getString("display");
            break;
        case "currency_volume":
            return "my programmers are too lazy to figure that out";
        default:
            return "unknown column: " + column;
        }

        Date date = new Date(quote.getLong("now") / 1000); // Gox gives us microseconds
        String time_fmt = compactDate(date,tz);
        String symbol = quote.getJSONObject("last_local").getString("currency");

        String lag = " (MtGox)";
        JSONObject lago = gox.apiCall("generic/order/lag");
        if(lago.has("result")
           && lago.getString("result").equals("success")
           && lago.getJSONObject("return").getDouble("lag_secs") > 1.0)
            lag = " (MtGox lag: " + lago.getJSONObject("return").getString("lag_text") + ")";

        ret = price_fmt + "  " + time_fmt + " " + lag;
        return ret;
    }

    private String formatBtcAvgQuote(JSONObject quote, TimeZone tz, String currency) {
        String ret = "My programmers are horrible";
        double price = quote.getDouble("last");
        double avg = quote.getDouble("24h_avg");
        String timestamp = quote.getString("timestamp");  //RFC 1123 Format
        SimpleDateFormat df = new SimpleDateFormat("EEEE, d MMMM yyyy, hh:mma z");
        Date date;
        try {
            date = df.parse(timestamp);
        } catch (ParseException e) {
            date = new Date();
        }
        String date_fmt = compactDate(date, tz);
        double price_fmt = Double.parseDouble(new DecimalFormat("#.##").format(price));
        ret = date_fmt + " " + currency + " " + price_fmt + " ";
        double change = price - avg;
        double percentage_change = ((change)/avg)*100;
        ret += "(";
        if(change > 0)
            ret += "+";
        if(change < 0)
            ret += Constants.RED;
        if(percentage_change != 0)
            ret += Double.parseDouble(new DecimalFormat("#.##").format(percentage_change)) + "%)";
        if(change < 0)
            ret += Constants.NORMAL;
        ret += " (via bitcoinaverage)";
        return ret;

    }


    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
        return new String[]{"bitcoin", "buttcoin", "goxlag"};
    }

    private boolean tooSoon(String symbol) {
        if (symbol.startsWith("mtgox"))
            return false; // it's never too soon at MtGox.  Even if they claim it's 30 seconds.
	if ((System.currentTimeMillis() - lastCall) < 30000)
            return true;
	else
            return false;
    }

    private JSONArray btchartQuotes()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        HttpURLConnection connection = null;

        URL bitcoincharts = new URL("http://bitcoincharts.com/v1/markets.json");
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

    private JSONObject btcavgQuotes()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        HttpURLConnection connection = null;

        URL bitcoinavg = new URL("https://api.bitcoinaverage.com/ticker/all");
        connection = (HttpURLConnection) bitcoinavg.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new ProtocolException("bitcoinaverage.com HTTP error: " + connection.getResponseCode());
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while((line = reader.readLine())!=null)
            builder.append(line);

        String content = builder.toString();

        if (connection!=null)
            connection.disconnect();

        return new JSONObject(content);
    }


    private String compactDate(Date date, TimeZone tz) {
    	if (null == tz)
            tz = TimeZone.getDefault();
        Date now = new Date();
        String formatString = "hh:mm:ss zzz";  // default format for recent quotes (less than one hour)
        if(now.getTime() - date.getTime() > 1000*60*60*24*365) // more than one year ago, roughly
            formatString = "d MMM yyyy";
        else if(now.getTime() - date.getTime() > 1000*60*60*24*2) // more than two days ago, less than a year
            formatString = "d MMM zzz";
        else if(now.getTime() - date.getTime() > 1000*60*60*24) // between one and two days ago
            formatString = "d MMM haa zzz";
        else if(now.getTime() - date.getTime() > 1000*60*60) // between one hour and one day ago
            formatString = "h:mmaa zzz";

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
