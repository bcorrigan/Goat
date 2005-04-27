package goat.module;

import goat.core.Message;
import goat.core.Module;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * @author bc
 *         Created on 01-Feb-2005
 */
public class Sexy extends Module {

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        m.createReply(calcNumResults(m.modTrailing)).send();
    }

    private String calcNumResults(String searchTerm) {
        try {
            double result = getNumResults("%22" + searchTerm + "%22 sex") / (double) getNumResults("%22" + searchTerm + "%22") * 100.0d;
            if (Double.isNaN(result) || Double.isInfinite(result)) return "Can't say for sure how sexy that is.";
            result = Math.round(result*100)/100.0d;
            return searchTerm.trim() + " is " + Double.toString(result) + "% sexy.";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return ioe.getMessage();
        } catch (ArithmeticException ae) {
            //probably a divide by zero error here
            return "Whoops, that causes a divide by zero. Infinity percent?";
        }
    }

    private long getNumResults(String searchString) throws IOException {
        BufferedReader in = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://www.google.co.uk/search?q=" + searchString.replaceAll(" ", "+"));
            connection = (HttpURLConnection) url.openConnection();
            // incompatible with 1.4
            // connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows 98)");
            connection.setRequestProperty("referer", "http://www.google.com");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("connection", "close");
            connection.setRequestMethod("GET");
            //connection.
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException("Whoa, google gives an HTTP_NOT_FOUND");
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Hmmmn, the server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.matches("color=>Results")) { //in the content line
                    inputLine = inputLine.substring(0,477); //ignore later shite it can confuse and varies too much
                    inputLine = inputLine.replaceAll(".*</b> of about <b>", ""); //zap everything before
                    inputLine = inputLine.replaceAll(".*\\d</b> of <b>", ""); //sometimes above not work when just 1 result
                    inputLine = inputLine.replaceAll("<.*", "");        //zap everything after
                    inputLine = inputLine.replaceAll(",", "");         //get rid of commas, should just be left with a number now
                    try {
                        return Long.parseLong(inputLine);
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                        throw new IOException("omg a parse error: " + nfe.getMessage());
                    }
                }
            }
        } finally {
            if (in != null) in.close();
            if (connection != null) connection.disconnect();
        }
        return 0;
    }

    public String[] getCommands() {
        return new String[]{"sexiness"};
    }
}
