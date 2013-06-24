package goat.util;
import goat.core.BotStats;
import goat.core.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UrbanDictionary {

    public Vector<Definition> definitions = new Vector<Definition>();
    public String[][] matchList = new String[0][0];
    public String error = "";
    public static final String dictionaryDescription = "The somewhat spotty slang dictionary at urbandictionary.com" ;


    public UrbanDictionary(String word) {
        try {
            parse(apiDefine(word));
        } catch (JSONException je) {
            error = "Could not parse JSON from UrbanDictionary:  " + je.getMessage();
            je.printStackTrace();
        }
    }

    private void parse(JSONObject json) throws JSONException {
        if(json.has("error"))
            error = json.getString("error");
        else if(!json.has("result_type"))
            error = "Response has no result type; I'm too confused to continue.";
        else if(json.getString("result_type").equals("no_results"))
            error = "No results found.";
        else if(json.getString("result_type").equals("exact"))
            definitions = buildDefinitions(json);
        else if(json.getString("result_type").equals("fulltext"))
            matchList = buildMatchList(json);
        else
            error = "Unknown result type:  \"" + json.get("result_type") + "\".";
    }

    public Vector<Definition> buildDefinitions(JSONObject json) throws JSONException {
        Vector<Definition> ret = new Vector<Definition>();
        if((json.has("result_type") && json.get("result_type").equals("exact"))) {
            JSONArray jlist = json.getJSONArray("list");
            for (int i = 0; i < jlist.length(); i++)
                ret.add(new Definition("urban", dictionaryDescription,
                        jlist.getJSONObject(i).getString("word"),
                        jlist.getJSONObject(i).getString("definition") +
                        "  " + Constants.BOLD + "Example: " + Constants.NORMAL +
                        jlist.getJSONObject(i).getString("example")));

        }
        return ret;
    }

    public String[][] buildMatchList(JSONObject json) throws JSONException {
        String[][] ret = new String[0][0];
        if((json.has("result_type") && json.get("result_type").equals("fulltext"))) {
            JSONArray jlist = json.getJSONArray("list");
            ArrayList<String> matches = new ArrayList<String>();
            for(int i = 0; i < jlist.length(); i++) {
                String thisWord = jlist.getJSONObject(i).getString("word");
                if(!matches.contains(thisWord))
                    matches.add(thisWord);
            }
            ret = new String[matches.size()][2];
            for(int j = 0; j < matches.size(); j++) {
                ret[j][0] = "urban";
                ret[j][1] = matches.get(j);
            }
        }
        return ret;
    }

    private JSONObject apiDefine(String word) throws JSONException {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        String response = "";
        String protocol = "https";
        try {
            URL url = new URL("http://api.urbandictionary.com/v0/define?term=" + java.net.URLEncoder.encode(word, "UTF8"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Goat IRC Bot v" +
                    BotStats.getInstance().getVersion() +
                    " (" + System.getProperty("os.name") +
                    " v" + System.getProperty("os.version") + ';'
                    + System.getProperty("os.arch") + ")");
            connection.setConnectTimeout(3000);  // 3 seconds, no waiting around
            connection.setReadTimeout(3000);  // 3 seconds, no waiting around
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new JSONObject("{\"error\":\"urban dictionary gave me a " + connection.getResponseCode() + " :(\"}");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = "" ;
            while ((inputLine = in.readLine()) != null)
                response += inputLine;

        } catch  (SocketTimeoutException e) {
            response = "{\"error\":\"I got bored waiting for UrbanDictionary.\"}" ;
        } catch (IOException e) {
            e.printStackTrace();
            response = "{\"error\":\"I had an I/O problem when trying to talk to UrbanDictionary :(\"}";
        } finally {
            if(connection!=null) connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.err.println("UrbanDictionary: Could not close input stream!");
                ioe.printStackTrace();
            }
        }
        return new JSONObject(response);
    }
}
