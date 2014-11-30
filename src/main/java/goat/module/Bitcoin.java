package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.Users;
import goat.core.Constants;
import goat.util.CommandParser;
import goat.util.BitcoinCharts;
import goat.util.BitcoinAverage;

import java.io.*;
import java.net.* ;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class Bitcoin extends Module {

    /**
     * Returns the specified bitcoin quote.
     * A hodgepodge of copied code and googled examples.
     */

    private BitcoinCharts bitcoincharts = new BitcoinCharts();
    private BitcoinAverage bitcoinaverage = new BitcoinAverage();

    public boolean isThreadSafe() {
        return false;
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
        return new String[]{"bitcoin", "buttcoin"};
    }

    public void processChannelMessage(Message m) {

        if(m.getModTrailing().startsWith("help"))
            m.reply("usage: bitcoin [help]  " +
                    "[column={volume, bid, high, currency_volume, ask, close, avg, low}]  " +
                    "[currency={"+ bitcoincharts.currencies() +"}] " +
                    "[symbol={see http://bitcoincharts.com/markets for list}]  " +
                    "  If both currency and symbol are specified, symbol overrides currency. " +
                    "results cached for 30 seconds");
        else
            ircQuote(m);
    }

    private void ircQuote(Message m) {
        //System.out.println("Entering bitcoin ircQuote");
        String userCurrency = "";
        Users users = goat.Goat.getUsers();
        if(users.hasUser(m.getSender()))
            userCurrency = users.getUser(m.getSender()).getCurrency();
        if (userCurrency.equals(""))
            userCurrency = "GBP"; // goat is UKian

        TimeZone tz = null;
        if(users.hasUser(m.getSender()))
            tz = users.getUser(m.getSender()).getTimeZone();

        CommandParser parser = new CommandParser(m.getModTrailing());

        String symbol = "btcavg";

        if (parser.hasVar("symbol"))
            symbol = parser.get("symbol");
        else if (parser.hasVar("currency") && bitcoincharts.hasCurrency(parser.get("currency")))
            userCurrency = parser.get("currency").toUpperCase();

        try {
            if (symbol.startsWith("btcavg")) {
                m.reply(bitcoinaverage.quote(tz, userCurrency));
            } else {
                String column = parser.hasVar("column") ? parser.get("column") : "close";
                m.reply(bitcoincharts.quote(symbol, column, tz));
            }
        } catch (JSONException e) {
            m.reply("Someone didn't program the JSON properly:  " + e.getMessage());
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            m.reply("I got bored waiting for the bitcoins to talk to me.");
        } catch (MalformedURLException e) {
            m.reply("My programmers don't know how to write a URL.");
            e.printStackTrace();
        } catch (ProtocolException e) {
            m.reply("Network buggery:  " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            m.reply("I/O is hard, let's go shopping.  " + e.getMessage());
            e.printStackTrace();
        }
    }

}
