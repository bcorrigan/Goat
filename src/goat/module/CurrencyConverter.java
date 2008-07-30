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
    		System.out.println("convert: today's date is \"" + todaysDate() + "\"");
    		System.out.println("convert: rate-table's date is \"" + s_date + "\"");
    	}
        if (!s_date.equals(todaysDate())) {
            EXCHANGE_RATES.clear();
        }
        if (EXCHANGE_RATES.isEmpty()) {
        	try {
        		updateRates();
        		if (DEBUG)
        			System.out.println("convert: new rate-table's date is \"" + s_date + "\"");
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
        if (users.hasUser(m.sender)) {
        	user = users.getUser(m.sender);
        }
        
        // System.out.println("1");
        m.modTrailing = m.modTrailing.toLowerCase().trim();
        
        // a few aliases... be sure to keep things lower case in this section
        m.modTrailing = m.modTrailing.replaceAll("gay money", "eur");
        m.modTrailing = m.modTrailing.replaceAll("real money", "usd");
        m.modTrailing = m.modTrailing.replaceAll("proper money", "gbp");
        m.modTrailing = m.modTrailing.replaceAll("blood money", "usd");
        m.modTrailing = m.modTrailing.replaceAll("oil money", "usd");
        m.modTrailing = m.modTrailing.replaceAll("dirty money", "usd");
        m.modTrailing = m.modTrailing.replaceAll("eddie money", "usd");
        m.modTrailing = m.modTrailing.replaceAll("monopoly money", "cad");
        m.modTrailing = m.modTrailing.replaceAll("tubgirl money", "jpy");
        m.modTrailing = m.modTrailing.replaceAll("dinero", "mxn");
        m.modTrailing = m.modTrailing.replaceAll("pounds", "gbp");
        m.modTrailing = m.modTrailing.replaceAll("pound", "gbp");
        m.modTrailing = m.modTrailing.replaceAll("yen", "jpy");
        m.modTrailing = m.modTrailing.replaceAll("dollars", "nzd");
        m.modTrailing = m.modTrailing.replaceAll("dollar", "cad");
        m.modTrailing = m.modTrailing.replaceAll("bucks", "usd");
        m.modTrailing = m.modTrailing.replaceAll("buck", "usd");
        m.modTrailing = m.modTrailing.replaceAll("quid", "gbp");
        m.modTrailing = m.modTrailing.replaceAll("loonies", "cad");
        m.modTrailing = m.modTrailing.replaceAll("loonie", "cad");
        
        if (!EXCHANGE_RATES.isEmpty()) {
        	String fromCurrency="";
        	double fromAmount = 1.0;
        	String toCurrency="";
        	String[] cmds = {};
        	if(DEBUG)
        		System.out.println("convert: modTrailing is \"" + m.modTrailing + "\"");
            if (m.modTrailing.matches("\\d+([,\\d]+)?(\\.\\d+)?\\s+[a-z]{3}\\s+to\\s+[a-z]{3}.*")) {
                cmds = m.modTrailing.split("\\s+");
                fromCurrency = cmds[1];
                toCurrency = cmds[3].substring(0,3);
                try {
                	fromAmount = Double.parseDouble(cmds[0].replaceAll(",", ""));
                } catch (NumberFormatException nfe) {
                    m.createReply("There's something funky about that number that confuses me.").send();
                    return;
                }
            } else if(m.modTrailing.matches("[a-z]{3}\\s+to\\s+[a-z]{3}.*")) {
            	cmds = m.modTrailing.split("\\s+");
            	fromCurrency = cmds[0];
            	toCurrency = cmds[2].substring(0,3);
            } else if(m.modTrailing.matches("\\d+([,\\d]+)?(\\.\\d+)?\\s+to\\s+[a-z]{3}.*")) {
            	if(user.getCurrency().equals("")) {
            		m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
            		return;
            	} else {
            		fromCurrency = user.getCurrency();
            		cmds = m.modTrailing.split("\\s+");
            		fromAmount = Double.parseDouble(cmds[0]);
            		toCurrency = cmds[2].substring(0,3);
            	}
            } else if(m.modTrailing.matches("\\d+([,\\d]+)?(\\.\\d+)?\\s+[a-z]{3}.*")) {
            	if(user.getCurrency().equals("")) {
            		m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
            		return;
            	} else {
            		toCurrency = user.getCurrency();
            		cmds = m.modTrailing.split("\\s+");
            		fromAmount = Double.parseDouble(cmds[0]);
            		fromCurrency = cmds[1].substring(0,3);
            	}
            } else if(m.modTrailing.matches("to\\s+[a-z]{3}.*")) {
            	if(user.getCurrency().equals("")) {
            		m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
            		return;
            	} else {
            		fromCurrency = user.getCurrency();
            		cmds = m.modTrailing.split("\\s+");
            		fromAmount = 1.0;
            		toCurrency = cmds[1].substring(0,3);
            	}
            } else if(m.modTrailing.matches("[a-z]{3}.*")) {
            	if(user.getCurrency().equals("")) {
            		m.createReply("I don't know your currency, " + m.sender + ".  Either specify a currency to convert from, or set your currency by typing \"currency XXX\", where XXX is your currency code.").send();
            		return;
            	} else {
            		toCurrency = user.getCurrency();
            		fromAmount = 1.0;
            		fromCurrency = m.modTrailing.substring(0,3);
            	}
            } else if (m.modTrailing.trim().toLowerCase().equals(RATES_KEYWORD)) {
                m.createReply("Last Update: " + s_date).send();
                final Iterator<String> it = EXCHANGE_RATES.keySet().iterator();
                String rate;
                final StringBuffer buff = new StringBuffer(0);
                while (it.hasNext()) {
                    rate = it.next();
                    if (buff.length() > 0) {
                        buff.append(", ");
                    }
                    buff.append(rate).append(": ").append(EXCHANGE_RATES.get(rate));
                }
                m.createPagedReply(buff.toString()).send();
                return;
            } else {
                m.createPagedReply("The supported currencies are: " + EXCHANGE_RATES.keySet().toString()).send();
                return;
            }
            
            if (fromCurrency.equalsIgnoreCase(toCurrency))
            	m.createReply("I'm not converting from " + fromCurrency.toUpperCase() + " to " + toCurrency.toUpperCase() + ", duh.").send();
            else
            	try {
            		NumberFormat nf = NumberFormat.getInstance();
            		nf.setMaximumFractionDigits(3);
            		m.createReply(fromAmount + " " + fromCurrency.toUpperCase() + " = " +  nf.format(convert(fromAmount, fromCurrency, toCurrency)) + " " + toCurrency.toUpperCase()).send();
            	} catch (NullPointerException e) {
                    m.createPagedReply("The supported currencies are: " + EXCHANGE_RATES.keySet().toString()).send();
                } catch (NumberFormatException nfe) {
                    m.createReply("I got confused by a number in my exchange rate table.").send();
                }
        } else {
            m.createReply("Sorry, but the exchange rate table is empty.").send();
        }
    }

}
