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

   public static GoogleSearchResult simpleSearch (String query, boolean safe)
      throws GoogleSearchFault {
      GoatGoogle gg = new GoatGoogle() ;
      gg.setSafeSearch(safe) ;
      gg.setQueryString(query) ;
      return gg.doSearch() ;
   }

   public static GoogleSearchResult simpleSearch (String query)
      throws GoogleSearchFault {
      return GoatGoogle.simpleSearch(query, false) ;
   }

   /**
    * Return the first google search result.
    *
    * @param   query your search string
    * @param   safe  true if you want Google SafeSearch on
    * @return  the first GoogleSearchResultElement, or null if no results were found.
    */
   public static  GoogleSearchResultElement feelingLucky (String query, boolean safe)
      throws GoogleSearchFault {
      GoogleSearchResult sr = simpleSearch(query, safe) ;
      if (sr.getResultElements().length < 1)
         return null ;
      return sr.getResultElements()[0] ;
   }

   /**
    * Return the first google search result with SafeSearch off.
    *
    * @param   query yer search string
    * @return  first un-Safe(tm) search result, or null if no results.
    */
   public static GoogleSearchResultElement feelingLucky (String query)
      throws GoogleSearchFault {
      return GoatGoogle.feelingLucky(query, false) ;
   }
	
   /**
    * Return the estimated sexiness of the given string.
    * <p>
    * You should probably clean up your query and quote it
    * before you pass it to this method. Like with quoteAndClean(), say.
    *
    * @param   query your search string.
    * @return        a float between 0 and 1, usually, but sometimes
    *                more than 1.  -1 if google returns no results for
    *                your query.
    * @see     quoteAndClean()
    */
   public static float sexiness (String query)
      throws GoogleSearchFault {
      GoogleSearchResult plainResult = simpleSearch(query) ;
      if (plainResult.getEstimatedTotalResultsCount() < 1)
         return -1 ;
      GoogleSearchResult sexResult = simpleSearch(query + " sex");
      //debug
      //System.out.println(sexResult.getEstimatedTotalResultsCount()) ;
      //System.out.println(plainResult.getEstimatedTotalResultsCount()) ;
      return (float) sexResult.getEstimatedTotalResultsCount() /
         (float) plainResult.getEstimatedTotalResultsCount() ;
   }

   public static float gayness (String query)
      throws GoogleSearchFault {
      GoogleSearchResult plainResult = simpleSearch(query) ;
      if (plainResult.getEstimatedTotalResultsCount() < 1)
         return -1 ;
      GoogleSearchResult gayResult = simpleSearch(query + " gay");
      return (float) gayResult.getEstimatedTotalResultsCount() /
         (float) plainResult.getEstimatedTotalResultsCount() ;
   }

   /**
    * Return the estimated pornographicalness of the given string.
    * <p/>
    * This works by doing two google searches for your query, one
    * search with google's SafeSearch(tm) turned on, an one with
    * it turned off.  The results are then compared, and turned
    * into a number representing the fraction of the un-"Safe(tm)"
    * results which are not in the "Safe(tm)" results; ie, the
    * fraction which google considers offensive.
    * <p/>
    * You should probably clean up your query and quote it
    * before you pass it to this method.  Like with quoteAndClean(), say.
    *
    * @param   query your search string.
    * @return        a float between 0 and 1.   0 is totally clean, 1 is completely filthy.  -1 if google returns no results for the query.
    *                your query.
    * @see     quoteAndClean()
    */
   public static float pornometer(String query)
      throws GoogleSearchFault {
      GoogleSearchResult pornoResult = simpleSearch(query, false) ;
      if (pornoResult.getEstimatedTotalResultsCount() < 1)
         return (float) -1 ; // no google results
      GoogleSearchResult cleanResult = simpleSearch(query, true) ;
      GoogleSearchResultElement [] pornoResultElements = pornoResult.getResultElements() ;
      GoogleSearchResultElement [] cleanResultElements = cleanResult.getResultElements() ;
      float totalResults = (float) pornoResultElements.length ;
      int numIntersect = getResultsIntersection(pornoResultElements, cleanResultElements).length ;
      return (totalResults - (float) numIntersect) / totalResults ;
   }


   /**
    * Get the intersection of two GoogleSearchResultElement arrays.
    * <p/>
    * Results will be incorrect if both a and b contain multiple
    * GoogleResultElements, all with the same URL.
    *
    * @param a
    * @param b
    */
   public static GoogleSearchResultElement [] getResultsIntersection
      (GoogleSearchResultElement [] a,
       GoogleSearchResultElement [] b) {
      GoogleSearchResultElement [] intersection = new GoogleSearchResultElement[a.length] ;
      if (intersection.length == 0)
         return intersection ;
      int length = 0 ;
       for (GoogleSearchResultElement anA : a)
           for (int j = 0; j < b.length; j++)
               if (anA.getURL().equals(b[j].getURL())) {
                   intersection[length++] = anA;
                   break;
               }
       GoogleSearchResultElement [] ret = new GoogleSearchResultElement[length] ;
      if (0 == length)
         return ret ;
      System.arraycopy(intersection, 0, ret, 0, length) ;
      return ret ;
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
	 * For your debugging pleasure.  Note that you should use the junit
	 * class goat.util.GoatGoogleTest if you know what results to expect.
	 *
	 * @see	goat.util.GoatGoogleTest
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

