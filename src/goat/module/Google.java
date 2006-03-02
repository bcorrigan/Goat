package goat.module;

import java.lang.Math ;
import java.util.Random ;

import goat.core.Message ;
import goat.core.Module ;
import goat.util.GoatGoogle ;

import com.google.soap.search.* ;

/**
 * Module to ask google about stuff.
 * 
 * @author encontrado
 *         Created on 27-Feb-2006
 */
public class Google extends Module {

	private static Random random = new Random() ;

	/* IRC methods
	 */
	
	public String[] getCommands() {
		return new String[]{"google", "goatle", "googlefight", 
			"searchcount", "pornometer", "pronometer", "pr0nometer",
			"sexiness"};
	}

	public void processPrivateMessage(Message m) {
   	processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		//debug
		//System.out.println("PROCESSING:  " + m.modCommand) ;
		m.removeFormattingAndColors() ;
		try {
			if ("google".equalsIgnoreCase(m.modCommand) || 
				"goatle".equalsIgnoreCase(m.modCommand)) {
				ircGoogle(m) ;
			} else if ("searchcount".equalsIgnoreCase(m.modCommand)) {
				ircSearchCount(m) ;
			} else if ("googlefight".equalsIgnoreCase(m.modCommand)) {
				ircGoogleFight(m) ;
			} else if ("pornometer".equalsIgnoreCase(m.modCommand) ||
					"pronometer".equalsIgnoreCase(m.modCommand) ||
					"pr0nometer".equalsIgnoreCase(m.modCommand)) {
				ircPornometer(m) ;
			} else if ("sexiness".equalsIgnoreCase(m.modCommand)) {
				ircSexiness(m) ;
			} else {
				m.createReply(m.modCommand + " not yet implemented.").send() ;
			}
		} catch (GoogleSearchFault f) {
			m.createReply("There was a problem with google.").send() ;
			System.out.println(f.toString()) ;
		}
	}

	private void ircGoogle (Message m) 
		throws GoogleSearchFault {
		m.createReply(luckyString(m.modTrailing)).send() ;
	}
	
	private void ircSearchCount (Message m) {
		m.createReply(GoatGoogle.numSearchesToday() + " google search requests have been made in the past 24 hours.").send() ;
	}

	private void ircSexiness (Message m) 
		throws GoogleSearchFault {
		String query = quoteAndClean(m.modTrailing) ;
		int sexyPercentage = Math.round((float) 100 * sexiness(query)) ;
		if (sexyPercentage < 0) {
			m.createReply(query + " does not exist, and therefore can not be appraised for sexiness.").send() ;
		} else {
			m.createReply(query + " is " + sexyPercentage + "% sexy.").send() ;
		}
	}

	private void ircPornometer(Message m) 
		throws GoogleSearchFault {
		String query = quoteAndClean(m.modTrailing) ;
		if (query.matches("^[\\\"\\s]*$")) {
			m.createReply("The pornometer is a sophisticated instrument, but it won't do anything unless you give it something to measure.").send() ;
			return ;
		}
		float pornometerReading = pornometer(query) ;
		int pornPercent = Math.round((float) 100 * pornometerReading) ;
		if (pornPercent < 0) {
			m.createReply(query + " could not be measured with the pornometer, due to a lack of actually existing.").send() ;
		} else if ((float) 0 == pornometerReading) {
			// a little fun here
			String [] possibleReplies = {
				"Even Jesus would approve of " + query + ".",
				query + " is so fresh and so " + Message.BOLD + "clean!",
				query + " is safe for church.",
				query + " is 100% " + Message.BOLD + "BORING.",
				query + " is as clean as a whistle."
			} ;
			m.createReply(possibleReplies[random.nextInt(possibleReplies.length)]).send();
		} else if ((float) 1 == pornometerReading) {
			String [] possibleReplies = {
				"I totally want to fuck " + query,
				query + " is so totally going to Hell.",
				query + " is completely filthy.",
				query + " is 100% " + Message.BOLD + "HOTTT."
			} ;
			m.createReply(possibleReplies[random.nextInt(possibleReplies.length)]).send() ;
		} else {
			m.createReply(query + " is " + pornPercent + "% pornographic.").send() ;
		}
	}

