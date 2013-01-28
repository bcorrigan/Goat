package goat.util ;

import static goat.util.StringUtil.byteLength;
import static goat.util.StringUtil.maxEncodeable;
import static goat.core.Constants.*;
import goat.core.BotStats;
import java.nio.charset.CharacterCodingException;
import static java.lang.Math.min;

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

    private static int defaultMaxBytes = 460;
    public int maxBytes = defaultMaxBytes;

    public String innerPre = "\u2026";
    public String innerPost = " [more]";
    private int maxWalkback = 32;

    private String inputExceededMsg = " " + BOLD + "[" + NORMAL + "no more \u2014 buffer protection engaged" + BOLD + "]";

    // max input length is specified in characters, not bytes
    // it's OK if unicodes blow it up to 4x apparent length,
    // we just need to cap it somewhere to prevent qpt from
    // crushing our heap.
    // 17 pages of crap should be enough for anyone...
    private int maxInputCharacters = maxBytes * 17 - inputExceededMsg.length();

    private boolean untapped = true ;

    // various fiddly number conveniences
    private int firstMax(int numBytes) {
        return numBytes - byteLength(innerPost);
    }

    private int lastMax(int numBytes) {
        return numBytes - byteLength(innerPre) ;
    }

    private int midMax(int numBytes){
        return numBytes - byteLength(innerPre) - byteLength(innerPost) ;
    }

    /**
     * Ye Olde empty constructor
     */
    public Pager() {}

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
        if(maxInputCharacters > 0 && text.length() > maxInputCharacters)
            text = text.substring(0, maxInputCharacters) + inputExceededMsg;
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
    public synchronized String getNext(int numBytes) {
        String ret = "" ;
        try {
            if (untapped) {
                if (remaining() <= numBytes)
                    ret = getPoliteChunk(numBytes);
                else
                    ret = getPoliteChunk(firstMax(numBytes));
                untapped = false ;
            } else if(remaining() <= lastMax(numBytes)) {
                ret = innerPre + getPoliteChunk(lastMax(numBytes));
            } else {
                ret = innerPre + getPoliteChunk(midMax(numBytes));
            }
            if (! buffer.isEmpty())
                ret += innerPost ;
        } catch (CharacterCodingException cce) {
            // This will rarely happen, if ever, but just in case:
            ret = "I didn't feel like encoding that for you.";
            cce.printStackTrace();
            System.err.println("Pager buffer contents: \n" + buffer + "\n\n(buffer purged.)\n\n");
            buffer = "";
        }
        return ret ;
    }

    //    public String getNext() {
    //    return getNext(maxBytes);
    //}

    private static BotStats botStats = BotStats.getInstance();
    //":goat!goat@cloak-E4425697.edslocomb.com PRIVMSG #goat :"
    public String getNext(String command, String chan) {
        String header = ":" + botStats.getClientName() + "!" + botStats.getHostmask() + " " + command + " " + chan + " :\r\n";
        int byteCount = 512 - byteLength(header);
        byteCount -= 2; // for luck
        byteCount = min(byteCount, maxBytes);
        return getNext(byteCount);
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

    //pop the first numBytes bytes off the buffer, or until the first form-feed
    //character, whichever comes first, and without horking multibyte unicode
    private String getChunk(int numBytes) throws CharacterCodingException {
        String ret = buffer ;
        if ( remaining() > numBytes )
            ret = maxEncodeable(buffer, numBytes);
        ret = ret.trim(); // prevents form feed from being last character
        if(ret.contains("\f")) {
            int formFeed = ret.indexOf('\f');
            ret = buffer.substring(0, formFeed);
            buffer = buffer.substring(formFeed + 1);
        } else {
            buffer = buffer.substring(ret.length());
        }
        buffer = buffer.trim();
        return ret;
    }

    //pop the first num bytes off the buffer, after first walking num back
    //to the nearest whitespace (but not walking back further than maxWalkback
    private String getPoliteChunk(int numBytes) throws CharacterCodingException {
        int politePosition=0;
        if ( remaining() > numBytes ) {
            String rudeChunk = maxEncodeable(buffer, numBytes);
            int i = rudeChunk.length() - 1;
            while(i >= 0 && i >= rudeChunk.length() - maxWalkback) {
                if(buffer.substring(i, i+1).matches("\\s")) {
                    politePosition = i;
                    break;
                }
                i--;
            }
        }
        if(politePosition > 0)
            numBytes=byteLength(buffer.substring(0,politePosition));
        return getChunk(numBytes);
    }

    public static String smush(String text) {
        // convert all whitespace (except form feeds) to spaces
        text = text.replaceAll("[\\t\\n\\x0B\\r]", " ") ;
        // condense all multi-space down to two spaces
        text = text.replaceAll(" {3,}", "  ") ;
        return text ;
    }
}
