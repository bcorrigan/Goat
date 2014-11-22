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

public class DOGeCoin {
    public JSONObject apiCall(String method) throws JSONException {
        return apiCall(method, true);
    }

    public JSONObject insecureApiCall(String method) throws JSONException {
        return apiCall(method, false);
    }

    private JSONObject apiCall(String method, boolean secure) throws JSONException {

        HttpURLConnection connection = null;
        BufferedReader in = null;
        String response = "";
        String protocol = "https";
        if (!secure)
            protocol = "http";

        try {
            URL url = new URL(protocol + "://pubapi.cryptsy.com/api.php?method=singlemarketdata&marketid=182");
            connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestProperty("User-Agent", "Goat IRC Bot v" +
					  BotStats.getInstance().getVersion() +
					  " (" + System.getProperty("os.name") +
					  " v" + System.getProperty("os.version") + ';'
					  + System.getProperty("os.arch") + ")");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new JSONObject("{\"error\":\"DOGeCoin.org gave me a " + connection.getResponseCode() + " :(\"}");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "" ;
            while ((inputLine = in.readLine()) != null)
                response += inputLine;

        } catch  (SocketTimeoutException e) {
            response = "{\"error\":\"I got bored waiting for Dog eCoin.\"}" ;
        } catch (IOException e) {
            e.printStackTrace();
            response = "{\"error\":\"I had an I/O problem when trying to talk to DOGeCoin.org :(\"}";
        } finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.err.println("Dog eCoin: Could not close input stream!");
                ioe.printStackTrace();
            }
        }
        return new JSONObject(response);
    }
}
