package goat.util ;

import java.io.* ;
import com.google.soap.search.* ;
import java.util.Date ;
import java.util.ArrayList ;
import java.beans.XMLDecoder ;
import java.beans.XMLEncoder ;
import java.util.NoSuchElementException ;

/*  (non-Javadoc)
 *  goat has a gmail address:
 *      uname :  goat.jism
 *      pass  :  plumduff
 *
 *  goat's gmail address is the where any API client-licence 
 *  stuff will be sent.
 */

/**
 *   A wrapper around teh googal API, for convenience and usage-tracking.
 *   <p/>
 *   This is a subclass of GoogleSearch which remembers goat's 
 *   client Key for you, keeps track of the number of 
 *   searches that have been done in the past 24 hours, and 
 *   catches GoogleSearchFaults so you don't have to.
 *	  <p/>
 *   <i>Note:  there isn't any documentation as to whether the
 *   1000-search limit is per-24-hour-period, or per-day with
 *   the counter reset once a day.  Anecdotal evidence suggests
 *   the former, so that's what we use here.</i>
 *   <p/>
 *   @author rs
 */
public class GoatGoogle extends GoogleSearch {
	
	private static final String clientKey = "n2T+w21QFHIyv1M+HVnVulnSucdmiEWv" ;
	private static final String timestampsFilename = "resources/googleSearchTimestamps.xml" ;
	
	private static ArrayList timestamps = new ArrayList() ;
	private static boolean timestampsLoaded = false ;
	
	/**
	 * Ye Olde constructor.
	 *
	 * All we add to the inherited constructor is some internal search-count
	 * book-keeping.
	 */
	public GoatGoogle() {
		setKey(clientKey) ;
		if (! timestampsLoaded) {
			loadTimestamps() ;
			pruneTimestamps() ;
		}
	}	

	/* (non-Javadoc)
	 * 
	 * Override the inherited query methods, so as to keep track of how 
	 * many searches have been done in the past 24 hrs, and catch 
	 * GoogleSearchFaults.
	 */

	/**
	 * Adds book-keeping to overridden method.
	 */
	public GoogleSearchResult doSearch() 
		throws GoogleSearchFault {

		GoogleSearchResult r = super.doSearch() ;
		timestamps.add(new Date()) ;
		pruneTimestamps() ;
		/* We don't really have to prune the timestamp array after every
		 * search, but it's a convenient way to make sure the array doesn't
		 * get too long */
		return r ;
	}

	/**
	 * Adds book-keeping to overridden method.
	 * <p/>
	 * Use this only if you specifically need google spelling 
	 * suggestions.  Use the spelling-suggester in the Dict module otherwise
	 * <p/>
	 * @see goat.util.DICTClient#getMatches()
	 */
	public String doSpellingSuggestion(String phrase) 
		throws GoogleSearchFault {

		String s = super.doSpellingSuggestion(phrase) ;
		timestamps.add(new Date()) ;
		pruneTimestamps() ; // see doSearch()
		return s;
	}
	
	/**
	 * Adds book-keeping to overridden method.
	 * <p>
	 * Note: You don't need this.  What is an IRC bot going to do with a 
	 * cached web page, eh?
	 */ 
	public byte[] doGetCachedPage(String url)
		throws GoogleSearchFault {
		
		byte [] ba = super.doGetCachedPage(url) ;
		timestamps.add(new Date()) ;
		pruneTimestamps() ; // see doSearch()
		return ba ;
	}


	/* (non-Javadoc)
	 * Extra Added goaty methods
	 */
	
	/* (non-Javadoc)
	 * Public goaty methods
	 */
	
	/**
	 * Gets number of searches done in the past 24 hours.
	 */
	public static int numSearchesToday() {
		pruneTimestamps() ;
		return timestamps.size() ;
	}
	
	/* (non-Javadoc)
	 * Private goaty methods 
	 */

	/* (non-Javadoc)
	 * Methods to deal with the timestamp array
	 */
	
	private static void pruneTimestamps() {
		if (! timestampsLoaded)
			loadTimestamps() ;
		Date now = new Date() ;
		long yesterday = now.getTime() - 24*60*60*1000 ;
		while ((! timestamps.isEmpty()) && 
				 (((Date) timestamps.get(0)).getTime() < yesterday)) {
			timestamps.remove(0) ;
				 }
		saveTimestamps() ;
	}

	private static void loadTimestamps() {
		XMLDecoder XMLdec = null;
	   try {
			XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream(timestampsFilename)));
			timestamps = (ArrayList) XMLdec.readObject();
			timestampsLoaded = true ;
		} catch (FileNotFoundException e) {
			timestamps = new ArrayList();
			timestampsLoaded = true ; // new file
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			timestamps = new ArrayList();
			timestampsLoaded = true ; // empty file?
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			// not clear on what this would mean... file too large?
			// should we set timestampsLoaded or not?
			timestampsLoaded = true ;
			e.printStackTrace();
		} finally {
			if(XMLdec!=null) XMLdec.close();
		}
	}
	
	private static void saveTimestamps() {
		if (timestampsLoaded) {
			XMLEncoder XMLenc = null;
			try {
				XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(timestampsFilename)));
				XMLenc.writeObject(timestamps);
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			} finally {
				if(XMLenc!=null) XMLenc.close();
			}
		} else {
			System.out.println("Oops!  Someone tried to write the Goat.util.Goatgoogle timestamps file (" +timestampsFilename + ") without loading it first!  File not written.") ;
		}
	}

	/**
	 * Main method. 
	 * <p/>
	 * For your debugging pleasure.
	 */
	public static void main(String[] args) {
		GoatGoogle gg = new GoatGoogle() ;
		gg.setQueryString("goat sex") ;
		try {
			GoogleSearchResult r = gg.doSearch();
			System.out.println("Google Search Results:");
			System.out.println(r.toString());
		} catch (GoogleSearchFault f) {
			System.out.println(f.toString()) ;
		}
		System.out.println("\nSearches so far today: " + numSearchesToday());
	}
}
