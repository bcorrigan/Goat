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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Map.Entry;

/* Goat keeps one nice static Users object in Goat.users.  Use that one instead of
   creating your own and writing all over goat's files.
*/

public class Users implements Runnable {

    public final static String DEFAULT_USERS_FILENAME = "resources/users.xml" ;

    private ConcurrentHashMap<String,User> usersHash = new ConcurrentHashMap<String,User>() ;
    private String usersFilename = DEFAULT_USERS_FILENAME;
    private Boolean updatesPending = false;
    protected Object writeLock = new Object();

    private boolean isDoneReadingUsersFileOnStartup = false;

    public Users() {
        try {
            synchronized (writeLock) {  // shouldn't have to synchronize in constructor, but do this if it's moved outside the constructor.
                readUsersFromDisk() ;
                isDoneReadingUsersFileOnStartup = true;
            }
        } catch (Exception e) {
            System.err.println("Error reading users from disk!");
            // usersHash = new ConcurrentHashMap<String, User>();
        }
        new Thread(this).start();
    }

    /*
      public void save() {
      saveToDisk() ;
      }
    */

    private void saveUsersToDisk() {
        FileOutputStream fos = null;
        XMLEncoder XMLenc = null;
        //synchronized (writeLock) {
        if(! usersHash.isEmpty()) {
            try {
                fos = new FileOutputStream(usersFilename);
                XMLenc = new XMLEncoder(new BufferedOutputStream(fos));
                synchronized(usersHash) {
                    XMLenc.writeObject(usersHash);
                }
                updatesPending = false;
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
        //}
    }


    /*
     * Read users in from disk.
     *
     * The file should be in XMLEncoder-encoded format.
     *
     * Beyond that, it can contain either a single Map (of any kind) of all users,
     * or plain User objects listed one after the other.
     *
     * When storing the data read in to usersHash, the key will be User.name in the case where
     * there are just a bunch of User objects in the file, or whatever key was used for the
     * stored Map, in the case of one big Map of all users.
     *
     * This should allow us to change the internal Users representation and/or save
     * format a little without having to completely rewrite this method.
     */
    private void readUsersFromDisk() {
        FileInputStream fis = null;
        XMLDecoder XMLdec = null;
        synchronized (writeLock) {
            try {
                fis = new FileInputStream(usersFilename);
                XMLdec = new XMLDecoder(new BufferedInputStream(fis),null,null,User.class.getClassLoader());
                Object decodedXML = XMLdec.readObject();
                if(decodedXML instanceof Map) {
                    Map<?,?> map = (Map<?,?>) decodedXML;
                    for(Entry<?,?> entry: map.entrySet()) {
                        String key;
                        User value;
                        if(entry.getValue() instanceof User) {
                            value = (User) entry.getValue();
                            value.setContainer(this);
                            // change the block below if you want to use something other than
                            // the saved Map key as the key for the new user in userHash
                            if(entry.getKey() instanceof String) {
                                key = (String) entry.getKey();
                                usersHash.put(key, value);
                            }
                        }
                    }
                } else if(decodedXML instanceof User) {
                    User u = (User) decodedXML;
                    try {
                        do {
                            if (decodedXML instanceof User) {
                                u = (User) decodedXML;
                                u.setContainer(this);
                                usersHash.put(u.getName(), u);
                            } else {
                                String str = "While reading Users save file \"" + usersFilename + "\":"
                                    + "\n   expected goat.module.User, instead got ";
                                if(null != decodedXML.getClass().getCanonicalName())
                                    str += decodedXML.getClass().getCanonicalName();
                                else
                                    str += "an object without a canonical name.  Whoa.";
                                System.out.println(str);
                            }
                        } while ((decodedXML = XMLdec.readObject()) instanceof Object);
                    } catch (ArrayIndexOutOfBoundsException ioe) {
                        // end of file, expected
                    }
                } else {
                    System.out.println("Buh? unexpected serialized class in users.xml: " + decodedXML.getClass());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // not sure why this was here...
                //		} catch (NoSuchElementException e) {
                //			diskUsers = new HashMap<String,User>();
                //			e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Users savefile \"" + usersFilename + "\" contained no XMLencoded java objects");
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
        }
    }


    public boolean hasUser(String name) {
        return usersHash.containsKey(name.toLowerCase()) ;
    }


    /* possibly misleading
       public boolean hasUser(User u) {
       return usersHash.containsKey(u.getName().toLowerCase()) ;
       }
    */

    public User getUser(String name) {
        return usersHash.get(name.toLowerCase()) ;
    }

    private User putUserIfAbsent(User u)
	throws NullPointerException {
        User ret;
        if(null != u) {
            u.setContainer(this);
            if(u.getName().equals(""))
                throw new IllegalArgumentException("empty string not allowed as a user name");
        }
        ret = usersHash.putIfAbsent(u.getName().toLowerCase(), u) ;
        //Uncomment here if we start using this method for anything other than adding
        //   new, empty User (ie, User with no member data set)
        //if(null == ret)
        //	notifyUpdatesPending();
        return ret;
    }

    public User getOrCreateUser(String uname)
	throws NullPointerException, IllegalArgumentException {
        User u = new User(uname);
        User ret;
        ret = putUserIfAbsent(u);
        if(null == ret)
            ret = u;
        return ret;
    }

    private Boolean running = false;
    private Boolean stop = false;
    private Integer updateIntervalInSeconds = 6;

    protected void notifyUpdatesPending() {
        synchronized(writeLock) {
            updatesPending = true;
        }
    }

    public void run() {
        running = true;
        while(!stop) {
            try {
                Thread.sleep(updateIntervalInSeconds * 1000);
            } catch (InterruptedException ie) {}
            if(updatesPending && isDoneReadingUsersFileOnStartup) {
                saveUsersToDisk();
            }
        }
        running = false;
    }

    public synchronized void stop() {
        stop = true;
    }

    public synchronized boolean isRunning() {
        return running;
    }
}
