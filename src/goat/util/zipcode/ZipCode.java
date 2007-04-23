package goat.util.zipcode;

import java.io.File;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import static com.sleepycat.persist.model.Relationship.*;
import com.sleepycat.persist.model.SecondaryKey;
import com.sleepycat.persist.PrimaryIndex;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.Iterator;

@Persistent
abstract class ZipCode {
	
	private static Environment environment = null;
	private static EntityStore entityStore = null;
	
	private static final String STORE_NAME = "ZipStore" ; 
	
	private static final String DEFAULT_LOCAL_DIR = "resources" + File.separator + "zipcodes";
	private static String localDir = DEFAULT_LOCAL_DIR;
	private static String localCacheDir = DEFAULT_LOCAL_DIR + File.separator + "cache";
	
	/**
	 * Five-digit zip code.
	 */
	@PrimaryKey
	String code = "";
	/**
	 * Two-letter US state code.
	 */
	String state = "";
	/**
	 * County name.
	 */
	String county = "";
	/**
	 * Population.
	 */
	int population = -1;
	/**
	 * Housing units.
	 */
	int housingUnits = -1;
	/**
	 * Land area in square meters.
	 * 
	 * For statistical purposes only.
	 */
	long landArea = -1;
	/**
	 * Water area in square meters.
	 */
	long waterArea = -1;
	/**
	 * Land area in square miles.
	 */
	double landAreaSqMiles = -1.0;
	/**
	 * Water area in square miles.
	 */
	double waterAreaSqMiles = -1.0;
	/**
	 * Latitude in decimal degrees.
	 * 
	 * Negative values indicate South, positive North.
	 */
	double latitude = 91.0;
	/**
	 * Longitude in decimal degrees.
	 * 
	 * Negative values indicate West, positve East.
	 */
	double longitude = 181.0;
	
	/**
	 * Name for this zip code.
	 * 
	 * Usually the city or town where the corresponding post office is located.
	 */
	@SecondaryKey(relate=MANY_TO_ONE)
	String name = "";
	
	/**
	 * Allocation factor.
	 * 
	 * This number denotes the percentage of the state that is in this zip code.
	 */
	double allocationFactor = -1.0;
	
	short fipsState = -1;
	
	short fipsCounty = -1;
	
	String zipClass = "";
	
	
	/**
	 * @return URL where the data file can be found.
	 */
	abstract String dataFileURL();
	
	abstract ZipCode getByCode(String code);
	
	abstract boolean importData();
	
	public void overwriteWith(ZipCode source) {
		if(source == null)
			return;
		if(code.equals("") && !source.code.equals(""))
			code = source.code;
		else if(!code.equals(source.code))
			return;
		if(source.state != "")
			state = source.state;
		if(source.county != "")
			county = source.county;
		if(source.population >= 0)
			population = source.population;
		if(source.housingUnits >= 0)
			housingUnits = source.housingUnits;
		if(source.landArea >= 0)
			landArea = source.landArea;
		if(source.waterArea >= 0)
			waterArea = source.waterArea;
		if(source.landAreaSqMiles >= 0.0)
			landAreaSqMiles = source.landAreaSqMiles;
		if(source.waterAreaSqMiles >= 0.0)
			waterAreaSqMiles = source.waterAreaSqMiles;
		if((source.latitude >= -90.0) && (source.latitude <= 90.0))
			latitude = source.latitude;
		if((source.longitude >= -180.0) && (source.longitude <= 180.0))
			longitude = source.longitude;
		if(! source.name.equals(""))
			name = source.name;
		if(source.allocationFactor >= 0.0)
			allocationFactor = source.allocationFactor;
		if(source.fipsState >=0 )
			fipsState = source.fipsState;
		if(source.fipsCounty >= 0)
			fipsCounty = source.fipsCounty;
		if(source.zipClass != "")
			zipClass = source.zipClass;
	}
	
	public final static void destroyCache() {
		closeCache();
		File cacheDir = new File(localCacheDir);
		String[] files = cacheDir.list();
		if (files != null)
			for(int i=0;i<files.length;i++)
				new File(files[i]).delete();
		cacheDir.delete();
	}
	
	public final static boolean init() {
		return openCache();
	}
	
