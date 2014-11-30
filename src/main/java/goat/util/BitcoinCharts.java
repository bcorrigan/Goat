package goat.util;

import static goat.util.StringUtil.compactDate;

import java.io.*;
import java.net.* ;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;
import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


public class BitcoinCharts {


    public String currencies() {
        String currencies;
        try {
            currencies = keysToOrderedString(getSymbols().keySet());
        } catch (Exception e) {
            currencies = "<error fetching currencies>";
            System.err.println("Error building currency list from bitcoincharts: \n\n" + e.getMessage());
        }
        return currencies;
    }

    public boolean hasCurrency(String cur) {
        boolean ret = false;
        try {
            ret = getSymbols().containsKey(cur.toUpperCase());
        } catch (Exception e) {}
        return ret;
    }

    public String quote(String symbol, String column, TimeZone tz)
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        JSONArray quotes = fetch();
        JSONObject quote = null;
        if (quotes == null)
            return "Problem getting bitcoincharts.com API";

        for (int i=0;i < quotes.length();i++) {
            if (symbol.equals(quotes.getJSONObject(i).getString("symbol"))) {
                quote = quotes.getJSONObject(i);
                break;
            }
        }
        return format(quote, column, tz);
    }

    private long lastCall = 0;
    private JSONArray lastQuotes;
    private static List<String> columns = Arrays.asList(new String[]{"volume", "bid", "high", "currency_volume", "ask", "close", "avg", "low"});

    private boolean tooSoon() {
	if ((System.currentTimeMillis() - lastCall) < 30000)
            return true;
	else
            return false;
    }

    private LinkedHashMap<String, String> getSymbols()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        LinkedHashMap<String, String> symbols = new LinkedHashMap<String, String>();
        HashMap<String, Double> volumes = new HashMap<String, Double>();
        JSONArray quotes = fetch();
        JSONObject quote;
        String currency;
        String market;
        Double volume = 0.0;
        for(int i = 0; i < quotes.length(); i++) {
            quote = quotes.getJSONObject(i);
            currency = quote.getString("currency");
            market = quote.getString("symbol");
            volume = quote.getDouble("volume");
            if ( !volumes.containsKey(currency) || volumes.get(currency) < volume ) {
                symbols.put(currency, market);
                volumes.put(currency, volume);
                symbols.put(currency, market);
            }
        }
        return symbols;
    }

    private String keysToOrderedString(Set<String> symbols) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = symbols.iterator();
        while (iter.hasNext())
            sb.append(iter.next() + " ");
        return sb.toString();
    }

    private JSONArray fetch()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        JSONArray quotes;
        if (tooSoon()) {
            quotes = lastQuotes;
        } else {
            quotes = forceFetch();
            lastQuotes = quotes;
            lastCall = System.currentTimeMillis();
        }
        return quotes;
    }

    private String format(JSONObject quote, String column, TimeZone tz) throws JSONException {

        String ret = "My programmers are horrible";
        String symbol = quote.getString("symbol");
        double price = quote.getDouble(column);
        long trade_t = quote.getLong("latest_trade");


        if(! columns.contains(column))
            return "I don't know what you mean by \"" + column + "\".";

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

    private JSONArray forceFetch()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        HttpURLConnection connection = null;

        URL bitcoincharts = new URL("http://api.bitcoincharts.com/v1/markets.json");
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

}
