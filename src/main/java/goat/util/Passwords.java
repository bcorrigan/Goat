package goat.util;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

public class Passwords {

    public static final String GOAT_PROPS_FILE = "config/goat.properties" ;
    public static final String GOAT_PASSWORDS_FILE = "config/passwords.properties" ;

    public static class Properties extends java.util.Properties {
	/**
	 * Overrides, called by the store method.
	 */
	@SuppressWarnings("unchecked")
	    public synchronized Enumeration keys() {
		Enumeration keysEnum = super.keys();
		Vector keyList = new Vector();
		while(keysEnum.hasMoreElements()){
		    keyList.add(keysEnum.nextElement());
		}
		Collections.sort(keyList);
		return keyList.elements();
	    }
    }


    public static String getPassword(String id) {
	// the intermediary passwords Properties object here isn't stored, which is what we want
	return getPasswords().getProperty(id);
    }

    public static Properties getPropsFromFile(String filename) {
	Properties props = new Properties();
	try {
	    props.load(new FileInputStream(filename));
	} catch (IOException e) {
	    System.err.println("WARNING:  Could not load properties from file \"" + filename + "\"") ;
	    e.printStackTrace() ;
	}
	return props;
    }

    /* use getPassword() in preference to this; if you do need several things out of the file,
     * remember to set your Properties object to null when you're done; it's good housekeeping
     * to make sure the whole password keboodle doesn't hang around in ram */
    public static Properties getPasswords() {
	return getPropsFromFile(GOAT_PASSWORDS_FILE) ;
    }

    public static void writePasswords(Properties passwords) throws IOException {
	FileOutputStream out = new FileOutputStream(GOAT_PASSWORDS_FILE);
	passwords.store(out, passwdCommentText);
	out.close();
    }

    private static String passwdCommentText =
	"# a file for all your goat passwords.\n"
	+ "# edit as appropriate and save as config/passwords.properties\n"
	+ "#\n"
	+ "# auth.hash is the hash of the password required to become\n"
	+ "#   goat's master and give him admin commands at runtime\n"
	+ "#\n"
	+ "# irc.* is the crud goat needs to connect to his irc network\n"
	+ "#   and log in with his registered nick\n"
	+ "#\n"
	+ "# gmail.* is self-explanatory\n"
	+ "#\n"
	+ "# twitter.* is the crud for connecting to tweeter's api\n"
	+ "#\n"
	+ "# this file may be saved by goat at runtime; it's probably\n"
	+ "#   a good idea to avoid editing this file manually on a\n"
	+ "#   live goat, on the off chance that you clobber\n"
	+ "#   a password change.\n"
	+ "#\n"
	+ "# the text of this comment is autogenerated if you see a\n"
	+ "# timestamp below.\n"
	+ "#\n"
	+ "# edits to this comment text should go\n"
	+ "# into src/goat/Goat.java first.\n"
	+ "#\n";
}