	/**
	 * Initialize the cache, creating it if necessary.
	 * 
	 * This shouldn't be strictly necessary, as the cache should be set up on the fly
	 * by the accessor methods if it doesn't exist, but since setting up the cache
	 * can be resource-intensive, you can use this method to get it out of the way
	 * up front.
	 * 
	 * @return
	 */
	public final static boolean openCache() {
		boolean ret = false;
		File storeDir = new File(localCacheDir) ;
		if (! storeDir.isDirectory())
			storeDir.mkdirs();
		try {
			environment = getEnvironment();
			makeStoreWriteable(); // this makes sure the store is created, if it doesn't exist.
			entityStore = makeStoreReadOnly();
			ret = true;
		} catch (DatabaseException dbe) {
			System.out.println("Couldn't open Berkeley DB Environment for ZipCode using directory \"" + localCacheDir +"\"\n");
			dbe.printStackTrace();
		}
		return ret;
	}
		
	protected final static Environment getEnvironment() throws DatabaseException {
		Environment ret = null;
		if (environment != null)
			ret = environment;
		else {
			File storeDir = new File(localCacheDir) ;
			if (! storeDir.isDirectory())
				storeDir.mkdirs();
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setReadOnly(false);
			envConfig.setAllowCreate(true);
			ret = new Environment(new File(localCacheDir), envConfig);
			environment = ret;
		}
		return ret;
	}
	
	protected final static EntityStore getStore() throws DatabaseException {
		EntityStore ret = null;
		if (entityStore != null)
			ret = entityStore;
		else {
			ret = makeStoreReadOnly();
			entityStore = ret;
		}
		return ret;
	}
	
	/**
	 * Get a writable EntityStore.
	 * 
	 * You are responsible for closing the EntityStore returned by this method.
	 * 
	 * @return
	 * @throws DatabaseException
	 */
	protected final static EntityStore makeStoreWriteable() throws DatabaseException {
		if (entityStore != null)
			if(entityStore.getConfig().getReadOnly())
				entityStore.close();
			else
				return entityStore;
		EntityStore ret = null;
		StoreConfig sc = new StoreConfig();
		sc.setReadOnly(false);
		sc.setAllowCreate(true);
		ret = new EntityStore(getEnvironment(), STORE_NAME, sc);
		entityStore = ret;
		return ret;
	}
	
	protected final static EntityStore makeStoreReadOnly() throws DatabaseException {
		StoreConfig sc = new StoreConfig();
		sc.setReadOnly(true);
		sc.setAllowCreate(true);
		if (entityStore != null)
			if(! entityStore.getConfig().getReadOnly()) {
				entityStore.close();
				entityStore = new EntityStore(getEnvironment(), STORE_NAME, sc);
				return entityStore;
			} else
				return entityStore;
		else {
			makeStoreWriteable();
			entityStore = new EntityStore(getEnvironment(), STORE_NAME, sc);
			return entityStore;
		}
	}
	
	/**
	 * Closes your EntityStore and Envirionment.
	 * 
	 * Since read-only EntityStores and Environments are Singletons in the zipcode package, 
	 * this method will close all read-only EntityStores and Environments.  Writable
	 * EntityStores and Environments must be closed by their creators. 
	 *
	 */
	public final static void closeCache() {
		if (environment != null) {
			try {
				if (entityStore != null)
					entityStore.close();
			    if (environment != null) {
			        environment.cleanLog(); // Clean the log before closing
			        environment.close();
			    } 
			} catch (DatabaseException dbe) {
			    System.err.println("Problem closing Berkeley DB setup for ZipCode.");
			    dbe.printStackTrace();
			}
			entityStore = null;
			environment = null;
		}
	}
	
	public final static void setDataDir(String path) {
		localDir = path;
		localCacheDir = path + File.separator + "cache";
	}
	
