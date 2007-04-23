package goat.util.zipcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

@Entity
public class CfdZip extends ZipCode {

	@Override
	String dataFileURL() {
		return "http://www.cfdynamics.com/cfdynamics/zipbase/zip_codes.zip";
	}

	@Override
	ZipCode getByCode(String code) {
		return get(code);
	}
	
	public static CfdZip get(String code) {
		CfdZip ret = new CfdZip();
		if(! ret.hasDataCached())
			ret.importData();
		ret = null;
		try {
			PrimaryIndex<String, CfdZip> index = getStore().getPrimaryIndex(String.class, CfdZip.class);
			ret = index.get(code);
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return ret;
	}

	@Override
	boolean importData() {
		System.out.println("Importing CF Dynamics data from file to cache, this may take a minute...");
		boolean ret = false;
		if(! hasLocalFile()) {
			fetchDataFile();
			if (! hasLocalFile())
				return false;
		}
		try {
			EntityStore store = makeStoreWriteable();
			PrimaryIndex<String, CfdZip> index = store.getPrimaryIndex(String.class, CfdZip.class) ; 
			ZipInputStream zin = new ZipInputStream(new FileInputStream(localDataFilename()));
			zin.getNextEntry(); //move to start of data file in zip file.
			BufferedReader in = new BufferedReader(new InputStreamReader(zin));
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				CfdZip zip = new CfdZip();
				zip.parseLine(inputLine) ;
				index.put(zip);
			}
			in.close();
			makeStoreReadOnly();
			ret = true;
			System.out.println("CF Dynamics ZIP code data imported.");
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
		code = vals[0].replaceAll("\"", "");
		if (! vals[1].equals(""))
			latitude = Double.parseDouble(vals[1].replaceAll("\"", ""));
		if (! vals[2].equals(""))
			longitude = - Double.parseDouble(vals[2].replaceAll("\"", ""));
		name = vals[3].replaceAll("\"", "");
		county = vals[5].replaceAll("\"","");
		state = vals[4].replaceAll("\"","");
		String zc = vals[6].replaceAll("\"", "");
		if (zc.equals("STANDARD"))
			zipClass = "S";
		else if (zc.equals("PO BOX ONLY"))
			zipClass = "P";
		else if (zc.equals("UNIQUE"))
			zipClass = "U";
		else if (zc.equals("MILITARY"))
			zipClass = "M";
	}
}

