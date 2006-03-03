package goat.util ;

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
	public static final int maxMessageLength = 420 ;
	private String innerPre = ".." ;
	private String innerPost = "..[more]" ;

	//
	private boolean untapped = true ; 
	
	// various easier-making fiddly numbers
	private int firstMax = maxMessageLength - innerPost.length() ;
	private int lastMax = maxMessageLength - innerPre.length() ;
	private int midMax = maxMessageLength - innerPre.length() - innerPost.length() ;

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
	public void init(String text) {
		buffer = smush(text) ;	
		untapped = true ;
	}
	/**
	 * @param text to add to the end of the buffer
	 */
	public void add(String text) {
		buffer = buffer + " " + smush(text) ;
	}
	/**
	 * @return the next block of text, which is removed from the buffer, and prettied up with prefix and suffix, as appropriate.
	 */
	public String getNext() {
		String ret = "" ;
		if (untapped) {
			if (remaining() <= maxMessageLength) {
				ret = getChunk(maxMessageLength) ;
			} else {
				ret = getChunk(firstMax) + innerPost ;
			}
			untapped = false ;
		} else {
			if (remaining() <= lastMax) {
				ret = innerPre + getChunk(lastMax) ;
			} else {
				ret = innerPre + getChunk(midMax) + innerPost ;
			}
		}
		return ret ;
	}
	/**
	 * @return true if there's nothing in the buffer
	 */
	public boolean isEmpty() {
		if (buffer.equals("")) {
			return true ;
		} else {
			return false ;
		}
	}
	/**
	 * @return length of buffer remaining
	 */
	public int remaining() {
		return buffer.length() ;
	}
	
	/* private things */

	//pop the first num chars off the buffer
	private String getChunk(int num) {
		String ret = "" ;
		if ( remaining() >= num ) {
			ret = buffer.substring(0, num) ;
			buffer = buffer.substring(num) ;
		} else {
			ret = buffer.toString() ;
			buffer = "" ;
		}
		return ret ;
	}

	public static String smush(String text) {
		text = text.replaceAll("\\s+", " ") ;
		text = text.trim() ;
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
		for (int i=2 ; i<Pager.maxMessageLength ; i++) {
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
	}
}