	public String localDataFilename() {
		String ret = "" ;
		try {
			URL url = new URL(dataFileURL()) ;
			ret = localDir + File.separator + new File(url.getPath()).getName();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public final boolean hasLocalFile() {
		boolean ret = false;
		File f = new File(localDataFilename()) ;
		if (f.isFile())
			ret = true;
		return ret;
	}
	
	public final boolean hasDataCached() {
		boolean ret = false;
		try {
			boolean foundReadOnly = getStore().getConfig().getReadOnly();
			if (! getStore().getModel().getKnownClasses().contains(this.getClass().getName())) {
				// this disgusting hack is required in case the primary index doesn't exist yet, in which case getPrimaryIndex will try to create the index, which it can't do if the EntityStore is read-only.
				makeStoreWriteable();
				//debug
				//System.out.println("hasDataCached() did not find class \"" + this.getClass().getName() + "\" in the cache") ;
				//System.out.println("Cached classes:  " + setList(getStore().getModel().getKnownClasses()));
			}
			PrimaryIndex pi = getStore().getPrimaryIndex(String.class, this.getClass());
			if (pi.count() > 0)
				ret = true;
			if(foundReadOnly)
				makeStoreReadOnly(); // clean up hack
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
		return ret;
	}
	
	private String setList(Set s) {
		String ret = "";
		Iterator iter = s.iterator();
		while(iter.hasNext()) {
			ret += "\"" + iter.next() + "\" ";
		}
		return ret.trim();
	}
	
	protected void fetchDataFile() {
		System.out.println("Attempting to fetch data file from:  " + dataFileURL());
		try {
			URL url = new URL(dataFileURL()) ;
			String outFile = localDataFilename() ;
			File outDir = new File(localDir) ;
			if (! outDir.isDirectory())
				outDir.mkdirs();
			OutputStream out = new FileOutputStream(outFile);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				System.out.println("File not found at:  " + dataFileURL()) ;
				return;
			}
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				System.out.println("Server error " + connection.getResponseCode() + " connecting to " + dataFileURL()) ;
				return;
			}
			InputStream in = connection.getInputStream() ;
			byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
	        out.close();
	        in.close();
	        System.out.println("Data file fetched.");
		} catch (MalformedURLException e) {
			System.out.println("Bad URL:  " + dataFileURL());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Problem connecting to:  " + dataFileURL());
			e.printStackTrace();
		}		
	}

	@Override
	public String toString() {
		return 
			code + ";" +
			zipClass + ";" +
			name + ";" +
			state + ";" +
			county + ";" +
			latitude + ";" +
			longitude + ";" +
			population + ";" +
			housingUnits + ";" +
			landArea + ";" +
			waterArea + ";" +
			landAreaSqMiles + ";" +
			waterAreaSqMiles + ";" +
			allocationFactor + ";" +
			fipsState + ";" +
			fipsCounty;
	}
	
	public String toVerboseString() {
		String ret = "" ;
		if((code == null) || code.equals(""))
			return ret;
		else 
			ret = "ZIP code " + code;
		if((!name.equals("") || !state.equals("")) && ! zipClass.equals("M")) {
			if (population <= 0)
				ret += " is";
			ret += " in ";
			if(! name.equals(""))
				ret += capitalizeEachWord(name);
			if(! name.equals("") && !state.equals(""))
				ret += ", ";
			if(state != "") {
				if ((!county.equals("")) && (!countyDesignation().equals("")))
					ret += capitalizeEachWord(county) + " " + countyDesignation() + ", ";
				else
					System.out.println("not including county \"" + county + "\"");
				ret += state;
			}
		}
		if(population > 0) {
			ret += " is home to " + population + " souls";
			if(allocationFactor > 0.0) {
				ret += String.format(" (%.1f%% of the state)", allocationFactor * 100.0);
			}
			if(housingUnits > 0)
				ret += " living in " + housingUnits + " dwellings";
		}
		ret += ".  ";
		if((latitude >= -90.0) && (latitude <= 90.0)
				&& (longitude >= -180.0) && (longitude <= 180.0)) {
			ret += "It is situated at " + prettyCoords(latitude, longitude);
			if((landAreaSqMiles > 0.0) || (waterAreaSqMiles > 0.0)) {
				ret += " and encompasses ";
				if (landAreaSqMiles > 0.0)
					ret += String.format("%.2f square miles of land", landAreaSqMiles);
				if ((landAreaSqMiles > 0.0) && (waterAreaSqMiles > 0.0))
					ret += " and ";
				if(waterAreaSqMiles > 0.0)
					ret += String.format("%.2f square miles of water", waterAreaSqMiles);
			}
			ret += ".  ";
		}
		if((fipsState >= 0) || (fipsCounty >=0)) {
			ret += "FIPS code: ";
			if (fipsState >= 0)
				ret += "state " + fipsState;
			if((fipsCounty >= 0) && (fipsState >=0))
				ret += ", ";
			if(fipsCounty >= 0)
				ret += "county " + fipsCounty;
			ret += ".  ";
		}
		if(zipClass != "")
			if (zipClass.equalsIgnoreCase("M"))
				ret += "This is an APO/FPO Military Zip Code.";
			else if(zipClass.equalsIgnoreCase("P"))
				ret += "This is a P.O. Box Zip Code.";
			else if(zipClass.equalsIgnoreCase("U"))
				ret += "This is a single-address (unique) Zip Code.";
			else if(zipClass.equalsIgnoreCase("S"))
				ret += "";
			else
				ret += "This Zip Code has an unknown class designation: \"" + zipClass + "\"";
		return ret;
	}
	
	public String capitalizeEachWord(String s) {
		if (s == null)
			return s;
		String[] words = s.split("\\b", -1);
		String ret = "";
		for (int i=0;i<words.length;i++) {
			if (! words[i].matches("^\\s+$") && (words[i].length() > 0)) {
				String temp = words[i].toLowerCase();
				ret += temp.substring(0,1).toUpperCase() + temp.substring(1);
			} else {
				ret += words[i];
			}
		}
		return ret;
	}
	
	public String countyDesignation() {
		String ret = "county";
		if(state.equals("LA") || (fipsState == 22))
			ret = "parish";
		else if(state.equals("AK") || (fipsState == 2))
			ret = "borough";
		else if(isNonState())
			ret = "";
		return ret;
	}
	
	public boolean isNonState() {
		String[] twoLetterNonStates = { "DC", "AS", "FM", "GU", "MH", "MP", "PW", "PR", "UM", "VI" } ;
		short[] fipsNonStates = { 11, 3, 7, 14, 43, 52, 60, 64, 66, 68, 69, 70, 72, 74, 78, 81, 84, 86, 67, 89, 71, 76, 95, 79};
		if (zipClass.equals("M"))
			return true;
		for (int i=0; i < twoLetterNonStates.length;i++)
			if (state.equals(twoLetterNonStates[i]))
				return true;
		for (int i=0; i < fipsNonStates.length;i++)
			if (fipsState == fipsNonStates[i])
				return true;
		return false;
	}

	public String prettyCoords(Double latitude, Double longitude) {
		String ret = "";
		char deg = '\u00B0';
		if((latitude < -90.0) || (latitude > 90.0) || (longitude < -180.0) || (longitude > 180.0))
			return ret;
		ret += String.format("%d" + deg, (int) Math.floor(Math.abs(latitude)));
		Double min = 60.0 * Math.abs(latitude - Math.floor(latitude));
		Double sec = 60.0 * (min - Math.floor(min));
		if (min > 1.0)
			ret += String.format("%d'", (int) Math.floor(min));
		if (sec > 1.0)
			ret += String.format("%d\"", (int) Math.floor(sec));
		if(latitude >= 0.0)
			ret += "N";
		else
			ret += "S";
		ret += ", ";
		ret += String.format("%d" + deg, (int) Math.floor(Math.abs(longitude)));
		min = 60.0 * Math.abs(longitude - Math.floor(longitude));
		sec = 60.0 * (min - Math.floor(min));
		if (min > 1.0)
			ret += String.format("%d'", (int) Math.floor(min));
		if (sec > 1.0)
			ret += String.format("%d\"", (int) Math.floor(sec));
		if(longitude >= 0.0)
			ret += "E";
		else
			ret += "W";
		return ret;
	}
	
	public double getAllocationFactor() {
		return allocationFactor;
	}

	public void setAllocationFactor(double allocationFactor) {
		this.allocationFactor = allocationFactor;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public short getFipsCounty() {
		return fipsCounty;
	}

	public void setFipsCounty(short fipsCounty) {
		this.fipsCounty = fipsCounty;
	}

	public short getFipsState() {
		return fipsState;
	}

	public void setFipsState(short fipsState) {
		this.fipsState = fipsState;
	}

	public int getHousingUnits() {
		return housingUnits;
	}

	public void setHousingUnits(int housingUnits) {
		this.housingUnits = housingUnits;
	}

	public long getLandArea() {
		return landArea;
	}

	public void setLandArea(long landArea) {
		this.landArea = landArea;
	}

	public double getLandAreaSqMiles() {
		return landAreaSqMiles;
	}

	public void setLandAreaSqMiles(double landAreaSqMiles) {
		this.landAreaSqMiles = landAreaSqMiles;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public long getWaterArea() {
		return waterArea;
	}

	public void setWaterArea(long waterArea) {
		this.waterArea = waterArea;
	}

	public double getWaterAreaSqMiles() {
		return waterAreaSqMiles;
	}

	public void setWaterAreaSqMiles(double waterAreaSqMiles) {
		this.waterAreaSqMiles = waterAreaSqMiles;
	}

	public String getZipClass() {
		return zipClass;
	}

	public void setZipClass(String zipClass) {
		this.zipClass = zipClass;
	}

	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}
}