	/**
	 * Half of this probably belongs in its own method.
	 */
	private void ircGoogleFight (Message m)
		throws GoogleSearchFault {
		String [] contestants = m.modTrailing.split("\\s+[vV][sS]\\.?\\s+") ;
		if (contestants.length < 2) {
			m.createReply("Usage:  \"googlefight \"dirty dogs\" vs. \"fat cats\" [vs. ...]\"").send() ;
			return ;
		}
		for (int i = 0 ; i < contestants.length ; i++) 
			contestants[i] = contestants[i].trim() ;
		int [] scores = getResultCounts(contestants) ;
		int [] winners = getWinners(scores) ;
		switch(winners.length) {
			case 0 : // no winner
				m.createReply("There was no winner, only losers.  Try fighting with things that actually exist.") ;
				break;
			case 1 : // normal
				m.createReply("The winner is " + Message.BOLD + contestants[winners[0]] + Message.BOLD + ", with a score of " + scores[winners[0]] + "!").send() ;
				break;
			default : // tie
				String winnerString = Message.BOLD + contestants[winners[0]] + Message.BOLD ;
				for (int i=1 ; i < winners.length ; i++)
					winnerString += " and " + Message.BOLD + contestants[winners[i]] + Message.BOLD ;
				m.createReply("We have a tie!  " + winnerString + " tied with a score of " + scores[winners[0]]).send() ;
		}
	}
	
	/* Actually-do-stuff methods
	 */
	
	/**
	 * Put a given string in quotes, and remove irc gunk.
	 * <p/>
	 * Also removes spaces between quotes, and reduces multiple quotes
	 * down to one (so it's safe to pass in a string that's already
	 * in quotes).
	 * <p/>
	 *	This probably belongs in a utility library somewheres, along with
	 *	all of the convenience methods in goat.core.Message
	 */
	public String quoteAndClean(String s) {
		s = Message.removeFormattingAndColors(s) ;
		s = "\"" + s + "\"" ;
		s = s.replaceAll("\\s*\"\\s*", "\"") ; //remove space around quotes
		s = s.replaceAll("\"+", "\"") ; //strip away multiple quotes
		return s ;
	}
	
	/**
	 * Given an array of query strings, return an array of search-result counts.
	 */
	public int[] getResultCounts(String[] queries)
		throws GoogleSearchFault {
		int [] counts = new int[queries.length] ;
		for (int i = 0 ; i < queries.length; i++) {
			if (queries[i].matches("\\s*")) { // if string is empty
				counts[i] = -1 ;
			} else {
				counts[i] = simpleSearch(queries[i]).getEstimatedTotalResultsCount();
			}
		}
		return counts ;
	}

	/**
	 * Given an array of int, return an array of int containing the index of the largest element (or elements, in case of a tie).
	 *
	 */
	public int [] getWinners(int [] scores) {
		int[] indices = new int[scores.length] ;
		if (indices.length == 0) 
			return indices ;
		indices[0] = 0 ;
		int lastIndex = 0 ;
		for (int i = 1 ; i < scores.length ; i++ ) {
			if (scores[i] > scores[indices[0]]) { // new high
				indices[0] = i ;
				lastIndex = 0 ;
			} else if (scores[i] == scores[indices[0]]) { // tie
				indices[++lastIndex] = i ;
			}
		}
		int [] ret = new int[lastIndex + 1] ;
		System.arraycopy(indices, 0, ret, 0, lastIndex + 1) ;
		return ret ;
	}
				
	
	/**
	 * Return the estimated sexiness of the given string.
	 * <p>
	 * You should probably clean up your query and quote it
	 * before you pass it to this method. Like with quoteAndClean(), say.
	 * 
	 * @param 	query	your search string.
	 * @return			a float between 0 and 1, usually, but sometimes 
	 * 					more than 1.  -1 if google returns no results for 
	 * 					your query.
	 * @see		quoteAndClean()
	 */
	public float sexiness (String query) 
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
	 * @param 	query	your search string.
	 * @return			a float between 0 and 1.   0 is totally clean, 1 is completely filthy.  -1 if google returns no results for the query.
	 * 					your query.
	 * @see		quoteAndClean()
	 */
	public float pornometer(String query)
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

			
	/* General google api convenience methods.
	 *
	 * Most of what follows should probably go in goat.util.GoatGoogle
	 *
	 * Some methods should ideally be moved into subclasses of the various
	 * GoogleSearch* classes.
	 */
	
