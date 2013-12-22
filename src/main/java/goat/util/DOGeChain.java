package goat.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import goat.core.BotStats;

public class DOGeChain {
    public ArrayList<String> apiCall(String method) {
        return apiCall(method, true);
    }

    public ArrayList<String> insecureApiCall(String method) {
        return apiCall(method, false);
    }

    private ArrayList<String> apiCall(String method, boolean secure) {

        HttpURLConnection connection = null;
        BufferedReader in = null;
        ArrayList<String> response = new ArrayList<String>();
        String protocol = "https";
        if (!secure)
            protocol = "http";

        try {
            URL url = new URL(protocol + "://dogechain.info/chain/Dogecoin/q/" + method);
            connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestProperty("User-Agent", "Goat IRC Bot v" +
					  BotStats.getInstance().getVersion() +
					  " (" + System.getProperty("os.name") +
					  " v" + System.getProperty("os.version") + ';'
					  + System.getProperty("os.arch") + ")");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                response.add("error: DOGeChain.info gave me a " + connection.getResponseCode() + " :(");
                return response;
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "" ;
            while ((inputLine = in.readLine()) != null)
                response.add(inputLine.trim());

        } catch  (SocketTimeoutException e) {
            response.add("error: I got bored waiting for Dog eChain.");
        } catch (IOException e) {
            e.printStackTrace();
            response.add("error\":\"I had an I/O problem when trying to talk to DOGeChain.info :(");
        } finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.err.println("Dog eChain: Could not close input stream!");
                ioe.printStackTrace();
            }
        }
        if(response.isEmpty())
            response.add("error: Empty response from DOGeChain.info");
        return response;
    }
}
