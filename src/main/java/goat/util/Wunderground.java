package goat.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONException;

import goat.core.BotStats;
import goat.util.Passwords;

public class Wunderground {

    public JSONObject apiCall(String method, String query) throws JSONException {
        return apiCall(method, query, true);
    }

    public JSONObject insecureApiCall(String method, String query) throws JSONException {
        return apiCall(method, query, false);
    }

    // this appears to be crud internal to Wunderground, not a standard
    public static final String[] forecastCodes = {
        "", //0
        "Clear",
        "Partly Cloudy",
        "Mostly Cloudy",
        "Cloudy",
        "Hazy", //5
        "Foggy",
        "Very Hot",
        "Very Cold",
        "Blowing Snow",
        "Chance of Showers", //10
        "Showers",
        "Chance of Rain",
        "Rain",
        "Chance of a Thunderstorm",
        "Thunderstorm", //15
        "Flurries",
        "OMITTED",
        "Chance of Snow Showers",
        "Snow Showers",
        "Chance of Snow", //20
        "Snow",
        "Chace of Ice Pellets",
        "Ice Pellets",
        "Blizzard"
    };

    private String apiKey = "";

    private String apiKey() {
        if ("".equals(apiKey))
            apiKey = Passwords.getPassword("wunderground.apikey");
        return apiKey;
    }

    private JSONObject apiCall(String method, String query, boolean secure) throws JSONException {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        String response = "";
        String protocol = "https";
        if (!secure)
            protocol = "http";

        try {
            URL url = new URL(protocol + "://api.wunderground.com/api/" + apiKey() + "/" + method + "/q/" + java.net.URLEncoder.encode(query.trim()) + ".json");
            connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestProperty("User-Agent", "Goat IRC Bot v" +
					  BotStats.getInstance().getVersion() +
					  " (" + System.getProperty("os.name") +
					  " v" + System.getProperty("os.version") + ';'
					  + System.getProperty("os.arch") + ")");
            connection.setConnectTimeout(7000);  // 3 seconds.
            connection.setReadTimeout(7000);  // 3 seconds.
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new JSONObject("{\"error\":\"Weather Underground gave me a " + connection.getResponseCode() + " :(\"}");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "" ;
            while ((inputLine = in.readLine()) != null)
                response += inputLine;

        } catch  (SocketTimeoutException e) {
            response = "{\"error\":\"I got bored waiting for Weather Underground.\"}" ;
        } catch (IOException e) {
            e.printStackTrace();
            response = "{\"error\":\"I had an I/O problem when trying to talk to Weather Underground :(\"}";
        } finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.err.println("Wunderground: Could not close input stream!");
                ioe.printStackTrace();
            }
        }
        return new JSONObject(response);
    }
}
