package goat.util;

import goat.core.Constants;
import static goat.util.StringUtil.compactDate;

import java.io.*;
import java.net.*;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONObject;
import org.json.JSONException;


public class BitcoinAverage {

    public String quote(TimeZone tz, String currency)
        throws SocketTimeoutException, MalformedURLException,
               ProtocolException, IOException {
        JSONObject quotes = fetch();
        JSONObject quote = null;
        String ret = "";
        try {
            quote = quotes.getJSONObject(currency);
        } catch (JSONException e) {
            try {
                ret += "Couldn't find \"" + currency + "\" in bitcoinaverage.com JSON, switching to GBP.  ";
                quote = quotes.getJSONObject("GBP");
            } catch (JSONException e2) {
                return ret + "Couldn't find GBP in bicoinaverage.com JSON.";
            }
        }
        return ret + format(quote, tz, currency);
    }

    private JSONObject fetch()
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

    private String format(JSONObject quote, TimeZone tz, String currency) {
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
}
