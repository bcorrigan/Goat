package goat.module;
/**
 * TestDice - Test the Dice module.
 * @author bc
 */
import goat.core.MockMessage;
import goat.core.BotStats;
import goat.core.Message;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.junit.*;
import static org.junit.Assert.*;

public class TestDiceRoll {
    private MockMessage testMessage;
    private DiceRoll dice;
    private Pattern p;
    private Matcher m;

    @Before
    public void setUp() {
        BotStats.testing = true;
        dice = new DiceRoll();
    }
    
    @Test
    public void testSimpleD6Roll() {
        //do 100 rolls and check all as expected
        p = Pattern.compile(".*(\\d+d\\d+):(\\d):(\\d)\\s+Total:(\\d).*");
        for( int i=0; i<100; i++ ) {
            roll("1d6","roll");
            m = p.matcher(cleanMessage(testMessage.sentMessage));
            assertTrue("uh oh, result should be formatted as expected.", m.matches() );
            assertTrue( "Score should be in range 1..6", 1<=Integer.parseInt(m.group(2)) && 6>=Integer.parseInt(m.group(2)));
            assertTrue( "Total score should be equal to score", Integer.parseInt(m.group(4)) == Integer.parseInt(m.group(4)));
            assertEquals("Should be 1d6 that's been rolled.", "1d6", m.group(1));
        }
    }

