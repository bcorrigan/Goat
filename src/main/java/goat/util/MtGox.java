package goat.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONException;

public class MtGox {
       
    public JSONObject apiCall(String method) throws JSONException {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        String response = "";
        try {
            URL url = new URL("https://data.mtgox.com/api/1/" + method);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);  // 7 seconds, mtgox can be... slow.
            connection.setReadTimeout(7000);  // 7 seconds, mtgox can be... slow.
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new JSONObject("{\"error\":\"MtGox gave me a " + connection.getResponseCode() + " :(\"}");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "" ; 
            while ((inputLine = in.readLine()) != null)
                response += inputLine;
            
        } catch  (SocketTimeoutException e) {
            response = "{\"error\":\"I got bored waiting for MtGox.\"}" ;
        } catch (IOException e) {
            e.printStackTrace();
            response = "{\"error\":\"I had an I/O problem when trying to talk to MtGox :(\"}";
        } finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.err.println("MtGox: Could not close input stream!");
                ioe.printStackTrace();
            }
        }
        return new JSONObject(response);
    }
}