package goat.util;

import java.io.*;
import java.net.* ;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


public class BitcoinCharts {

    private long lastCall = 0;
    private JSONArray lastQuotes;

    private boolean tooSoon() {
	if ((System.currentTimeMillis() - lastCall) < 30000)
            return true;
	else
            return false;
    }

    public LinkedHashMap<String, String> getSymbols()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        LinkedHashMap<String, String> symbols = new LinkedHashMap<String, String>();
        HashMap<String, Double> volumes = new HashMap<String, Double>();
        JSONArray quotes = fetchQuotes();
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

    public JSONArray fetchQuotes()
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException, JSONException {

        JSONArray quotes;
        if (tooSoon()) {
            quotes = lastQuotes;
        } else {
            quotes = forceFetchQuotes();
            lastQuotes = quotes;
            lastCall = System.currentTimeMillis();
        }
        return quotes;
    }

    private JSONArray forceFetchQuotes()
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
