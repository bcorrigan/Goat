/**
 * <P>Allows users to query goat for bible quotes. eg:</P>
 * 
 * <b>&lt;bc&gt;</b> goat, bible genesis 1:1 bible=kjv<br>
 * <b>&lt;goat&gt;</b> In the beginning God created the heaven and the earth.
 *  
 * @author bc
 * Created on 10-Jul-2005
 */
package goat.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import goat.core.Message;
import goat.core.Module;
import goat.util.CommandParser;

public class Bible extends Module {
    
    private HashMap bibles = new HashMap();
    private String supportedBibles = "King James Version (KJV), New International Version (NIV)," +
            " New American Standard Bible(NASB), The Message (MSG), Amplified Bible (AMP)," +
            " New Living Translation (NLT), English Standard Version (ESV)," +
            " COntemporary English Version (CEV), New King James Version (NKJV)," +
            " 21st Century King James Version (KJ21), American Standard Version (ASV)," +
            " Young's Literal Translation (YLT), Darby Translation (DARBY), New Life Version (NLV)," +
            " Holman Christian Standard Bible (HCSB), New International Reader's Version (NIRV)," +
            " Wycliffe New Testament (WNT), World English New Testament (WE)," +
            " New International Version - UK (NIVUK).";
        
    
    public Bible() {
        bibles.put("KJV", new Integer(9));      //King James Version
        bibles.put("NIV", new Integer(31));     //New International Version
        bibles.put("NASB", new Integer(49));    //New American Standard Bible
        bibles.put("MSG", new Integer(65));     //The Message
        bibles.put("AMP", new Integer(45));     //Amplified Bible
        bibles.put("NLT", new Integer(51));     //New Living Translation
        bibles.put("ESV", new Integer(47));     //English Standard Version
        bibles.put("CEV", new Integer(46));     //Contemporary English Version
        bibles.put("NKJV", new Integer(50));    //New King James Version
        bibles.put("KJ21", new Integer(48));    //21st Century King James Version
        bibles.put("ASV", new Integer(8));      //American Standard Version
        bibles.put("YLT", new Integer(15));     //Young's Literal Translation
        bibles.put("DARBY", new Integer(16));   //Darby Translation
        bibles.put("NLV", new Integer(74));     //New Life Version
        bibles.put("HCSB", new Integer(77));    //Holman Christian Standard Bible
        bibles.put("NIRV", new Integer(76));    //New International Reader's Version
        bibles.put("WNT", new Integer(53));     //Wycliffe New Testament
        bibles.put("WE", new Integer(73));      //World English (New Testament)
        bibles.put("NIVUK", new Integer(64));   //New International Version - UK
    }
    
    public int messageType() {
        return WANT_COMMAND_MESSAGES;
    }
    
    public String[] getCommands() {
        return new String[] { "bible", "bibles" };
    }
    
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if(m.modCommand.equalsIgnoreCase("bible"))
            lookup(m);
        if(m.modCommand.equalsIgnoreCase("bibles"))
            m.createPagedReply( supportedBibles ).send();
    }
    
    private void lookup(Message m) {
        CommandParser parser = new CommandParser(m);
        String bible="KJV";
        String bibleQuery = null;
        if(parser.has("bible"))
            bible=parser.get("bible").toUpperCase();
        if(!bibles.containsKey(bible)) {
            m.createReply("I'm afraid that bible is not supported. Try one of these: ").send();
            m.createPagedReply( supportedBibles ).send();
            return;
        }
        try {
            bibleQuery = java.net.URLEncoder.encode( parser.remaining(), "ISO-8859-1" );
        } catch (UnsupportedEncodingException e) {
            m.createReply("Internal error: encoding not supported for urlencode. ").send();
            e.printStackTrace();
            return;
        }
        System.out.println("bibleQuery: " + bibleQuery);
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL("http://bible.gospelcom.net/passage/?search=" + bibleQuery + ";&version=" + bibles.get( bible ) + ";");
            connection = (HttpURLConnection) url.openConnection();
            // incompatible with 1.4
            // connection.setConnectTimeout(3000);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                m.createReply( "For some reason the bible server is giving me a 404, " + m.sender + ", sorry." ).send();
                return;
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                m.createReply( "Hmmmn, " + m.sender + ", the server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.").send();
                return;
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = in.readLine() + ' ';
            
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("en-" + bible)) {
                    inputLine = inputLine.replaceAll("<.*?>", " "); //strip html
                    inputLine = inputLine.replaceAll("&nbsp;", ""); //strip &nbsp;'s
                    m.createPagedReply( inputLine ).send();
                    return;
                }
                if (inputLine.contains("No results found")) {
                    m.createReply("Sorry, the Good Book doesn't have that.").send();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
            try {
                if(in!=null) in.close();
            } catch (IOException ioe) {
                System.out.println("Could not close stream. ");
                ioe.printStackTrace();
            }
        }
    }
}
