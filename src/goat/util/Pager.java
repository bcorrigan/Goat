package goat.util ;

import static goat.util.StringUtil.byteLength;
import static goat.util.StringUtil.truncateWhenUTF8;
import static goat.core.Constants.*;

/**
 * A wee IRC pager
 * 
 * @author encontrado
 *         <p/>
 *         A general utility class for taking chunks of text, mashing
 *         them all down to one line, stripping out extraneous whitepace,
 *         and feeding them out in IRC-message-sized chunks.
 */
public class Pager {

	private String buffer = "";
	
	// Config-like stuff
	//this is really bytes, not chars.
	//slashdot varies max bytes of message based on content! plain ascii - 460 bytes. 
	public static final int maxBytes = 456;
	private String innerPre = "\u2026" ;
	private String innerPost = "\u2026 [more]" ;
	private int maxWalkback = 32;
	
	private String inputExceededMsg = " " + BOLD + "[" + NORMAL + "no more \u2014 buffer protection engaged" + BOLD + "]";
	private int maxInputLength = maxBytes * 17 - byteLength(inputExceededMsg);  // 17 pages of crap should be enough for anyone, but not enough to swamp goat's heap 
	
	private boolean untapped = true ; 
	
	// various easier-making fiddly numbers
	private int firstMax = maxBytes - byteLength(innerPost);
	
	private int lastMax = maxBytes - byteLength(innerPre) ;
	
	
	private int midMax = maxBytes - byteLength(innerPre) - byteLength(innerPost) ;
	

	/**
	 * Ye Olde empty constructor
	 */
	public Pager() {
	}

	/**
	 * @param text to put in the buffer
	 */
	public Pager(String text) {
		init(text) ;
	}
	/**
	 * @param text to put in the buffer, blowing away anything already there
	 */
	private void init(String text) {
		if(maxInputLength > 0 && byteLength(text) > maxInputLength)
			text = truncateWhenUTF8(text,maxInputLength) + inputExceededMsg;
		buffer = smush(text);
		untapped = true ;
	}
	/**
	 * @param text to add to the end of the buffer
	 */
	public synchronized void add(String text) {
		if(buffer.equals(""))
			buffer = smush(text);
		else
			buffer += " " + smush(text) ;
	}
	/**
	 * @return the next block of text, which is removed from the buffer, and prettied up with prefix and suffix, as appropriate.
	 */
	public synchronized String getNext() {
		String ret = "" ;
		if (untapped) {
			if (remaining() <= maxBytes) {
				ret = getPoliteChunk(maxBytes) ;
			} else {
				ret = getPoliteChunk(firstMax) + innerPost ;
			}
			untapped = false ;
		} else {
			if (remaining() <= lastMax) {
				ret = innerPre + getPoliteChunk(lastMax) ;
			} else {
				ret = innerPre + getPoliteChunk(midMax) + innerPost ;
			}
		}
		return ret ;
	}
	/**
	 * @return true if there's nothing in the buffer
	 */
	public synchronized boolean isEmpty() {
		if (buffer.equals("")) {
			return true ;
		} else {
			return false ;
		}
	}
	/**
	 * @return length of buffer remaining
	 */
	public synchronized int remaining() {
		return byteLength(buffer);
	}
	
	/* private things */

	//pop the first num chars off the buffer
	private String getChunk(int numBytes) {
		String ret = "" ;
		if ( remaining() >= numBytes ) {
			ret = truncateWhenUTF8(buffer, numBytes);
			int chop = ret.length();
			buffer = buffer.substring(chop) ;
		} else {
			ret = buffer ;
			buffer = "" ;
		}
		return ret ;
	}

	//pop the first num bytes off the buffer, after first walking num back
	//to the nearest whitespace (but not walking back further than maxWalkback
	private String getPoliteChunk(int numBytes) {
		int numCharsBack=0;
		if ( remaining() > numBytes )
			for (int i = truncateWhenUTF8(buffer, numBytes).length(); i>=numBytes-maxWalkback; i--) 
				if(buffer.substring(i, i+1).matches("\\s")) {
					numCharsBack = i;
					break;
				}
		
		if(numCharsBack>0)
			numBytes=byteLength(buffer.substring(0,numCharsBack));
		
		return getChunk(numBytes);
	}

