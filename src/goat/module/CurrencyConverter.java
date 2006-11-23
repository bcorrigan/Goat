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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import org.jdom.input.SAXBuilder;

import java.io.IOException;

import java.net.URL;

import java.text.NumberFormat;

import java.util.*;

import goat.core.Message;
import goat.core.Module;


/**
 * Converts various currencies.
 *
 * @author Erik C. Thauvin
 * @version $Revision: 1.4 $, $Date: 2004/09/28 01:36:34 $
 *          <p/>
 *          Feb 11, 2004
 * @since 1.0
 */
public class CurrencyConverter extends Module {
    /**
     * The exchange rates table URL.
     */
    private static final String EXCHANGE_TABLE_URL = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml";

    /**
     * The exchange rates.
     */
    private static final Map EXCHANGE_RATES = new TreeMap();

    /**
     * The rates keyword.
     */
    private static final String RATES_KEYWORD = "rates";

    /**
     * The last exchange rates table publication date.
     */
    private static String s_date = "";

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if (!s_date.equals(new Date())) {
            EXCHANGE_RATES.clear();
        }
        if (EXCHANGE_RATES.isEmpty()) {
            try {
                final SAXBuilder builder = new SAXBuilder();
                builder.setIgnoringElementContentWhitespace(true);

                final Document doc = builder.build(new URL(EXCHANGE_TABLE_URL));
                final Element root = doc.getRootElement();
                final Namespace ns = root.getNamespace("");
                final Element cubeRoot = root.getChild("Cube", ns);
                final Element cubeTime = cubeRoot.getChild("Cube", ns);

                s_date = cubeTime.getAttribute("time").getValue();

                final List cubes = cubeTime.getChildren();
                Element cube;

                for (Object cube1 : cubes) {
                    cube = (Element) cube1;
                    EXCHANGE_RATES.put(cube.getAttribute("currency").getValue(), cube.getAttribute("rate").getValue());
                }

                EXCHANGE_RATES.put("EUR", "1");
            } catch (JDOMException e) {
                m.createReply("An error has occurred while parsing the exchange rates table.").send();
                e.printStackTrace();
            } catch (IOException e) {
                m.createReply("An error has occurred while fetching the exchange rates table:  " + e.getMessage()).send();
                e.printStackTrace();
            }
        }
        System.out.println("1");
        m.modTrailing = m.modTrailing.toLowerCase();
        if (!EXCHANGE_RATES.isEmpty()) {
            if (m.modTrailing.matches(".*\\d+([,\\d]+)?(\\.\\d+)? [a-z]{3}+ to [a-z]{3}+.*")) {
                final String[] cmds = m.modTrailing.split(" ");

                if (cmds.length == 4) {
                    if (cmds[3].equals(cmds[1])) {
                        m.createReply("You're kidding, right?").send();
                    } else {
                        try {
                            final double amt = Double.parseDouble(cmds[0].replaceAll(",", ""));
                            final double from = Double.parseDouble((String) EXCHANGE_RATES.get(cmds[1].toUpperCase()));
                            final double to = Double.parseDouble((String) EXCHANGE_RATES.get(cmds[3].toUpperCase()));

                            m.createPagedReply(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amt).substring(1) + ' ' +
                                    cmds[1].toUpperCase() + " = " +
                                    NumberFormat.getCurrencyInstance(Locale.getDefault()).format((amt * to) / from)
                                    .substring(1) + ' ' + cmds[3].toUpperCase()).send();
                        } catch (NullPointerException e) {
                            m.createReply("The supported currencies are: " + EXCHANGE_RATES.keySet().toString()).send();
                        } catch (NumberFormatException nfe) {
                            m.createReply("There's something funky about that number that confuses me.").send();
                        }
                    }
                }
            } else if (m.modTrailing.equals(RATES_KEYWORD)) {
                m.createReply("Last Update: " + s_date).send();

                final Iterator it = EXCHANGE_RATES.keySet().iterator();
                String rate;

                final StringBuffer buff = new StringBuffer(0);

                while (it.hasNext()) {
                    rate = (String) it.next();
                    if (buff.length() > 0) {
                        buff.append(", ");
                    }
                    buff.append(rate).append(": ").append(EXCHANGE_RATES.get(rate));
                }

                m.createReply(buff.toString()).send();

            } else {
                m.createReply("The supported currencies are: " + EXCHANGE_RATES.keySet().toString());
            }
        } else {
            m.createReply("Sorry, but the exchange rate table is empty.").send();
        }
    }

    public static String[] getCommands() {
        return new String[]{"convert"};
    }
}
