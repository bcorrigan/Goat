package goat.util ;

import goat.core.Message ;
import java.util.HashMap ;

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

	private HashMap vars = new HashMap();
	private String command = "" ;
	private String remaining = "" ; 
	
	/**
	 * @param m Message to be parsed
	 */
	public CommandParser(Message m) {
		command = m.modCommand ;
		parse(m.modTrailing) ;
	}

	public CommandParser(String text) {
		parse(text) ;
	}

	/**
	 * @param text String to be parsed
	 */
	private void parse(String text) {
		//TODO might be nice if this handled quotes helpfully
		String[] words = text.split("\\s+") ;
		int start = 0 ;
		if (command.equals("")) {
			command = words[0] ;
			start = 1 ;
		}
		String word = "" ;
		String[] buf = {} ;
		for(int i=start;i<words.length; i++) {
			//quote handling would go in here somewhere
			word = words[i] ;
			if ( word.matches("\\S+=\\S+") ) {
				buf = word.split("=") ;
				vars.put(buf[0].toLowerCase(),buf[1]) ;
			} else {
				remaining = remaining + " " + word ;
			}
		}
		remaining = remaining.trim() ;
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
	 * Remainder getter
	 *
	 * @return what remains of the line, after the command and vars have been parsed out
	 */
	public String remaining() {
		System.out.println("remaining in remaining(): " + this.remaining) ;
		return remaining ;
	}

	/**
	 * Checker
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
