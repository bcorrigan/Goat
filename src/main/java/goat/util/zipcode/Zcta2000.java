package goat.util.zipcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;

@Entity
public class Zcta2000 extends ZipCode {

	String dataFileURL() {
		return "http://www.census.gov/tiger/tms/gazetteer/zcta5.zip";
	}

	@Override
	public ZipCode getByCode(String code) {
		return get(code);
	}

	public Zcta2000(){}
	
	public Zcta2000(String code) {
		this.overwriteWith(get(code));
	}
	
	public static Zcta2000 get(String code) {
		Zcta2000 ret = new Zcta2000();
		if(! ret.hasDataCached())
			ret.importData();
		ret = null;
		try {
			PrimaryIndex<String, Zcta2000> index = getStore().getPrimaryIndex(String.class, Zcta2000.class);
			ret = index.get(code);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return ret;
	}
	
	boolean importData() {
		System.out.println("Importing Zcta2000 data from file to cache, this may take a minute...");
		boolean ret = false;
		if(! hasLocalFile()) {
			fetchDataFile();
			if (! hasLocalFile())
				return false;
		}
		try {
			EntityStore store = makeStoreWriteable();
			PrimaryIndex<String, Zcta2000> index = store.getPrimaryIndex(String.class, Zcta2000.class) ; 
			ZipInputStream zin = new ZipInputStream(new FileInputStream(localDataFilename()));
			zin.getNextEntry(); // move to start of data file in zip file
			BufferedReader in = new BufferedReader(new InputStreamReader(zin));
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				Zcta2000 zcta = new Zcta2000();
				zcta.parseLine(inputLine) ;
				index.put(zcta) ;
			}
			in.close();
			makeStoreReadOnly();
			ret = true;
			System.out.println("Zcta2000 data imported.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	private void parseLine(String line) {
		if(line.length() > 146) {
			state = line.substring(0,2);
			code = line.substring(2,7);
			population = Integer.parseInt(line.substring(66,75).trim());
			housingUnits = Integer.parseInt(line.substring(75,84).trim());
			landArea = Long.parseLong(line.substring(84,98).trim());
			waterArea = Long.parseLong(line.substring(98,112).trim());
			landAreaSqMiles = Double.parseDouble(line.substring(112,124).trim());
			waterAreaSqMiles = Double.parseDouble(line.substring(124,136).trim());
			latitude = Double.parseDouble(line.substring(136,146).trim());
			longitude = Double.parseDouble(line.substring(146,157).trim());
		}
	}
}
