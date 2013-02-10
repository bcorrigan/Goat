package goat.util.zipcode;


import junit.framework.*;

/**
 * This unit test rebuilds the berkely dbs every time it is run.
 * 
 * Which shouldn't be a big deal.  Comment out the calls to the importData() methods if it gets tedious.
 * 
 * @author RobotSlave
 *
 */
public class ZipCodeTest extends TestCase {
	
	public static Test suite() {
		return new TestSuite(ZipCodeTest.class);
	}

	@Override
	protected void setUp() {
		ZipCode.init();
	}

	public void testZip1990() {
		Zip1990 zc90 = new Zip1990();
		if(! zc90.hasLocalFile()) {
			zc90.fetchDataFile() ;
		}
		Assert.assertTrue(zc90.hasLocalFile());
		ZipCode zippy = null;
		zippy = zc90.getByCode("60201");
		Assert.assertTrue(zippy != null);
		System.out.println("1990: " + zippy);
		System.out.println("1990 verbose:  " + zippy.toVerboseString());
	}
	
	public void testZip1999() {
		Zip1999 zc9 = new Zip1999();
		if(! zc9.hasLocalFile()) {
			zc9.fetchDataFile() ;
		}
		Assert.assertTrue(zc9.hasLocalFile());
		ZipCode zippy = null;
		zippy = zc9.getByCode("60201");
		Assert.assertTrue(zippy != null);
		System.out.println("1999: " + zippy);
		System.out.println("1999 verbose:  " + zippy.toVerboseString());
	}
	
	public void testZatc2000() {
		Zcta2000 zcta = new Zcta2000();
		if(! zcta.hasLocalFile()) {
			zcta.fetchDataFile() ;
		}
		Assert.assertTrue(zcta.hasLocalFile());
		ZipCode zippy = null;
		zippy = zcta.getByCode("60201");
		Assert.assertTrue(zippy != null);
		System.out.println("2000: " + zippy);
		System.out.println("2000 verbose:  " + zippy.toVerboseString());	
	}
	
	public void testZipCfd() {
		CfdZip zip = new CfdZip();
		if(! zip.hasLocalFile()) {
			zip.fetchDataFile() ;
		}
		Assert.assertTrue(zip.hasLocalFile());
		ZipCode zippy = null;
		zippy = zip.getByCode("60201");
		Assert.assertTrue(zippy != null);
		System.out.println("CFD: " + zippy);
		System.out.println("CFD verbose:  " + zippy.toVerboseString());	
	}
	
	public void testCombined() {
		ZipCode zippy = Combined.get("60201");
		Assert.assertTrue(zippy != null);
		System.out.println("Combined: " + zippy);
		System.out.println("Combined verbose:  " + zippy.toVerboseString());
	}
	
	public void testUnusual() {
		System.out.println("\n...trying a few unusual zip codes...");
		// 09521 USS Nimitz test
		ZipCode zippy = Combined.get("09521");
		Assert.assertTrue(zippy != null);
		System.out.println("USS Nimitz: " + zippy);
		System.out.println("USS Nimitz verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("M"));
		// 08544 Princeton University test
		zippy = Combined.get("08544");
		Assert.assertTrue(zippy != null);
		System.out.println("Disneyland: " + zippy);
		System.out.println("Disneyland verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("U"));
		// 90743 - Surfside, CA test
		zippy = Combined.get("90743");
		Assert.assertTrue(zippy != null);
		System.out.println("Surfside: " + zippy);
		System.out.println("Surfside verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("P"));
		// 48222 - Great Lakes marine station
		zippy = Combined.get("48222");
		Assert.assertTrue(zippy != null);
		System.out.println("Marine: " + zippy);
		System.out.println("Marine verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("U"));
		// 10048 - World Trade Center
		zippy = Combined.get("10048");
		Assert.assertTrue(zippy != null);
		System.out.println("WTC: " + zippy);
		System.out.println("WTC verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("S"));
		// New Orleans 
		zippy = Combined.get("70149");
		Assert.assertTrue(zippy != null);
		System.out.println("Nawlins: " + zippy);
		System.out.println("Nawlins verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("S"));
		Assert.assertTrue(zippy.countyDesignation().equals("parish")) ;
		// Alaska
		zippy = Combined.get("99520");
		Assert.assertTrue(zippy != null);
		System.out.println("Alaska: " + zippy);
		System.out.println("Alaska verbose:  " + zippy.toVerboseString());
		Assert.assertTrue(zippy.zipClass.equals("P"));
		Assert.assertTrue(zippy.countyDesignation().equals("borough"));
	}

	@Override
	protected void tearDown() {
		ZipCode.closeCache();
	}
	
}
