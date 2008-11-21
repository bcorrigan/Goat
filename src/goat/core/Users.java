package goat.core;

import goat.core.User;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/* Goat keeps one nice static Users object in Goat.users.  Use that one instead of 
  creating your own and writing all over goat's files.
*/

public class Users {
	private HashMap<String,User> usersHash = new HashMap<String,User>() ;

	private String usersFilename = "resources/users.xml";
	
	public Users() {
		try {
			usersHash = readFromDisk(usersFilename) ;
		} catch (Exception e) {
			usersHash = new HashMap<String, User>();
		}
	}

	public void save() {
		saveToDisk(usersHash, usersFilename) ;
	}
	
	private void saveToDisk(HashMap<String, User> saveUsers, String filename) {
		FileOutputStream fos = null;
        XMLEncoder XMLenc = null;
        if(! saveUsers.isEmpty()) {
        	try {
        		fos = new FileOutputStream(filename);
        		XMLenc = new XMLEncoder(new BufferedOutputStream(fos));
        		XMLenc.writeObject(saveUsers);
        	} catch (FileNotFoundException fnfe) {
        		fnfe.printStackTrace();
        		// This seems to happen on Windows sometimes
        		System.out.println("Users:  users.xml not saved.");
        	} finally {
        		if(XMLenc!=null) XMLenc.close();
        		try {
        			if(fos != null) fos.close();
        		} catch (IOException ioe) {
        			System.out.println("I couldn't close the users.xml file after writing it!");
        			ioe.printStackTrace();
        		}
        	}
        }
	}
	
	private HashMap<String, User> readFromDisk(String filename) {
        FileInputStream fis = null;
        XMLDecoder XMLdec = null;
        HashMap<String, User> diskUsers = new HashMap<String, User>() ;
		try {
			fis = new FileInputStream(filename);
			XMLdec = new XMLDecoder(new BufferedInputStream(fis));
			diskUsers = (HashMap<String,User>) XMLdec.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			diskUsers = new HashMap<String,User>();
// not sure why this was here...
//		} catch (NoSuchElementException e) {
//			diskUsers = new HashMap<String,User>();
//			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		} finally {
            if(XMLdec!=null) XMLdec.close();
            try {
            	if(fis!=null) fis.close();
            } catch (IOException ioe) {
            	System.out.println("I couldn't close the users.xml file after reading it!");
            	ioe.printStackTrace();
            }
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
