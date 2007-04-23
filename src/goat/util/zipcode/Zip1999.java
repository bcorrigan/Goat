package goat.util.zipcode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import com.svcon.jdbf.DBFReader;
import com.svcon.jdbf.JDBFException;

@Entity
public class Zip1999 extends ZipCode {

	@Override
	String dataFileURL() {
		return "http://www.census.gov/geo/www/tiger/zip1999.zip";
	}
	
	@Override
	public ZipCode getByCode(String code) {
		return get(code);
	}
	
	public Zip1999(){}
	
	public Zip1999(String code) {
		this.overwriteWith(get(code));
	}
	
	public static Zip1999 get(String code) {
		Zip1999 ret = new Zip1999();
		if(! ret.hasDataCached())
			ret.importData();
		ret = null;
		try {
			PrimaryIndex<String, Zip1999> index = getStore().getPrimaryIndex(String.class, Zip1999.class);
			ret = index.get(code);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return ret;
	}

	@Override
	boolean importData() {
		System.out.println("Importing Zip1999 data from file to cache, this may take a minute...");
		boolean ret = false;
		if(! hasLocalFile()) {
			fetchDataFile();
			if (! hasLocalFile())
				return false;
		}
		try {
			EntityStore store = makeStoreWriteable();
			PrimaryIndex<String, Zip1999> index = store.getPrimaryIndex(String.class, Zip1999.class) ; 
			ZipInputStream zin = new ZipInputStream(new FileInputStream(localDataFilename()));
			zin.getNextEntry();  //go to beginning of doc file in the zip file
			zin.getNextEntry();  //go to the beginning of the dbf file in the zip file
			DBFReader in = new DBFReader(zin);
			while (in.hasNextRecord()) {
				Zip1999 zip = new Zip1999();
				zip.parseRecord(in.nextRecord()) ;
				index.put(zip);
			}
			in.close();
			makeStoreReadOnly();
			ret = true;
			System.out.println("Zip1999 data imported");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		} catch (JDBFException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	private void parseRecord(Object[] fields) {
		code = (String) fields[0];
		latitude = Double.parseDouble((String) fields[1]);
		longitude = Double.parseDouble((String) fields[2]);
		zipClass = (String) fields[3];
		if (zipClass.equals(""))
			zipClass = "S";
		name = (String) fields[4];
		fipsState = Short.parseShort((String) fields[5]);
		fipsCounty = Short.parseShort((String) fields[6]);
	}
}