	public GoogleSearchResult simpleSearch (String query, boolean safe)
		throws GoogleSearchFault {
		GoatGoogle gg = new GoatGoogle() ;
		gg.setSafeSearch(safe) ;
		gg.setQueryString(query) ;
		return gg.doSearch() ;
	}

	public GoogleSearchResult simpleSearch (String query)
		throws GoogleSearchFault {
		return simpleSearch(query, false) ;
	}

	/**
	 * takes a GoogleSearchResultElement and gives you a simple, irc-friendly string representation of it.
	 */
	public String simpleResultString (GoogleSearchResultElement re) {
		return boldConvert(re.getTitle()) + "  " + re.getURL() ;
	}
		
	/**
	 * This is pretty foolish.
	 * <p/>
	 * It's a relic of my misunderstanding of getStartIndex() and
	 * getEndIndex() in the googleAPI
	 */
	public GoogleSearchResultElement getElement (GoogleSearchResult r, int i) {
		GoogleSearchResultElement [] results = r.getResultElements() ;
		return results[i] ;
	}

	/**
	 * This, too, is foolish.
	 * <p/>
	 * And also a relic of a misunderstanding of the googleAPI.
	 */
	public GoogleSearchResultElement firstElement (GoogleSearchResult r) {
		return getElement(r, 0) ;
	}

	public GoogleSearchResultElement feelingLucky (String query, boolean safe)
		throws GoogleSearchFault {
		return firstElement(simpleSearch(query, safe)) ;
	}

	public GoogleSearchResultElement feelingLucky (String query)
		throws GoogleSearchFault {
		return feelingLucky(query, false) ;
	}

	public String luckyString (String query, boolean safe)
		throws GoogleSearchFault {
		return simpleResultString(feelingLucky(query, safe)) ;
	}
	
	public String luckyString (String query)
		throws GoogleSearchFault {
		return luckyString(query, false) ;
	}
	
	/**
	 * Convert html &lt;b&gt; tags in a string to irc BOLD formatting characters
	 */
	public String boldConvert (String s) {
		return s.replaceAll("<[/ ]*[bB] *>", Message.BOLD) ;
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
	public GoogleSearchResultElement [] getResultsIntersection
		(GoogleSearchResultElement [] a, 
		 GoogleSearchResultElement [] b) {
		GoogleSearchResultElement [] intersection = new GoogleSearchResultElement[a.length] ;
		if (intersection.length == 0) 
			return intersection ;
		int length = 0 ;
		for(int i = 0; i < a.length ; i++)
			for(int j = 0; j < b.length ; j++)
				if (a[i].getURL().equals(b[j].getURL())) {
					intersection[length++] = a[i] ;
					break ;
				}
		GoogleSearchResultElement [] ret = new GoogleSearchResultElement[length] ;
		if (0 == length)
			return ret ;
		System.arraycopy(intersection, 0, ret, 0, length) ;
		return ret ;
	}

	public static void main(String[] args) {
		Google g = new Google() ;
		try {
			System.out.println(g.luckyString("goat fucker")) ;
			System.out.println("\"goat\" is " + (int) (100.0 * g.sexiness("goat")) + "% sexy") ;
			System.out.println("\"asdfsaidfyuwcv129038\" is " + (int) (100.0 * g.sexiness("asdfsaidfyuwcv129038")) + "% sexy") ;
			System.out.println(GoatGoogle.numSearchesToday() + " searches in the past 24 hours.") ;
		} catch (GoogleSearchFault f) {
			System.out.println(f.toString()) ;
		}
	}
}