    @Test
    public void testD6D20Roll() {
        p = Pattern.compile(".*(\\d+d\\d+):(\\d+):(\\d+)\\s+(\\d+d\\d+):(\\d),(\\d):(\\d+)\\s+Total:(\\d+).*");
        for( int i=0; i<100; i++ ) {
            roll("2d6 + 1d20","roll");            
            m = p.matcher(cleanMessage(testMessage.sentMessage));
            assertTrue("uh oh, result should be formatted as expected.", m.matches() );
            assertEquals("Die type should be correct", "2d6", m.group(4));
            assertEquals("Die type should be correct", "1d20", m.group(1));
            assertEquals("Dice should total up correctly", Integer.parseInt(m.group(5)) + Integer.parseInt(m.group(6)), Integer.parseInt(m.group(7)));
            assertTrue("Dice range should be correct", 1<=Integer.parseInt(m.group(5)) && 6>=Integer.parseInt(m.group(5)));
            assertTrue("Dice range should be correct", 1<=Integer.parseInt(m.group(6)) && 6>=Integer.parseInt(m.group(6)));
            assertEquals("Dice should total up correctly", Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            assertEquals("Dice should total up correctly", Integer.parseInt(m.group(3)) + Integer.parseInt(m.group(7)), Integer.parseInt(m.group(8)) );
        }
    }

    @Test
    public void testAggregation() {
        roll("1d1+3d1+0d1+0d1  + 2d1+4d1+0d0+0d0 ","roll"); 
        String result = cleanMessage(testMessage.sentMessage);
        assertEquals( "Aggregation of terms should work as expected", "10d1:1,1,1,1,1,1,1,1,1,1:10  Total:10", result );
    }
    
    @Test
    public void testAbusiveRollSize() {
    	roll("100d0+345d23+780d99","roll");
    	String result = cleanMessage(testMessage.sentMessage);
    	assertEquals("1)Goat should refuse to roll a zillion dice", "I'm not rolling that many dice, I'd be here all day!", result );
    	roll("99999999999999999999999999999999999999999999999999999999999999999999d88","roll");
    	result = cleanMessage(testMessage.sentMessage);
    	assertEquals("2)Goat should refuse to roll a zillion dice", "It is so funny to make me try and throw more dice than exist in the universe.", result );
    }
    
    @Test
    public void testAbusiveDieSize() {
    	roll("1d999999999999999999999999999999999999999999999999999999999999999999999999","roll");
    	String result = cleanMessage(testMessage.sentMessage);
    	assertEquals("Goat should refuse to roll a die with a zillion sides", "I'm not rolling a sphere, sorry.", result);
    }
    
    @Test
    public void testCrapInput() {
    	roll("d6+d6+2x9","roll");
    	String result = cleanMessage(testMessage.sentMessage);
    	assertEquals("Goat should detect shite input", "Sorry, I don't know how to do that.", result);
    	roll("-5d6","roll");
    	result = cleanMessage(testMessage.sentMessage);
    	assertEquals("Goat should detect shite input", "Sorry, I don't know how to do that.", result);
    	roll("100d6+1.0d6","roll");
    	result = cleanMessage(testMessage.sentMessage);
    	assertEquals("Goat should detect shite input", "Sorry, I don't know how to do that.", result);
    	roll("100d6+1d6.0001","roll");
    	result = cleanMessage(testMessage.sentMessage);
    	assertEquals("Goat should detect shite input", "Sorry, I don't know how to do that.", result);
    }
    
    @Test
    public void testComplexRoll() {
    	
    	//					   -7d100  1	2	  3		 4		5	   6	  7		 8   	9  		  10  -3d2	  11	 12	  13	14		  15 -3d9	  16	 17	   18	 19				 20
    	p = Pattern.compile(".*(\\d+d\\d+):(\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+):(\\d+)\\s+(\\d+d\\d+):(\\d),(\\d),(\\d):(\\d+)\\s+(\\d+d\\d+):(\\d),(\\d),(\\d):(\\d+)\\s+Total:(\\d+).*");
    	for( int j=0; j<1000; j++ ) {
			roll("d9     +d2+ 2d9 + 2d2 + 7d100","roll");
			m = p.matcher(cleanMessage(testMessage.sentMessage));
		    assertTrue("uh oh, result should be formatted as expected.", m.matches() );
		    assertEquals("Die type should be correct", "7d100", m.group(1));
		    assertEquals("Die type should be correct", "3d2", m.group(10));
		    assertEquals("Die type should be correct", "3d9", m.group(15));
		    assertEquals("Dice should total up correctly for 7d100", Integer.parseInt(m.group(2))+Integer.parseInt(m.group(3))+Integer.parseInt(m.group(4))+Integer.parseInt(m.group(5))+Integer.parseInt(m.group(6))+Integer.parseInt(m.group(7))+Integer.parseInt(m.group(8)), Integer.parseInt(m.group(9)) );
		    assertEquals("Dice should total up correctly for 3d2", Integer.parseInt(m.group(11))+Integer.parseInt(m.group(12))+Integer.parseInt(m.group(13)), Integer.parseInt(m.group(14)) );
		    assertEquals("Dice should total up correctly for 3d9", Integer.parseInt(m.group(16))+Integer.parseInt(m.group(17))+Integer.parseInt(m.group(18)), Integer.parseInt(m.group(19)) );
		    assertEquals("Dice totals should total up correctly", Integer.parseInt(m.group(9))+Integer.parseInt(m.group(14))+Integer.parseInt(m.group(19)), Integer.parseInt(m.group(20)) );
		    for( int i=2; i<9; i++ )
		    	assertTrue("Dice range should be correct for 7d100", 1<=Integer.parseInt(m.group(i)) && 100>=Integer.parseInt(m.group(i)) );
		    for( int i=11; i<14; i++ )
		    	assertTrue("Dice range should be correct for 3d2", 1<=Integer.parseInt(m.group(i)) && 2>=Integer.parseInt(m.group(i)) );
		    for( int i=16; i<19; i++ )
		    	assertTrue("Dice range should be correct for 3d9", 1<=Integer.parseInt(m.group(i)) && 9>=Integer.parseInt(m.group(i)) );
    	}
    }
    
    @Test
    public void testAbsurdlyLargeRoll() {
    	String roll = "1d1";
    	String expectedResult = "1000d1:1";
    	for( int i=0; i<999; i++ ) {
    		expectedResult += ",1";
    		roll += "+1d1";
    	}
    	expectedResult+=":1000  Total:1000";
    	roll(roll,"roll");
    }
    
    @Test
    public void testCoinToss() {
        for( int i=0; i<100; i++ ) {
        	roll("","toss");
        	assertTrue("Coin toss should result in heads or tails.", testMessage.sentMessage.equals("Heads.") || testMessage.sentMessage.equals("Tails."));
        	roll(" a coin baby!!","toss");
        	assertTrue("Coin toss should result in heads or tails.", testMessage.sentMessage.equals("Heads.") || testMessage.sentMessage.equals("Tails."));
        }        
    }
    
    @Test
    public void testCoinTossIgnored() {
        for( int i=0; i<100; i++ ) {
        	roll(" this salad","toss");
        	assertNull("Coin toss should be ignored", testMessage.sentMessage);
        }        
    }

    private void roll(String roll, String modCommand) {
        testMessage = new MockMessage("test1","test2","test3","test4");
        testMessage.sender = "bc";
        testMessage.modTrailing = roll;
        testMessage.modCommand = modCommand;
        dice.processChannelMessage(testMessage);
    }

    private String cleanMessage(String msg) {
        return msg.replaceAll(Message.BOLD, "").replaceAll(Message.UNDERLINE, "").replaceAll(Message.NORMAL, "");
    }
}
