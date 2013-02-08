package goat.util.zipcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.model.Entity;

@Entity
public class Zip1990 extends ZipCode {

	@Override
	String dataFileURL() {
		return "http://www.census.gov/tiger/tms/gazetteer/zips.zip";
	}
	
	@Override
	public ZipCode getByCode(String code) {
		return get(code);
	}
	
	public Zip1990(){}
	
	public Zip1990(String code) {
		this.overwriteWith(get(code));
	}
	
	public static Zip1990 get(String code) {
		Zip1990 ret = new Zip1990();
		if(!ret.hasDataCached())
			ret.importData();
		ret = null;
		try {
			PrimaryIndex<String, Zip1990> index = getStore().getPrimaryIndex(String.class, Zip1990.class);
			ret = index.get(code);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return ret;
	}

	
	@Override
	boolean importData() {
		System.out.println("Importing Zip1990 data from file to cache, this may take a minute...");
		boolean ret = false;
		if(! hasLocalFile()) {
			fetchDataFile();
			if (! hasLocalFile())
				return false;
		}
		try {
			EntityStore store = makeStoreWriteable();
			PrimaryIndex<String, Zip1990> index = store.getPrimaryIndex(String.class, Zip1990.class) ; 
			ZipInputStream zin = new ZipInputStream(new FileInputStream(localDataFilename()));
			zin.getNextEntry(); //move to start of data file in zip file.
			BufferedReader in = new BufferedReader(new InputStreamReader(zin));
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				Zip1990 zip = new Zip1990();
				zip.parseLine(inputLine) ;
				index.put(zip);
			}
			in.close();
			makeStoreReadOnly();
			ret = true;
			System.out.println("Zip1990 data imported.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	private void parseLine(String line) {
		line = line.trim();
		String[] vals = line.split(",", -1);
		fipsState = Short.parseShort(vals[0].replaceAll("\"",""));
		code = vals[1].replaceAll("\"", "");
		state = vals[2].replaceAll("\"","");
		name = vals[3].replaceAll("\"", "");
		longitude = - Double.parseDouble(vals[4]);
		latitude = Double.parseDouble(vals[5]);
		population = Integer.parseInt(vals[6]);
		allocationFactor = Double.parseDouble(vals[7]);
	}
}
