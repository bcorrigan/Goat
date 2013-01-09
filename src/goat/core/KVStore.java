package goat.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map;
//will become org.apache.jdbm soon
import net.kotek.jdbm.*;

import static goat.util.StringUtil.incString;

/**
 * Simple wrapper to store key/values.
 * A full-blown DB in goat is too much. We want something piss easy.
 * So lets adventure into the feature-shorn world of NOSQL.
 * 
 * This class will simply wrap JDBM for now.
 * 
 * In Module will be found methods such as saveChannel, saveUser, saveServer etc
 * 
 * Underlying engine is totally thread safe - so have at it.
 * 
 * Current implementation should be improved as follows:
 * 
 * Instead of using a seperate map per module/user/channel,
 * we should take advantage of the map being a SortedMap.
 * 
 * SortedMap has a sub method, that allows us to fetch keys within a specified range.
 * 
 * Therefore, KVStore controls the namespace, and the namespace is used to index onto just
 * ONE map that goat creates. 
 * 
 * 
 * @author bcorrigan
 */
public class KVStore<T> implements Map<String, T> {
	private static DB db;
	private static SortedMap<String,Object>  globalMap;
	private SortedMap<String,T> mapSlice;
	private String ns;
	
	static {
		if(db==null) {
			db = DBMaker.openFile("resources/goatdb")  
			    .deleteFilesAfterClose()
			    //.enableEncryption("password",false)
			    .make();
			globalMap=db.getTreeMap("globalMap");
			if(globalMap==null)
				globalMap = db.createTreeMap("globalMap");
		}
	}
	
	public KVStore(String nameSpace) {
		//nameSpace is eg "user." or "user.bc." or "channel." or "channel.#jism."
		//we need the slice of all keys that match this "address"
		//wtf-worthy :(
		ns=nameSpace;
		
		String toKey = incString(nameSpace);
		mapSlice = (SortedMap<String,T>) globalMap.subMap(nameSpace, toKey);
	}
	
	public void save(String key, T value) {
		mapSlice.put(ns+key,  value);
		db.commit();
	}
	
	public T get(String key) {
		return mapSlice.get(ns+key);
	}
	
	public boolean has(String key) {
		return mapSlice.containsKey(ns+key);
	}
	
	public boolean hasValue(T value) {
		return mapSlice.containsValue(value);
	}
	
	/*public Map getMap() {
		return mapSlice;
	}*/
	
	public void save() {
		db.commit();
	}
	
	public void incSave(String prop) {
		incSave(prop, 1);
	}
	
	public void incSave(String propName, int inc) {
		if(has(propName)) {
			if(inc>0) {
				Integer prop = (Integer) get(propName);
				prop=prop+inc;
				save(propName,(T) prop);
			}
		} else {
			save(propName, (T) new Integer(inc));
		}
	}
	
	public T getOrElse(String key, T defaultObj) {
		if(has(key)) 
			return get(key);
		else
			return defaultObj;
	}
	
	public void rollback() {
		db.rollback();
	}
	
	public static boolean hasStore(String ns) {
		return globalMap.subMap(ns, incString(ns)).size()>0;
	}
	
	public static <T> KVStore<T> getChanStore(Message m) {
		return new KVStore<T>("chan."+m.getChanname()+".");
	}
	
	public static <T> KVStore<T> getUserStore(Message m) {
		return new KVStore<T>("user."+m.getSender()+".");
	}
	
	public static <T> KVStore<T> getUserStore(String user) {
		return new KVStore<T>("user."+user+".");
	}
	
	public static <T> KVStore<T> getAllUsersStore() {
		return new KVStore<T>("user.");
	}
    public static <T> KVStore<T> getCustomStore(String custom) {
        return new KVStore<T>("custom."+custom+".");
    }
	
	//should return a list of all known user names
	public static Set<String> getAllUsers() {
		Map<String,Object> allUsers = getAllUsersStore();
		Set<String> users = new HashSet<String>();
		for(String prop : allUsers.keySet()) {
			//always user.something.. so:
			String user = prop.replaceFirst("user\\.", "").replaceFirst("\\..*", "");
			users.add(user);
		}
		
		return users;
	}
	
	//for supplied property, should return a map of userNames to value - for that property. Not writable.
	public static <T> Map<String,T> getAllUsers(String key) {
		Set<String> users = getAllUsers();
		Map<String,T> userToProps = new HashMap<String,T>();
		for(String user : users) {
			KVStore<Object> store = new KVStore<Object>("user." + user + ".");
			if(store.has(key)) {
				userToProps.put(user, (T) store.get(key)); 
			}
		}
		return userToProps;
	}
	
	public static boolean hasUserStore(String user) {
		return KVStore.hasStore("user."+user+".");
	}
	
	public static <T> KVStore<T> getModuleStore(String moduleName) {
		return new KVStore<T>(moduleName+".");
	}

	public static void main(String[] args) {
		KVStore<String> store = new KVStore<String>("user.user1.");
		store.save("blaha", "value1");
		System.out.println("OK, saved v1");
		store.save("blahb", "value2");
		System.out.println("saved2");

		String thingy = store.get("blaha");
		System.out.println("Got:" + thingy);
		
		//make a new store (which points into different slice of the same map!)
		store = new KVStore<String>("user.user2.");
		store.save("blaha", "this is against user2 value1");
		store.save("blahc", "this is against user2 value2");
		
		
		//now the difficult part, can we retrieve store1 again?
		store = new KVStore<String>("user.user1.");
		String val = store.get("blaha");
		System.out.println("Got from store1::::" + val);
		//we should NOT see store2 data here though
		val = store.get("blahc");
		System.out.println("This shoudl be null:" + val);
		
		Set<String> users = getAllUsers();
		for(String user: users) {
			System.out.println("Got:" + user);
		}
		
		Map<String,String> userToProps = getAllUsers("blaha");
		for(String user : userToProps.keySet()) {
			System.out.println("User:" + user + ":has blaha:" + userToProps.get(user));
		}
		
		KVStore<Integer> storeInt = new KVStore<Integer>("user.user1.");
		storeInt.save("myInt", 0);
		storeInt.incSave("myInt",5);
		storeInt.incSave("myInt",4);
		System.out.println("So it is:" + storeInt.get("myInt"));
	}

	@Override
	public int size() {
		return mapSlice.size();
	}

	@Override
	public boolean isEmpty() {
		return mapSlice.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return mapSlice.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public T get(Object key) {
		return mapSlice.get(ns+key);
	}

	@Override
	public T put(String key, T value) {
		return mapSlice.put(ns+key, value);
	}

	@Override
	public T remove(Object key) {
		return mapSlice.remove(ns+key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		Map<String, T> mNsCopy = new HashMap<String, T>(m.size());
		for(String key:m.keySet()) {
			mNsCopy.put(ns+key, m.get(key));
		}
		mapSlice.putAll(mNsCopy);
	}

	@Override
	public void clear() {
		mapSlice.clear();
	}

	@Override
	public Set<String> keySet() {
		return mapSlice.keySet();
	}

	@Override
	public Collection<T> values() {
		return mapSlice.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, T>> entrySet() {
		return mapSlice.entrySet();
	}


}
