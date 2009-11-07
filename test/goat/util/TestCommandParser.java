package goat.util;

import org.junit.*;

import static org.junit.Assert.*;

public class TestCommandParser {	
	CommandParser commandParser; 
	
	@Test
	public void testParser() {
		//test the basics of the parser
		commandParser = new CommandParser("testCommand command=somecommand somecommand2=\"another command\" remaining crap" );
		assertTrue("command should be set as a variable", commandParser.has("command"));
		assertTrue("somecommand2 should be set as a variable", commandParser.has("somecommand2"));
		assertEquals("command value should be set correctly","somecommand",commandParser.get("command"));
		assertEquals("somecommand2 value should be set correctly","another command",commandParser.get("somecommand2"));
		assertEquals("testCommand should be set correctly","testCommand",commandParser.command());
		assertEquals("remaining should be set correctly", "remaining crap", commandParser.remaining());
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
	}
	
	@Test
	public void testRemainingAsList() {
		commandParser = new CommandParser("headline one two three four five six");
		assertEquals("remaining array list should have 6 items",6,commandParser.remainingAsArrayList().size());
		assertEquals("First item should be one","one",commandParser.remainingAsArrayList().get(0));
		assertEquals("third item should be three","three",commandParser.remainingAsArrayList().get(2));
		assertEquals("sixth item should be three","six",commandParser.remainingAsArrayList().get(5));
		assertEquals("command should be headline","headline",commandParser.command());
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
	}
	
	@Test
	public void testVariedRemaining() {
		commandParser = new CommandParser("  testCommand ass dick=\"huge monster\" balls brain=small jism  ");
		assertEquals("remaining array list should have 3 items",3,commandParser.remainingAsArrayList().size());
		assertEquals("First item should be ass","ass",commandParser.remainingAsArrayList().get(0));
		assertEquals("second item should be balls","balls",commandParser.remainingAsArrayList().get(1));
		assertEquals("third item should be jism","jism",commandParser.remainingAsArrayList().get(2));
		assertEquals("remaining should be set correctly", "ass balls jism", commandParser.remaining());
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
		assertEquals("testCommand should be set correctly","testCommand",commandParser.command());
		assertEquals("dick value should be set correctly","huge monster",commandParser.get("dick"));
		assertEquals("brain value should be set correctly","small",commandParser.get("brain"));
	}
	
	@Test
	public void testNoRemaining() {
		commandParser = new CommandParser("  testCommand  dick=\"huge monster\" brain=small   ");
		assertEquals("remaining array list should have 0 items",0,commandParser.remainingAsArrayList().size());
		assertEquals("remaining should be set correctly", "", commandParser.remaining());
		assertFalse("hasRemaining should be false",commandParser.hasRemaining());
		assertEquals("testCommand should be set correctly","testCommand",commandParser.command());
		assertEquals("dick value should be set correctly","huge monster",commandParser.get("dick"));
		assertEquals("brain value should be set correctly","small",commandParser.get("brain"));
	}
	
	@Test
	public void testNoCommand() {
		commandParser = new CommandParser("  dick=\"huge monster\" brain=small  ass ");
		assertEquals("remaining array list should have 1 items",1,commandParser.remainingAsArrayList().size());
		assertEquals("remaining should be set correctly", "ass", commandParser.remaining());
		assertEquals("First item should be ass","ass",commandParser.remainingAsArrayList().get(0));
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
		assertEquals("No command should be set","",commandParser.command());
		assertEquals("dick value should be set correctly","huge monster",commandParser.get("dick"));
		assertEquals("brain value should be set correctly","small",commandParser.get("brain"));
	}
	
	@Test
	public void testNullString() {
		commandParser = new CommandParser("");
		assertEquals("remaining array list should have 0 items",0,commandParser.remainingAsArrayList().size());
		assertEquals("remaining should be set correctly", "", commandParser.remaining());
		assertFalse("hasRemaining should be false",commandParser.hasRemaining());
		assertEquals("No command should be set","",commandParser.command());
	}
	
	@Test
	public void testEmptyString() {
		commandParser = new CommandParser("      ");
		assertEquals("remaining array list should have 0 items",0,commandParser.remainingAsArrayList().size());
		assertEquals("remaining should be set correctly", "", commandParser.remaining());
		assertFalse("hasRemaining should be false",commandParser.hasRemaining());
		assertEquals("No command should be set","",commandParser.command());
	}
	
	@Test
	public void testNonenseString() {
		commandParser = new CommandParser("&£:hjs *(\"ggg fock and=\"buttock  ");
		assertEquals("remaining array list should have 3 items",3,commandParser.remainingAsArrayList().size());
		assertEquals("remaining should be set correctly", "*(\"ggg fock and=\"buttock", commandParser.remaining());
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
		assertEquals("Command should be set","&£:hjs",commandParser.command());
	}
	
	@Test
	public void testDoubleQuotes() {
		//test the basics of the parser
		commandParser = new CommandParser("testCommand command=\"somecommand one\" somecommand2=\"another command\" remaining crap" );
		assertTrue("command should be set as a variable", commandParser.has("command"));
		assertTrue("somecommand2 should be set as a variable", commandParser.has("somecommand2"));
		assertEquals("command value should be set correctly","somecommand one",commandParser.get("command"));
		assertEquals("somecommand2 value should be set correctly","another command",commandParser.get("somecommand2"));
		assertEquals("testCommand should be set correctly","testCommand",commandParser.command());
		assertEquals("remaining should be set correctly", "remaining crap", commandParser.remaining());
		assertTrue("hasRemaining should be true",commandParser.hasRemaining());
	}
	
	@Test
	public void testComplexUrl() {
		//this tests passing in something with =s signs in it
		commandParser = new CommandParser("tweetsearch radius=25 location=http://maps.google.co.uk/maps?f=q&source=s_q&hl=en&geocode=&q=mekkah&sll=55.835197,-4.264637&sspn=0.000672,0.001574&ie=UTF8&hq=&hnear=Mecca,+Saudi+Arabia&ll=21.429101,39.8172&spn=0.142537,0.20153&t=h&z=13");
		assertEquals("Command value should be set correctly", "tweetsearch", commandParser.command());
		assertEquals("Radius should be set correctly", 25, commandParser.getInt("radius"));
		assertEquals("Location should be set correctly", "http://maps.google.co.uk/maps?f=q&source=s_q&hl=en&geocode=&q=mekkah&sll=55.835197,-4.264637&sspn=0.000672,0.001574&ie=UTF8&hq=&hnear=Mecca,+Saudi+Arabia&ll=21.429101,39.8172&spn=0.142537,0.20153&t=h&z=13", commandParser.get("location"));
		assertFalse("hasRemaining should be false", commandParser.hasRemaining());
	}
}
