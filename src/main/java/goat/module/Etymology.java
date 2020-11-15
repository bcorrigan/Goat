package goat.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import goat.core.Module;
import goat.util.StringUtil;


//gets us the etymology of a given word from 
public class Etymology extends Module {

	private String etyuri = "http://etymonline.com/?term=";
	private String etysearch = "http://etymonline.com/?search=";
	
	public String[] getCommands() {
		return new String[]{"etymology"};
	}
	
	@Override
	public void processChannelMessage(Message m) {
		// grab query string
		String query = m.getModTrailing();
		// strip away any irc gunk and leading/trailing whitespace
		query = StringUtil.removeFormattingAndColors(query).trim();
		// remove quote marks, we'll put them back later if we need them
		query = query.replaceAll("\"", "") ;
		query=query.trim();
		String etym = searchEtyms(query, m);
		m.pagedReply(etym);
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}
	
	//straight rip of old confessions code - it lives!
	private String parseEtym(HttpURLConnection connection) throws SocketTimeoutException, IOException {
		//debug
		String etym = "";
		try {
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				System.out.println("Fuck up at etymology site, HTTP Response code: " + connection.getResponseCode());

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.matches(".*div id=\\\"dictionary\\\".*")) {  //inside etym
					while (true) {
						etym += inputLine + " ";
						if (inputLine.matches(".*<!-- DICTIONARY -->.*")) { //outside etym - break
							break;
						}
						inputLine = in.readLine();
					}
					etym = etym.replaceAll("<.*?>", "");
					etym = etym.replaceAll("\\s{2,}?", " ");
					etym = etym.replaceAll("\\r", "");
					etym = etym.replaceAll("\\t", "");
					etym = etym.trim();
					break;
				}
			}
			in.close();
		} catch(SocketTimeoutException ste) {
			// System.out.println("Connection timed out");
			throw ste;
		}
		System.out.println("etym:" + etym);
		return etym;
	}
	
	private String searchEtyms(String searchString, Message m) {
		String searchedEtym = "";
        HttpURLConnection connection = null;
		try {
			searchString = searchString.trim();
			searchString = StringUtil.removeFormattingAndColors(searchString);
			String query = searchString;
			//spaces between words need ot be +'s
			query = query.replaceAll("\\s+", "+");
			// pop our search string in quotes if it's got spaces in it.
			if (query.matches(".*\\s+.*"))
				query = "\"" + query + "\"";
			// we use URI here with a multi-argument constructor
			// because that way it encodes characters properly for us.
			URI searchURI = new URI("http", "//etymonline.com/?term=" + query, null);
			URL grouphug = searchURI.toURL();
			connection = (HttpURLConnection) grouphug.openConnection();
			connection.setConnectTimeout(3000) ;
			connection.setReadTimeout(10000);
			searchedEtym = parseEtym(connection);
			if(searchedEtym.equals("")) {
				m.reply("I don't know the origin of " + searchString + ".");
			}
		} catch (URISyntaxException e) {
			m.reply("Um, I couldn't make a valid URI out of \"" + searchString + "\".");
		} catch (SocketTimeoutException e) {
			if (null != m)
				m.reply("Timed out while trying to extract etymology of " + searchString);
			// e.printStackTrace() ;
		} catch (IOException e) {
			if (null != m)
				m.reply("I/O problem while trying to extract etymology of " + searchString);
			e.printStackTrace();
            if(connection!=null) connection.disconnect();
		}
		return searchedEtym;
	}
}
