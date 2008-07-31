/*
 * @(#)CurrencyConverter.java
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
 * $Id: CurrencyConverter.java,v 1.4 2004/09/28 01:36:34 erik Exp $
 *
 */
package goat.module;





import java.text.NumberFormat;
import org.jdom.JDOMException;
import java.io.IOException;
import java.util.*;

import goat.core.Message;
import goat.core.Module;
import goat.core.User;
import goat.core.Users;
import static goat.util.CurrencyConverter.*;



public class CurrencyConverter extends Module {
	
    private static boolean DEBUG = false;
	/**
	 * The rates keyword.
	 */
	private static final String RATES_KEYWORD = "rates";

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public static String[] getCommands() {
        return new String[]{"convert"};
    }
    
    public void processChannelMessage(Message m) {
    	if (DEBUG) {
    		System.out.println("convert: today's date is \"" + todaysRateDate() + "\"");
    		System.out.println("convert: rate-table's date is \"" + exchangeRatesPublicationDate + "\"");
    	}
        if (!exchangeRatesPublicationDate.equals(todaysRateDate())) {
        	try {
        		updateRates();
        		if (DEBUG)
        			System.out.println("convert: new rate-table's date is \"" + exchangeRatesPublicationDate + "\"");
        	} catch (JDOMException jde) {
        		m.createReply("I had a problem parsing the exchange rate table").send();
        		jde.printStackTrace();
        		return;
        	} catch (IOException ioe) {
        		m.createReply("I had a problem downloading the exchange rates table").send();
        		ioe.printStackTrace();
        		return;
        	}
        }
        Users users = new Users();
        User user = new User();
        if (users.hasUser(m.sender)) 
        	user = users.getUser(m.sender);
        if (!exchangeRates.isEmpty()) {
        	String fromCurrency="";
        	double fromAmount = 1.0;
        	String toCurrency="";
        	String[] args = {};
    		String trailing = Message.removeFormattingAndColors(m.modTrailing);
    		trailing = trailing.replaceAll(",", "");
    		trailing = translateCurrencyAliases(trailing);
    		trailing = trailing.trim();
    		if(trailing.matches(".*\\s+.*"))
    			args = trailing.split("\\s+");
        	if(DEBUG)
        		System.out.println("convert: modTrailing is \"" + m.modTrailing + "\"");
        	try {
        		if (trailing.matches("(?i)\\d+(\\.\\d+)?\\s+[a-z]{3}\\s+to\\s+[a-z]{3}.*")) {
        			fromCurrency = args[1];
        			toCurrency = args[3].substring(0,3);
        			fromAmount = Double.parseDouble(args[0]);
        		} else if(trailing.matches("(?i)[a-z]{3}\\s+to\\s+[a-z]{3}.*")) {
        			fromCurrency = args[0];
        			toCurrency = args[2].substring(0,3);
        		} else if(trailing.matches("(?i)\\d+(\\.\\d+)?\\s+to\\s+[a-z]{3}.*")) {
        			if(user.getCurrency().equals("")) {
        				m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
        				return;
        			} else {
        				fromCurrency = user.getCurrency();
        				fromAmount = Double.parseDouble(args[0]);
        				toCurrency = args[2].substring(0,3);
        			}
        		} else if(trailing.matches("(?i)\\d+(\\.\\d+)?\\s+[a-z]{3}.*")) {
        			if(user.getCurrency().equals("")) {
        				m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
        				return;
        			} else {
        				toCurrency = user.getCurrency();
        				fromAmount = Double.parseDouble(args[0]);
        				fromCurrency = args[1].substring(0,3);
        			}
        		} else if(trailing.matches("(?i)to\\s+[a-z]{3}.*")) {
        			if(user.getCurrency().equals("")) {
        				m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
        				return;
        			} else {
        				fromCurrency = user.getCurrency();
        				fromAmount = 1.0;
        				toCurrency = args[1].substring(0,3);
        			}
        		} else if(trailing.matches("[a-zA-Z]{3}.*")) {
        			if(user.getCurrency().equals("")) {
        				m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
        				return;
        			} else {
        				toCurrency = user.getCurrency();
        				fromAmount = 1.0;
        				fromCurrency = trailing.substring(0,3);
        			}
        		} else if (trailing.equalsIgnoreCase(RATES_KEYWORD)) {
        			m.createReply("Last Update: " + exchangeRatesPublicationDate).send();
        			final Iterator<String> it = exchangeRates.keySet().iterator();
        			String rate;
        			final StringBuffer buff = new StringBuffer(0);
        			while (it.hasNext()) {
        				rate = it.next();
        				if (buff.length() > 0) {
        					buff.append(", ");
        				}
        				buff.append(rate).append(": ").append(exchangeRates.get(rate));
        			}
        			m.createPagedReply(buff.toString()).send();
        			return;
        		} else {
        			m.createPagedReply("The supported currencies are: " + exchangeRates.keySet().toString()).send();
        			return;
        		}
        	} catch (NumberFormatException nfe) {
        		m.createReply("That number confuses me.  I prefer numbers like 0.12 or 1,234").send();
        	}
        	if (fromCurrency.equalsIgnoreCase(toCurrency))
        		m.createReply("I'm not converting from " + fromCurrency.toUpperCase() + " to " + toCurrency.toUpperCase() + ", duh.").send();
        	else
        		try {
        			NumberFormat nf = NumberFormat.getInstance();
        			nf.setMaximumFractionDigits(3);
        			String fromSymbol = "";
        			String toSymbol = "";
        			try {
        				fromSymbol = Currency.getInstance(fromCurrency.toUpperCase()).getSymbol();
        			} catch (IllegalArgumentException iae) {
        				System.out.println("CurrencyConverter:  weird, java doesn't recognize the currency " + fromCurrency);
        			}
        			try {
        				toSymbol = Currency.getInstance(toCurrency.toUpperCase()).getSymbol();
        			} catch (IllegalArgumentException iae) {
        				System.out.println("CurrencyConverter:  weird, java doesn't recognize the currency " + toCurrency);
        			}
        			// if java doesn't know a symbol for a currency, it barfs the code back at you...
        			if (fromSymbol.equalsIgnoreCase(fromCurrency))
        				fromSymbol = ""; 
        			if (toSymbol.equalsIgnoreCase(toCurrency))
        				toSymbol = "";
        			
        			m.createReply(fromSymbol + nf.format(fromAmount) + " " + fromCurrency.toUpperCase() + " = " + toSymbol + nf.format(convert(fromAmount, fromCurrency, toCurrency)) + " " + toCurrency.toUpperCase()).send();
        			
        		} catch (NullPointerException e) {
        			m.createPagedReply("The supported currencies are: " + exchangeRates.keySet().toString()).send();
        			e.printStackTrace();
        		} catch (NumberFormatException nfe) {
        			m.createReply("I got confused by a number in my exchange rate table.").send();
        			nfe.printStackTrace();
        		}
        } else {
        	m.createReply("Sorry, but the exchange rate table is empty.").send();
        }
    }

}
