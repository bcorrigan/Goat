package goat.core;

import goat.core.User;

/**
 * With the advent of MapDB this class is now mostly for backwards compatibility 
 * Should entirely get rid of it when someone can be arsed to do so - You can just go new User("bc") now directly
 */
public class Users {
    
    KVStore<String> strStore = new KVStore<String>("user.");

    public boolean hasUser(String name) {
        return strStore.has(name.toLowerCase()+".name");
    }

    public User getUser(String name) {
        return new User(name.toLowerCase()) ;
    }

    public User getOrCreateUser(String uname) {
        return getUser(uname);
    }
}
