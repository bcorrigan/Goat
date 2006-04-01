package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * A simple horoscope module
 * User: bc
 * Date: 24-Jan-2005
 * Time: 19:34:53
 */
public class Horoscope extends Module {

    ArrayList users;	//all the users of this horoscope module

    public Horoscope() {
        XMLDecoder XMLdec = null;
        try {
            XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/horoscopeUsers.xml")));
            users = (ArrayList) XMLdec.readObject();
        } catch (FileNotFoundException e) {
            File file = new File("resources/horoscopeUsers.xml");
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                System.out.println("horoscopeUsers.xml not found; and furthermore there is an error touching a new one.");
                ioe.printStackTrace();
            }
            users = new ArrayList();
        } catch (NoSuchElementException e) {
            users = new ArrayList();
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        } finally {
            if(XMLdec!=null) XMLdec.close();
        }
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if (m.modTrailing.matches("\\s*")) {     //if just whitespace
            Iterator it = users.iterator();
            while (it.hasNext()) {
                HoroscopeUser user = (HoroscopeUser) it.next();
                if (user.getName().equals(m.sender.toLowerCase())) {
                    m.createReply(user.getSign() + ": " + getReport(user)).send();
                    return;
                }
            }
            m.createReply("I don't know what sign you are, " + m.sender + ", perhaps you should tell me.").send();
        } else if (m.modTrailing.trim().toLowerCase().matches("aries") ||
                m.modTrailing.trim().toLowerCase().matches("taurus") ||
                m.modTrailing.trim().toLowerCase().matches("gemini") ||
                m.modTrailing.trim().toLowerCase().matches("cancer") ||
                m.modTrailing.trim().toLowerCase().matches("leo") ||
                m.modTrailing.trim().toLowerCase().matches("virgo") ||
                m.modTrailing.trim().toLowerCase().matches("libra") ||
                m.modTrailing.trim().toLowerCase().matches("scorpio") ||
                m.modTrailing.trim().toLowerCase().matches("sagittarius") ||
                m.modTrailing.trim().toLowerCase().matches("capricorn") ||
                m.modTrailing.trim().toLowerCase().matches("aquarius") ||
                m.modTrailing.trim().toLowerCase().matches("pisces")) {
            Iterator it = users.iterator();
            while (it.hasNext()) {
                HoroscopeUser user = (HoroscopeUser) it.next();
                if (user.getName().equals(m.sender.toLowerCase())) {
                    user.setSign(m.modTrailing.split(" ")[0].toUpperCase());
                    commit();
                    m.createReply(user.getSign() + ": " + getReport(user)).send();
                    return;
                }
            }
            HoroscopeUser user = new HoroscopeUser(m.sender.toLowerCase(), m.modTrailing.split(" ")[0].toUpperCase());
            users.add(user);
            commit();
            m.createReply(getReport(user)).send();
        }
    }

    public static String[] getCommands() {
        return new String[]{"horoscope"};
    }

    private String getReport(HoroscopeUser user) {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL("http://horoscopes.webscopes.com/daily" + user.getSign().toLowerCase() + ".php");
            connection = (HttpURLConnection) url.openConnection();
            // incompatible with 1.4
            // connection.setConnectTimeout(3000);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                return "That doesn't seem to be a valid star sign, " + user.getName() + ", sorry.";
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Hmmmn, " + user.getName() + ", the server is giving me an HTTP Status-Code " + connection.getResponseCode() + ", sorry.";
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = in.readLine() + ' ';
            boolean inContent = false;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("<!-- Start Content")) {
                    inContent = true;
                    continue;
                }
                if (inContent && inputLine.equals("<br>")) {
                    inputLine = in.readLine();
                    inputLine = inputLine.replaceAll("<.*?>", " "); //strip html
                    break;
                }
            }
            return inputLine;
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
        return null;
    }

    private void commit() {
        XMLEncoder XMLenc = null;
        try {
            XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("resources/horoscopeUsers.xml")));
            XMLenc.writeObject(users);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } finally {
            if(XMLenc!=null) XMLenc.close();
        }
    }

    public static class HoroscopeUser {
        String name;
        String sign;

        public HoroscopeUser() {
            name = "";
            sign = "";
        }

        public HoroscopeUser(String name, String sign) {
            this.name = name;
            this.sign = sign;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSign() {
            return sign;
        }

        public void setSign(String sign) {
            this.sign = sign;
        }
    }
}
