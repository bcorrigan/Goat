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
            " New International Version - UK (NIVUK), Hatian Creole Version (HCV), Luther Bibel 1545 (LUTH1545).";
        
    
    public Bible() {
        bibles.put("KJV", 9);      //King James Version
        bibles.put("NIV", 31);     //New International Version
        bibles.put("NASB", 49);    //New American Standard Bible
        bibles.put("MSG", 65);     //The Message
        bibles.put("AMP", 45);     //Amplified Bible
        bibles.put("NLT", 51);     //New Living Translation
        bibles.put("ESV", 47);     //English Standard Version
        bibles.put("CEV", 46);     //Contemporary English Version
        bibles.put("NKJV", 50);    //New King James Version
        bibles.put("KJ21", 48);    //21st Century King James Version
        bibles.put("ASV", 8);      //American Standard Version
        bibles.put("YLT", 15);     //Young's Literal Translation
        bibles.put("DARBY", 16);   //Darby Translation
        bibles.put("NLV", 74);     //New Life Version
        bibles.put("HCSB", 77);    //Holman Christian Standard Bible
        bibles.put("NIRV", 76);    //New International Reader's Version
        bibles.put("WNT", 53);     //Wycliffe New Testament
        bibles.put("WE", 73);      //World English (New Testament)
        bibles.put("NIVUK", 64);   //New International Version - UK
        bibles.put("HCV", 23);     //Haitian Creole Version
        bibles.put("LUTH1545", 10);//Martin Luther version
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
        if(m.getModCommand().equalsIgnoreCase("bible"))
            lookup(m);
        if(m.getModCommand().equalsIgnoreCase("bibles"))
            m.createPagedReply( supportedBibles ).send();
    }
    
    private void lookup(Message m) {
        CommandParser parser = new CommandParser(m);
        String bible="KJV";
        String bibleQuery = null;
        if(parser.has("bible"))
            bible=parser.get("bible").toUpperCase();
        if(!bibles.containsKey(bible)) {
            m.reply("I'm afraid that bible is not supported. Try one of these: ");
            m.createPagedReply( supportedBibles ).send();
            return;
        }
        try {
            bibleQuery = java.net.URLEncoder.encode( parser.remaining(), "ISO-8859-1" );
        } catch (UnsupportedEncodingException e) {
            m.reply("Internal error: encoding not supported for urlencode. ");
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
                m.reply( "For some reason the bible server is giving me a 404, " + m.getSender() + ", sorry." );
                return;
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                m.reply( "Hmmmn, " + m.getSender() + ", the server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.");
                return;
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String inputLine = in.readLine() + ' ';
            
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("en-" + bible) || inputLine.contains("cpf-" + bible) || inputLine.contains("de-" + bible)) {
                    inputLine = inputLine.replaceAll("<.*?>", " "); //strip html
                    inputLine = inputLine.replaceAll("&nbsp;", ""); //strip &nbsp;'s
                    m.createPagedReply( inputLine ).send();
                    return;
                }
                if (inputLine.contains("No results found")) {
                    m.reply("Sorry, the Good Book doesn't have that.");
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
