package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: bc
 * Date: 26-Nov-2006
 * Time: 15:52:59
 */
public class Googlism extends Module {
	/**
	 * The Yahoo! stock quote URL.
	 */
	private static final String WHO_URL = "http://www.googlism.com/index.htm?type=1&ism=";
    private static final String WHAT_URL = "http://www.googlism.com/index.htm?type=2&ism=";
    private static final String WHERE_URL = "http://www.googlism.com/index.htm?type=3&ism=";
    private static final String WHEN_URL = "http://www.googlism.com/index.htm?type=4&ism=";

    private String[] stripWords = {"and","the","of","is","are", "those", "these", "was", "a", "in", "for"};

    public void processChannelMessage(Message m) {
        if(!m.directlyAddressed) {
            return;
        }
        String query = m.modTrailing.trim();
        query = stripInsubstantialWords(query);
        query = query.replaceAll("\\?","");
        if(query.length()==0) {
            //infuriate the user
            m.createReply("I'm sorry, but you don't seem to have anything interesting to ask.").send();
            return;
        }

        HttpURLConnection connection = null;
        BufferedReader in = null;
        String googlismUrl;

        String failureTerm;
        if(m.modCommand.trim().toLowerCase().equals("who")) {
            googlismUrl = WHO_URL;
            failureTerm = "that person";
        } else if(m.modCommand.trim().toLowerCase().equals("what")) {
            googlismUrl = WHAT_URL;
            failureTerm = "that";
        } else if(m.modCommand.trim().toLowerCase().equals("where")) {
            googlismUrl = WHERE_URL;
            failureTerm = "that place";
        } else if(m.modCommand.trim().toLowerCase().equals("when")) {
            googlismUrl = WHEN_URL;
            failureTerm = "when that happened";
        } else {
            return;
        }

        try {
            URL url = new URL( googlismUrl + URLEncoder.encode(query, "ISO-8859-1"));
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				m.createReply("I don't know anything about that, " + m.sender + ", sorry.").send();
                return;
            }
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			    m.createReply( "Hmmmn, " + m.sender + ", the googlism server is giving me HTTP Status-Code " + connection.getResponseCode() + ", sorry.").send();
			    return;
            }
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String googlismLine;
            boolean inResults;
            String results = "";

            while ((googlismLine = in.readLine()) != null) {
                if(googlismLine.toLowerCase().startsWith(query.toLowerCase())) {
                    //in results
                    results+= tidyResultString(googlismLine);
                } else if(googlismLine.contains("Googlism for")) {
                    //the first line is special, annoyingly
                    if(!googlismLine.contains("doesn't know enough about")) {
                        results += tidyResultString( query.toLowerCase() + " " + googlismLine.toLowerCase().replaceAll(".*" + query.toLowerCase(), "") );
                    }
                }
            }
            if(results.length()>0)
                m.createPagedReply(results).send();
            else
                m.createReply("I don't know anything about " + failureTerm + ", " + m.sender + ".").send();
        }
		catch (IOException e) {
			m.createReply("I'm broken!").send();
		}
	}

    private String tidyResultString(String googlismLine) {
        googlismLine = googlismLine.replaceAll("<.*?>", "");
        googlismLine = googlismLine.replaceAll("\\s{2,}?", " ");
        googlismLine = googlismLine.replaceAll("\\r", "");
        googlismLine = googlismLine.replaceAll("\\t", "");
        googlismLine = googlismLine.replaceAll("\\&amp;", "&");
        googlismLine = googlismLine.replaceAll("\\&quot;", "'");
        googlismLine = googlismLine.trim();
        return googlismLine + ". ";
    }

    /**
     * A crap attempt to strip out irrelevant words from the start
     * of the query. So a query like (where) "is the mountain ben nevis"
     * will be stripped to just "mountain ben nevis".
     * @param query String to be stripped
     * @return A string stripped of words like "of", "and", and "the" at the start
     */
    private String stripInsubstantialWords(String query) {
        outer:
        while(true) {
            for (String stripWord : stripWords) {
                if (query.startsWith(stripWord)) {
                    if(query.replaceFirst(stripWord, "").startsWith(" ")) {
                        query = query.replaceFirst(stripWord, "").trim();
                        continue outer;
                    }
                }
            }
            break;
        }
        return query;
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public static String[] getCommands() {
		return new String[]{"who", "what", "where", "when"};
	}
}

