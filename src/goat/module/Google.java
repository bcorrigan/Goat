package goat.module;

import java.lang.Math ;

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

	/* IRC methods
	 */
	
	public String[] getCommands() {
		return new String[]{"google", "goatle", "googlefight", 
			"searchcount", "pornometer", "sexiness"};
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
//			} else if ("pornometer".equalsIgnoreCase(m.modCommand)) {
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
		String query = "\"" + m.modTrailing.trim() + "\"" ;
		int sexyPercentage = Math.round((float) 100 * sexiness(query)) ;
		if (sexyPercentage < 0) {
			m.createReply(query + " does not exist, and therefore can not be appraised for sexiness.").send() ;
		} else {
			m.createReply(query + " is " + sexyPercentage + "% sexy.").send() ;
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
				m.createReply("There was no winner, only losers.  Try fighting with something that actually exists.") ;
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
	 * This might hit an array-out-of-bounds exception if it's fed a no-element
	 * or uninitialized array.
	 *
	 * Why isn't vim syntax-highlighting javadoc here, and hereafter?
	 */
	public int [] getWinners(int [] scores) {
		int[] indices = new int[scores.length] ;
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
		for (int i = 0 ; i <= lastIndex ; i++)
			ret[i] = indices[i] ;
		return ret ;
	}
				
	
	/**
	 * Return the estimated sexiness of the given string.
	 *
	 * You probably want to enclose your query in quotes.  Just sayin'.
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
	
	/* General search convenience methods.
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

	public String simpleResultString (GoogleSearchResultElement re) {
		return boldConvert(re.getTitle()) + "  " + re.getURL() ;
	}
		
	public GoogleSearchResultElement getElement (GoogleSearchResult r, int i) {
		if (i < r.getStartIndex())
			i = r.getStartIndex() ;
		if (i > r.getEndIndex())
			i = r.getEndIndex() ;
		GoogleSearchResultElement [] results = r.getResultElements() ;
		return results[i] ;
	}

	public GoogleSearchResultElement firstElement (GoogleSearchResult r) {
		return getElement(r, r.getStartIndex()) ;
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
	
	public String boldConvert (String s) {
		return s.replaceAll("<[/ ]*[bB] *>", Message.BOLD) ;
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
