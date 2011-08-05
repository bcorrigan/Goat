package goat.util;

import goat.Goat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Properties;
import org.json.JSONException;
import org.json.JSONObject;

public class WhatTheTrend {
	
	public Boolean debug = true;
	public static final String ATTRIBUTION = "Definition by whatthetrend.com";
	
	private String API_KEY = null;	
	private final String URI_HOST = "api.whatthetrend.com";
	private final String URI_PATH = "/api/";
	
	// For documentation of the methods named below, see: http://api.whatthetrend.com/api
	// private static final String[] v1methods = {"listAll", "listExplained", "getByName", "getById", 
	//	"search", "search_extended", "update" };
	// private static final String[] v2methods = {"trends", "", "trends/active", "trends/spam", 
	//	"trends/location/top", "revert", "update", "spam", "categories", "locations", "locations/all",
	//	"vote_up", "vote_down", "flag"};
	
	public WhatTheTrend () {
		Properties props = Goat.getPasswords();
		API_KEY = props.getProperty("wtt.apikey");
		if (API_KEY == null) {
			System.err.println("I couldn't read the API key for WhatTheTrend");
		}
	}
	
	// note that the query string should be the full URI query part, minus the API key, e.g. "ID=4321&versions=3"
	// if we weren't lazy, we might do convenience methods wrapping this for each API method. 
	private JSONObject doApiCall(String path, String query) throws Exception {
		JSONObject ret;
		HttpURLConnection connection = null;

		if(path != null && !path.equals(""))
			path = URI_PATH + path;
		else 
			path = URI_PATH;
		if(query != null && !query.trim().equals(""))
			query = query + "&api_key=" + API_KEY;
		else
			query = "api_key=" + API_KEY;
		
		// farting around with URI instead of URL to get query encoded correctly
		URI uri = new URI("http", URI_HOST, path, query, null);
		if(debug)
			System.out.println("url: " + uri.toURL());
		connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
		connection.setReadTimeout(3000);
		int responseCode = connection.getResponseCode();
		if (responseCode != HttpURLConnection.HTTP_OK) {
			System.out.println("Fuckup at whatthetrend.com, HTTP Response code: " + responseCode);
			throw new Exception("response from whatthetrend.com: " + responseCode);
		}
		if(debug)
			System.out.println("Response OK for query \"" + path + query + "\"") ;
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while((line = reader.readLine()) != null) {
			builder.append(line);
		}
		if (debug)
			System.out.println("builder built");
		String content = builder.toString();
		if(content.trim().equals(""))
			throw new Exception("empty response from whatthetrend.com");
		if (debug)
			System.out.println("content:\n" + content + "\n\n");
		try {
			ret = new JSONObject(content);
		} catch (JSONException je) {
			throw new Exception(content);
		}
		if (debug)
			System.out.println("pretty:\n" + ret.toString(3) + "\n\n");
		if(connection!=null) connection.disconnect();
		return ret ;
	}

	// method for the only API call we're going to be using any time soon:
	// results are in order of most recent to least recent.
	public JSONObject getByName(String trend, int numResults) throws Exception {
		if (trend == null || trend.trim().equals("")) {
			throw new Exception("I need the name of a trend before I can look it up.");
		}
		if (numResults < 1) {
			// -1 means "return all" for whatthetrend.  How quirky.
			numResults = -1;
		}
		String path = "trend/getByName/" + trend + "/json";
		// if omitted in the api call, numResults will default to 1
		String query = null;
		if (numResults != 1) {
			query = "versions=" + numResults;
		}
		return doApiCall(path, query);
	}
	
	public JSONObject getByName(String trend) throws Exception {
		return getByName(trend, 1);
	}
	
}
