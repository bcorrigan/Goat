package goat.core;

import java.util.SortedMap;

//will become org.apache.jdbm soon
import net.kotek.jdbm.*;

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
 * @author bcorrigan
 */
public class KVStore<T> {
	private static DB db;
	private SortedMap<String,T>  map;
	public KVStore(String mapName) {
		initDB();
		map = db.getTreeMap(mapName);
		if(map==null)
			map = db.createTreeMap(mapName);
	}
	
	public void save(String key, T value) {
		map.put(key,  value);
		db.commit();
	}
	
	public T get(String key) {
		return map.get(key);
	}
	
	public boolean has(String key) {
		return map.containsKey(key);
	}
	
	public boolean hasValue(T value) {
		return map.containsValue(value);
	}
	
	private static void initDB() {
		if(db==null)
			db = DBMaker.openFile("resources/goatdb")  
			    .deleteFilesAfterClose()
			    //.enableEncryption("password",false)
			    .make();
	}
	
	public static boolean hasStore(String store) {
		initDB();
		return db.getTreeMap(store)!=null;
	}
	
	public static void main(String[] args) {
		KVStore<String> store = new KVStore<String>("somemap");
		store.save("key", "value2");
		System.out.println("OK, saved");
		String thingy = store.get("key");
		System.out.println("Got:" + thingy);
	}
}