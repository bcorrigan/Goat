package goat.module;
/*
 * @(#)StockQuote.java
 *
 * Copyright (c) 2004, Erik C. Thauvin (erik@thauvin.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * $Id: StockQuote.java,v 1.3 2005/05/05 19:47:29 erik Exp $
 *
 */
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.methods.GetMethod;

import goat.core.Module;
import goat.core.Message;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;


/**
 * Retrieves a stock quote from Yahoo!.
 *
 * @author  Erik C. Thauvin
 * @version $Revision: 1.3 $, $Date: 2005/05/05 19:47:29 $
 * @created Feb 7, 2004
 * @since   1.0
 */
public class StockQuote extends Module {
	/**
	 * The Yahoo! stock quote URL.
	 */
	private static final String YAHOO_URL = "http://finance.yahoo.com/d/quotes.csv?&f=snl1d1t1c1oghv&e=.csv&s=";


	/**
	 * Returns the specified stock quote.
	 */
	public void processChannelMessage(Message m) {
        String symbol = m.modTrailing.trim();
        if( !symbol.matches("\\w*"))
            return;
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL(YAHOO_URL + symbol.toUpperCase());
			connection = (HttpURLConnection) url.openConnection();
			// incompatible with 1.4
			connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				m.createReply("That doesn't seem to be a valid quote, " + m.sender + ", sorry.").send();
                return;
            }
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			    m.createReply( "Hmmmn, " + m.sender + ", the yahoo quote server is giving me HTTP Status-Code " + connection.getResponseCode() + ", sorry.").send();
			    return;
            }
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String quoteString;
            while ((quoteString = in.readLine()) != null) {
                final String[] quote = quoteString.split(",");

                if (quote.length > 0) {
                    String replyString = "";
                    if ((quote.length > 3) && (!"\"N/A\"".equalsIgnoreCase(quote[3])))
                    {
                        replyString += Message.BOLD + "Symbol: " + Message.NORMAL + quote[0].replaceAll("\"", "") + " [" + quote[1].replaceAll("\"", "") + ']';

                        if (quote.length > 5) {
                            replyString += Message.BOLD + " Last Trade: " + Message.NORMAL + quote[2] + " (" + quote[5] + ')';
                        }
                        else {
                            replyString += Message.BOLD + " Last Trade: " + Message.NORMAL + quote[2];
                        }

                        if (quote.length > 4) {
                            replyString += Message.BOLD + " Time: " + Message.NORMAL + quote[3].replaceAll("\"", "") + ' ' + quote[4].replaceAll("\"", "");
                        }

                        if (quote.length > 6) {
                            replyString += Message.BOLD +" Open: " + Message.NORMAL + quote[6];
                        }

                        if (quote.length > 7) {
                            replyString += Message.BOLD + " Day's Range: " + Message.NORMAL + quote[7] + " - " + quote[8];
                        }

                        if (quote.length > 9) {
                            replyString += Message.BOLD + " Volume: " + Message.NORMAL + quote[9];
                        }
                    } else {
                        replyString = "Invalid ticker symbol.";
                    }
                    m.createPagedReply(replyString).send();
                }

                else {
                    m.createReply( "No data returned." ).send();
                }
            }
        }
		catch (IOException e) {
			m.createReply("Unable to retrieve stock quote for: " + symbol).send();
		}
	}

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public static String[] getCommands() {
		return new String[]{"quote"};
	}
}