	public static String smush(String text) {
		// convert all whitespace to spaces
		text = text.replaceAll("\\s", " ") ;
		// condense all multi-whitespace down to two spaces
		text = text.replaceAll("\\s{3,}", "  ") ;
		return text ;
	}
	
	/**
	 * Main method here for debugging
	 */
	public static void main(String[] args) {
		String testString = "As an enlightened, modern parent, I try to be as involved as possible in the lives of my six children. I encourage them to join team sports. I attend their teen parties with them to ensure no drinking or alcohol is on the premises. I keep a fatherly eye on the CDs they listen to and the shows they watch, the company they keep and the books they read. You could say I'm a model parent. My children have never failed to make me proud, and I can say without the slightest embellishment that I have the finest family in the USA.\n\n  Two years ago, my wife Carol and I decided that our children\'s education would not be complete without some grounding in modern computers. To this end, we bought our children a brand new Compaq to learn with. The kids had a lot of fun using the handful of application programs we'd bought, such as Adobe's Photoshop and Microsoft's Word, and my wife and I were pleased that our gift was received so well. Our son Peter was most entranced by the device, and became quite a pro at surfing the net. When Peter began to spend whole days on the machine, I became concerned, but Carol advised me to calm down, and that it was only a passing phase. I was content to bow to her experience as a mother, until our youngest daughter, Cindy, charged into the living room one night to blurt out: \"Peter is a computer hacker!\"" ;
		System.out.println("Testing!\n\n");
		Pager pager = new Pager(testString) ;
		while (! pager.isEmpty()) {
			System.out.println(pager.getNext() + "\n") ;
		}
		System.out.println("\n\nA more synthetic test...\n\n");
		testString = "B" ;
		for (int i=2 ; i<Pager.maxBytes ; i++) {
			testString = testString + String.valueOf(i % 10) ;
		}
		testString = testString + "E" ;
		pager.init(testString) ;
		while (! pager.isEmpty()) {
			System.out.println(pager.getNext() + "\n") ;
		}
		System.out.println("\n\n...same, doubled...\n\n");
		testString = testString + testString ;
		pager.init(testString) ;
		while (! pager.isEmpty()) {
			System.out.println(pager.getNext() + "\n") ;
		}
		//exceeds max allowed by one char (3 bytes)
		testString="Top box office:, "+BOLD+"1"+BOLD+":The Hobbit: An Unexpected Journey(★★★)"+BOLD+"2"+BOLD+":Rise of the Guardians(★★★✫)"+BOLD+"3"+BOLD+":Lincoln(★★★★✫)"+BOLD+"4"+BOLD+":Life of Pi(★★★★☆)"+BOLD+"5"+BOLD+":Skyfall(★★★★✫)"+BOLD+"6"+BOLD+":The Twilight Saga: Breaking Dawn Part 2(★★☆)"+BOLD+"7"+BOLD+":Wreck-it Ralph(★★★★☆)"+BOLD+"8"+BOLD+":Red Dawn(✫)"+BOLD+"9"+BOLD+":Playing for Keeps"+BOLD+"10"+BOLD+":Flight(★★★✰)"+BOLD+"11"+BOLD+":Hitchcock(★★★☆)"+BOLD+"12"+BOLD+":Argo(★★★★✫)"+BOLD+"13"+BOLD+":Silver Linings Playbook(★★★★✫)"+BOLD+"14"+BOLD+":Hotel Transylvania(★★)"+BOLD+"15"+BOLD+":Anna Karenina(★★★)"+BOLD+"16"+BOLD+":Here Comes the Boom(★✰)";
		//testString="★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★"; //max unicode case.. still 460 chars
		//testString="123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_";
		//testString=" I do so well in school, but I feel so horrible about it. Like in a lot of the \"hard\" classes, where a lot of my friends would fail the test, I would ace it. I feel so bad because I ruin the curve for them. Sometimes I just want to completely bomb a test just so they would get a better grade. But I can't get myself to do that for some odd reason. Everytime that I want to bomb a test, I always convince myself otherwise when it comes to it. I don't know what's so hard about these classes";
		System.out.println(testString.length() + ", bytes:" + byteLength(testString) );
		pager.init(testString) ;
		while (! pager.isEmpty()) {
			String next = pager.getNext();
			System.out.println("str length:" + next.length());
			System.out.println("byte length:" + byteLength(next));
			System.out.println(next + "\n") ;
		}
	}
}
