package goat.util ;

import goat.core.Message ;

import java.util.Collections;
import java.util.HashMap ;
import java.util.ArrayList ;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple command parser
 * 
 * @author encontrado
 * 
 * 	Takes a string or Message, condenses whitespace,
 * 	strips off first word, makes available through command(),
 * 	finds "foo=bar" substrings, makes values available via get() and getInt(),
 * 	makes what's left available via remaining().
 * 	variable names are all case-insensitive.  All values preserve case.
 */
public class CommandParser {

	private HashMap<String,String> vars = new HashMap<String,String>();
	private String command = "" ;
	private String remaining = "" ; 
	private ArrayList<String> remainingAsArrayList = new ArrayList<String>() ;
	
	/**
	 * @param m Message to be parsed
	 */
	public CommandParser(Message m) {
		command = m.getModCommand() ;
		parse(m.getModTrailing().trim()) ;
	}

	public CommandParser(String text) {
		parse(text.trim()) ;
	}

	/**
	 * @param text String to be parsed
	 */
	private void parse(String text) {
		// Actual regex before being escaped: \w+=\w+|\w+=\"([^\"]+?)\"|^\w+
		// ie match first word (command) OR match someword=anotherword OR match someword="some words"
		// idea is to describe each field instead of the delimiter between them,
		String commandRegex = "\\w+=\\w+|\\w+=\\\"([^\\\"]+?)\\\"|^\\w+";
		Pattern commandRE = Pattern.compile(commandRegex);
		Matcher m = commandRE.matcher(text);

		int last=0;
		String[] buf = {};
		if(command.equals("") & m.find()) { //we do not want to short circuit! that causes a bug
			if(m.group().contains("=")) {
				//not command string proper
				m.reset();
			} else {
				command = m.group().trim();
				last=m.end();
				remaining+=text.substring(0,m.start()).trim() + " "; //anything unmatched from start onto remaining
			}
		} 
		
		//process each match
		while(m.find()) {
			String group = m.group();
			//anything unmatched between last match and this match added to remaining
			remaining+=text.substring(last,m.start()).trim() + " ";
			last=m.end();
			buf = group.split("=");
			//trim quotes
			if( buf[1].startsWith("\"")) 
				buf[1] = buf[1].substring(1);
			if( buf[1].endsWith("\""))
				buf[1] = buf[1].substring(0,buf[1].length()-1);
			vars.put(buf[0].toLowerCase(), buf[1]);
		}
		//unmatched tail onto remaining
		remaining += text.substring(last, text.length()).trim();
		remaining=remaining.trim();
		
		//now for remaining as list - simply split on whitespace and add to arraylist
		//no quote handling here but dunno if we want it
		// we need to wrap this in an if() because java's split() returns an
		// array with one element in it ("") when invoked on an empty string.
		if(!remaining.equals(""))
			Collections.addAll(remainingAsArrayList, remaining.split("\\s+"));
	}
	
	/**
	 * Command getter
	 *
	 * @return the "command," ie, the first word on the line
	 */
	public String command() {
		return command ;
	}

	/**
	 * Remainder getters
	 *
	 * @return what remains of the line, after the command and vars have been parsed out
	 */
	public String remaining() {
		// System.out.println("remaining in remaining(): " + this.remaining) ;
		return remaining ;
	}

	public ArrayList remainingAsArrayList() {
		return remainingAsArrayList ;
	}

	/**
	 * Remainder setter.  For use in further user processing of the command.
	 *
	 * @param string New remainder.  
	 */
	public void setRemaining(String string) {
		if( string != null)
			remaining = string.trim() ;
	}
	
	/**
	 * Remainder checker
	 *
	 * @return true if there's some text remaining, false otherwise.
	 */
	public boolean hasRemaining() {
		if (remaining.equals(""))
			return false ;
		else
			return true ;
	}

	/**
	 * Variable Checker
	 *
	 * @param name var name to check for
	 * 
	 * @return true if we've got a var with that name (case insensitive), otherwise false.
	 */
	public boolean has(String name) {
		name = name.toLowerCase() ;
		if(vars.containsKey(name))
			return true ;
		else
			return false ;
	}
		
	/**
	 * Variable getter
	 * 
	 * @param name name of var to be fetched
	 *
	 * @return value of var, as String
	 */
	public String get(String name) {
		name = name.toLowerCase() ;
		if (vars.containsKey(name)) 
			return (String) vars.get(name) ;
		else 
			return null ;
	}

	/**
	 * Variable-as-int getter
	 * 
	 * @param name name of var to be fetched
	 *
	 * @return value of var, as Int
	 */
	public int getInt(String name) {
		name = name.toLowerCase() ;
		int ret = 0 ;
		if (vars.containsKey(name)) {
			try {
				ret = Integer.parseInt((String) vars.get(name)) ;
			} catch (NumberFormatException e) {
				e.printStackTrace() ;
			}
		}
		return ret ;
	}
	
	/*
	 
	/**
	 * Fetch names of all stored vars
	 *
	public String[] listVars() {
		String vars[] = {} ;
		return vars ;
	}
	
	/**
	 * Dump all variable name/value pairs
	 *
	 * @return 2-dimensional array dump, with var name in dump[i][0], and value in dump[i][1] ;
	 *
	public String[][] dumpVars() {
		String[][] pairs = {{}} ;
		return pairs ;
	}
	
	*/
}
