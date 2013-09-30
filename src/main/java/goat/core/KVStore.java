package goat.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.Map;

import org.mapdb.*;

import biz.source_code.base64Coder.Base64Coder;

import java.io.*;

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
 * (ConcurrentNavigableMap implements SortedMap)
 *
 * Therefore, KVStore controls the namespace, and the namespace is used to index onto just
 * ONE map that goat creates.
 *
 *
 * @author bcorrigan
 */
//TODO should really be <T extends Serializable> I guess :-(
public class KVStore<T> implements Map<String, T> {
    private static DB db;
    private static ConcurrentNavigableMap<String,Object>  globalMap;
    private ConcurrentNavigableMap<String,T> mapSlice;
    private String ns;

    static {
	if(db==null) {
	    File file = new File("resources/goatdb").getAbsoluteFile();
	    System.out.println("DBFILE:" + file.getAbsolutePath());
	    db = DBMaker.newFileDB(file)
		//.deleteFilesAfterClose()
		.closeOnJvmShutdown()
		//.asyncWriteDisable()
		//.enableEncryption("password",false)
		.make();
	    //db.clearCache();
	    //db.defrag(true);

	    globalMap=db.getTreeMap("globalMap");
	}
    }

    public void compact() {
	db.compact();
    }

    //dump the entire global map: keys, types, and values.
    public void dump(String backupFile) throws IOException {
        File file = new File(backupFile);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for(String key : globalMap.keySet()) {
            //System.out.print("key:" + key);
            String value = toBase64String((Serializable) globalMap.get(key));
            String type=value.getClass().getName();
            //System.out.println(":type:" + value.getClass().getName());
            //TODO escape this
            bw.write(key+"13DELIM37"+type+"13DELIM37"+value+'\n');
        }
        bw.flush();
        bw.close();
    }

    //load a new global map that replaces existing global map.
    public StringBuilder load(String backupFile) throws IOException {
	try {
	    StringBuilder duffObjects = new StringBuilder();
	    File file = new File(backupFile);
	    FileReader fr = new FileReader(file.getAbsoluteFile());
	    BufferedReader br = new BufferedReader(fr);
	    String line;
	    globalMap.clear();
	    while((line=br.readLine())!=null) {
		//TODO escaping!!!!
		String[] vals = line.split("13DELIM37");
		String key = vals[0];
		String type = vals[1];
		Object o;
		try {
		    o = fromBase64String(vals[2]);
		} catch (ClassNotFoundException e) {
		    System.out.println("Couldn't instantiate " + key + " --SKIPPING");
		    duffObjects.append("key:").append(key).append(":type:").append(type).append("||");
		    continue;
		}
		globalMap.put(key, o);
	    }
	    db.commit();
	    return duffObjects;
	} catch(IOException ioe) {
	    System.out.println("ioe during load - Rolling back!");
	    ioe.printStackTrace();
	    db.rollback();
	    throw ioe;
	}
    }

    //caution - this returns a global store! handy for Dbutils but not otherwise
    public KVStore() {
	ns="";
	mapSlice=(ConcurrentNavigableMap<String,T>) globalMap;
    }

    public KVStore(String nameSpace) {
	//nameSpace is eg "user." or "user.bc." or "channel." or "channel.#jism."
	//we need the slice of all keys that match this "address"
	//wtf-worthy :(
	ns=nameSpace;

	String toKey = incString(nameSpace);
	mapSlice = (ConcurrentNavigableMap<String,T>) globalMap.subMap(nameSpace, toKey);
    }

    public KVStore<T> subStore(String subNs) {
	return new KVStore<T>(ns+subNs+".");
    }

    public void save(String key, T value) {
        debugWrite(value, "save()");
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

    public static <T> KVStore<T> getModuleStore(String moduleName, String prefix) {
	return new KVStore<T>(moduleName+"."+prefix+".");
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
        debugWrite(value, "put()");
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
            T value = m.get(key);
            debugWrite(value, "putAll");
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
	Set<String> keySet = mapSlice.keySet();
	Set<String> trimmedKeys = new HashSet<String>(keySet.size());
	for(String key : keySet) {
	    trimmedKeys.add(key.replaceFirst(ns, ""));
	}
	return trimmedKeys;
    }

    @Override
	public Collection<T> values() {
	return mapSlice.values();
    }

    @Override
	public Set<java.util.Map.Entry<String, T>> entrySet() {
	return mapSlice.entrySet();
    }

    //returns all matching keys for a given string
    public static List<String> findKeys(String keyQuery) {
	ArrayList<String> matchingKeys = new ArrayList<String>();
	for(String key: globalMap.keySet()) {
	    if(key.matches(keyQuery))
		matchingKeys.add(key);
	}
	return matchingKeys;
    }

    //serialise and deserialise the store
    /** Read the object from Base64 string. */
    private static Object fromBase64String( String s ) throws IOException ,
	ClassNotFoundException {
        byte [] data = Base64Coder.decode( s );
        ObjectInputStream ois = new ObjectInputStream(
						      new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }

    /** Write the object to a Base64 string. */
    private static String toBase64String( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return new String( Base64Coder.encode( baos.toByteArray() ) );
    }

    private void debugWrite(T value, String methodName) {
        Class klass = value.getClass();
        if(klass != String.class
           && klass != int.class
           && klass != long.class
           && klass != double.class
           && klass != Integer.class
           && klass != Long.class
           && klass != Double.class) {
            System.out.println("WARNING - KVStrore: attempting to " + methodName + " value of type " + klass.getName());
            Thread.dumpStack();
        }

    }
}
