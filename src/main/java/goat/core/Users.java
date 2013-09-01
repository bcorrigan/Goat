package goat.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import goat.core.User;

/**
 * Various convenience methods for dealing with bunches of users and keeping track of them and querying them
 */
public class Users {
    
    static KVStore<String> strStore = new KVStore<String>("userstore.");
    static KVStore<String[]> nmStore = new KVStore<String[]>("userstore.");
    static Set<String> userNames = new HashSet<String>();
    
    public static boolean hasUser(String name) {
        return strStore.has(name.toLowerCase()+".name");
    }

    public static User getUser(String name) {
        if(!hasUser(name)) {
            noteName(name.toLowerCase());
        }
        
        return new User(name.toLowerCase());
    }

    public static User getOrCreateUser(String uname) {
        return getUser(uname);
    }
    
    public static List<User> getAllUsers() {
        if(nmStore.has("names")) {
            //String[] names = nmStore.get("names");
            List<User> users = new ArrayList<User>(userNames.size());
            for(String nm : userNames) {
                User user = getUser(nm);
                users.add(user);
            }
            return users;
        } else 
            return new ArrayList<User>(0);
    }
    
    //all users active within the supplied duration
    public static List<User> getActiveUsers(long duration) {
        List<User> users = getAllUsers();
        List<User> activeUsers = new ArrayList<User>(10);
        
        for(User user : users) {
            if(user.isActiveWithin(duration))
                activeUsers.add(user);
        }
        
        return activeUsers;
    }
    
    //all existing users with the supplied property defined.. User.SCREENNAME, User.WEATHERSTATION, whatever
    //within specified duration
    public static List<User> getUsersWith(String property, long duration) {
        List<User> activeUsers = getActiveUsers(duration);
        List<User> definedUsers = new ArrayList<User>(10);
        
        for(User user : activeUsers) {
            if(user.has(property))
                definedUsers.add(user);
        }
        
        return definedUsers;
    }
    
    //when we have JDK8 with lambdas replace this with soemthing generic!
    public static List<User> getActiveUsersFollowing(String screenName, long duration) {
        String sn = screenName.toLowerCase();
        List<User> users = getActiveUsers(duration);
        List<User> followingUsers = new ArrayList<User>(5);
        for(User user : users) 
            for(String following : user.getFollowing()) {
                if(sn.equals(following)) {
                    followingUsers.add(user);
                    break;
                }
            }
        
        return followingUsers;
    }
    
    public static List<User> getAllUsersFollowing(String screenName) {
        String sn = screenName.toLowerCase();
        //hmn this could potentially get quite large, oh well!
        List<User> following = new ArrayList<User>();
        if(nmStore.has("names")) {
            //String[] names = nmStore.get("names");
            for(String name : userNames) {
                //userstore.bc.screenName.bbcnews
                if(strStore.has(name+"."+User.SCREENNAME+"."+sn.toLowerCase())) {
                    following.add(new User(name));
                }
            }
        }
        return following;
    }
    
    public static Set<String> channels(List<User> users) {
        Set<String> chans = new HashSet<String>(2);
        
        for(User user : users) {
            chans.add(user.getLastChannel());
        }
        
        return chans;
    }
    
    
    //note all user names as an array within one property for easy lookup without scanning
    private synchronized static void noteName(String uname) {
        //this nmStore thing was such a shitty idea, need to work out how mapdb does secondary keys, cos it does.
        if(nmStore.has("names")) {
            String[] oldNames = nmStore.get("names");
            
            for(String user: oldNames) {
                if(user!=null) {
                    userNames.add(user);
                }
            }
            
            userNames.add(uname);
            nmStore.save("names", userNames.toArray(new String[1]));
        } else {
            nmStore.save("names", new String[]{uname});
        }
    }
}
