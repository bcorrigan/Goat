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
    
    KVStore<String> strStore = new KVStore<String>("userstore.");
    static KVStore<String[]> nmStore = new KVStore<String[]>("userstore.");
    
    public boolean hasUser(String name) {
        return strStore.has(name.toLowerCase()+".name");
    }

    public User getUser(String name) {
        if(!hasUser(name)) {
            noteName(name.toLowerCase());
        }
        
        return new User(name.toLowerCase());
    }

    public User getOrCreateUser(String uname) {
        return getUser(uname);
    }
    
    public List<User> getAllUsers() {
        if(nmStore.has("names")) {
            String[] names = nmStore.get("names");
            List<User> users = new ArrayList<User>(names.length);
            for(String nm : names) {
                User user = getUser(nm);
                users.add(user);
            }
            return users;
        } else 
            return new ArrayList<User>(0);
    }
    
    //all users active within the supplied duration
    public List<User> getActiveUsers(long duration) {
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
    public List<User> getUsersWith(String property, long duration) {
        List<User> activeUsers = getActiveUsers(duration);
        List<User> definedUsers = new ArrayList<User>(10);
        
        for(User user : activeUsers) {
            if(user.has(property))
                definedUsers.add(user);
        }
        
        return definedUsers;
    }
    
    //when we have JDK8 with lambdas replace this with soemthing generic!
    public List<User> getActiveUsersFollowing(String screenName, long duration) {
        List<User> users = getActiveUsers(duration);
        List<User> followingUsers = new ArrayList<User>(5);
        for(User user : users) 
            for(String following : user.getFollowing()) 
                if(screenName.equals(following)) {
                    followingUsers.add(user);
                    break;
                }
        
        return followingUsers;
    }
    
    public Set<String> channels(List<User> users) {
        Set<String> chans = new HashSet<String>(2);
        
        for(User user : users) {
            chans.add(user.getLastChannel());
        }
        
        return chans;
    }
    
    
    //note all user names as an array within one property for easy lookup without scanning
    private void noteName(String uname) {
        if(nmStore.has("names")) {
            String[] oldNames = nmStore.get("names");
            String[] newNames = Arrays.copyOf(oldNames,oldNames.length+1);
            newNames[oldNames.length]=uname;
            nmStore.save("names", newNames);
        } else {
            nmStore.save("names", new String[]{uname});
        }
    }
}
