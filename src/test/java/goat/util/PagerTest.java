package goat.util;

import org.junit.*;

import static org.junit.Assert.*;

import static goat.core.Constants.*;
import static goat.util.StringUtil.*;

public class PagerTest {


    @Test
    public void testSimpleSynthetic() {
        Pager pager = new Pager("B" + pageMeat + "E");
        String page = pager.getNext(460);
        assertTrue("page begins with B", page.startsWith("B"));
        assertTrue("page ends with E", page.endsWith("E"));
        assertTrue("pager is empty", pager.isEmpty());
        assertEquals(page.length(), pager.maxBytes);

        pager = new Pager ("B" + pageMeat + "E!");
        page = pager.getNext(460);
        assertEquals(page.length(), pager.maxBytes);
        assertFalse(pager.isEmpty());
        assertTrue(page.endsWith(pager.innerPost));
        page = pager.getNext(460);
        assertTrue(pager.isEmpty());
        assertTrue(page.endsWith("!"));
    }

    @Test
    public void testEmojiSynthetic() {
        assertEquals(byteLength(PILE_OF_POO),4);
        String testPage = pageMeat + PILE_OF_POO;
        Pager pager = new Pager(testPage);
        String page = pager.getNext(460);
        assertEquals(page.length(), pager.maxBytes);
        assertEquals(byteLength(page), pager.maxBytes);
        assertFalse(pager.isEmpty());
        page = pager.getNext(460);
        assertTrue(page.endsWith(PILE_OF_POO));
        assertTrue(pager.isEmpty());

        pager = new Pager(PILE_OF_POO + pageMeat);
        page = pager.getNext(460);
        assertFalse(page.length() == byteLength(page));
        assertEquals(byteLength(page), pager.maxBytes);
        assertTrue(page.startsWith(PILE_OF_POO));
        assertFalse(pager.isEmpty());
        page = pager.getNext(460);
        assertTrue(pager.isEmpty());
    }

    @Test
    public void testStars() {
        String testString = starMeat();
        Pager pager = new Pager(testString);
        String page = pager.getNext(460);
        assertTrue(pager.isEmpty());
        assertEquals(page.length(), pager.maxBytes / byteLength(star));

        testString = starMeat() + PILE_OF_POO;
        pager = new Pager(testString);
        page = pager.getNext(460);
        assertFalse(pager.isEmpty());
        assertTrue(page.endsWith(pager.innerPost));
        int pil = pager.innerPost.length();
        assertEquals(page.length(), pil + (pager.maxBytes - pil) / byteLength(star));
        page = pager.getNext(460);
        assertTrue(pager.isEmpty());
        assertTrue(page.startsWith(pager.innerPre));
        assertEquals(page.substring(1,2), star);
        assertTrue(page.endsWith(PILE_OF_POO));
    }

    @Test
    public void testFormFeed() {
        Pager pager = new Pager(testString) ;
        String page = pager.getNext(460);

        assertTrue("first page starts correctly", page.startsWith("As an enlightened"));
        assertTrue("first page ends at form feed", page.endsWith("my six children." + pager.innerPost));
        page = pager.getNext(460);
        assertTrue("second page starts correctly", page.startsWith(pager.innerPre + "I encourage them"));
    }


    // the meat goes in the sandwich
    private Pager meatPager = new Pager();
    private String pageMeat = pageMeat();
    private String pageMeat() {
        StringBuilder sb = new StringBuilder();
        for (int i=2 ; i < meatPager.maxBytes ; i++)
            sb.append(String.valueOf(i % 10));
        return sb.toString();
    }

    private String star = "★";
    private String starMeat() {
        StringBuilder sb = new StringBuilder();
        for(int i=1; i <= meatPager.maxBytes / byteLength(star); i++)
            sb.append(star);
        return sb.toString();
    }


    private String testString = "As an enlightened, modern parent, I try to be as involved as possible in the lives of my six children.\f  I encourage them to join team sports. I attend their teen parties with them to ensure no drinking or alcohol is on the premises. I keep a fatherly eye on the CDs they listen to and the shows they watch, the company they keep and the books they read.\t You could say I'm a model parent. My children have never failed to make me proud, and I can say without the slightest embellishment that I have the finest family in the USA.\n\n  Two years ago, my wife Carol and I decided that our children\'s education would not be complete without some grounding in modern computers. To this end, we bought our children a brand new Compaq to learn with. The kids had a lot of fun using the handful of application programs we'd bought, such as Adobe's Photoshop and Microsoft's Word, and my wife and I were pleased that our gift was received so well. Our son Peter was most entranced by the device, and became quite a pro at surfing the net. When Peter began to spend whole days on the machine, I became concerned, but Carol advised me to calm down, and that it was only a passing phase. I was content to bow to her experience as a mother, until our youngest daughter, Cindy, charged into the living room one night to blurt out: \f\"Peter is a computer hacker!\"\f" ;
}

/*
        //exceeds max allowed by one char (3 bytes)
        //testString="Top box office:, "+BOLD+"1"+BOLD+":The Hobbit: An Unexpected Journey(★★★)"+BOLD+"2"+BOLD+":Rise of the Guardians(★★★✫)"+BOLD+"3"+BOLD+":Lincoln(★★★★✫)"+BOLD+"4"+BOLD+":Life of Pi(★★★★☆)"+BOLD+"5"+BOLD+":Skyfall(★★★★✫)"+BOLD+"6"+BOLD+":The Twilight Saga: Breaking Dawn Part 2(★★☆)"+BOLD+"7"+BOLD+":Wreck-it Ralph(★★★★☆)"+BOLD+"8"+BOLD+":Red Dawn(✫)"+BOLD+"9"+BOLD+":Playing for Keeps"+BOLD+"10"+BOLD+":Flight(★★★✰)"+BOLD+"11"+BOLD+":Hitchcock(★★★☆)"+BOLD+"12"+BOLD+":Argo(★★★★✫)"+BOLD+"13"+BOLD+":Silver Linings Playbook(★★★★✫)"+BOLD+"14"+BOLD+":Hotel Transylvania(★★)"+BOLD+"15"+BOLD+":Anna Karenina(★★★)"+BOLD+"16"+BOLD+":Here Comes the Boom(★✰)";
        testString="★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+BOLD+"★★★★✫"+"★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★★★✫★★"; //max unicode case.. still 460 chars
        testString="123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_";
        //testString=" I do so well in school, but I feel so horrible about it. Like in a lot of the \"hard\" classes, where a lot of my friends would fail the test, I would ace it. I feel so bad because I ruin the curve for them. Sometimes I just want to completely bomb a test just so they would get a better grade. But I can't get myself to do that for some odd reason. Everytime that I want to bomb a test, I always convince myself otherwise when it comes to it. I don't know what's so hard about these classes";
        System.out.println("Testing with string of length: " + testString.length() + ", bytes:" + byteLength(testString) + "\n");
        pager.init(testString) ;
        while (! pager.isEmpty()) {
            String next = pager.getNext();
            System.out.println("page string length: " + next.length() + ", byte length: " + byteLength(next));
            System.out.println(next + "\n") ;
        }

}
*/
