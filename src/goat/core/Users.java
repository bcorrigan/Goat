package goat.core;

import goat.core.User;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.NoSuchElementException;


/* Goat keeps one nice static Users object in Goat.users.  Use that one instead of 
  creating your own and writing all over goat's files.
*/

public class Users {
	private HashMap<String,User> usersHash = new HashMap<String,User>() ;

	private String usersFilename = "resources/weatherUsers_new.xml";
	
	public Users() {
		usersHash = readFromDisk(usersFilename) ;
	}

	public void save() {
		saveToDisk(usersHash, usersFilename) ;
	}
	
	public void saveToDisk(HashMap<String, User> saveUsers, String filename) {
        XMLEncoder XMLenc = null;
		try {
			XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(filename)));
			XMLenc.writeObject(saveUsers);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} finally {
            if(XMLenc!=null) XMLenc.close();
        }
	}
	
	public static HashMap<String, User> readFromDisk(String filename) {
        XMLDecoder XMLdec = null;
        HashMap<String, User> diskUsers = new HashMap<String, User>() ;
		try {
			XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream(filename)));
			diskUsers = (HashMap<String,User>) XMLdec.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			diskUsers = new HashMap<String,User>();
		} catch (NoSuchElementException e) {
			diskUsers = new HashMap<String,User>();
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		} finally {
            if(XMLdec!=null) XMLdec.close();
        }
		return diskUsers ;
	}
	
	public boolean hasUser(String name) {
		return usersHash.containsKey(name.toLowerCase()) ;
	}
	
	public boolean hasUser(User u) {
		return usersHash.containsKey(u.getName().toLowerCase()) ;
	}
	
	public User getUser(String name) {
		return usersHash.get(name.toLowerCase()) ;
	}
	
	public void addUser(User u) {
		if (! u.getName().equals("")) {
			usersHash.put(u.getName().toLowerCase(), u) ;
		}
		save() ;
	}
}
