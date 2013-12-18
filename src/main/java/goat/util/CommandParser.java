package goat.util;

import goat.core.Message;
import static goat.util.StringUtil.removeFormattingAndColors;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;

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
        parse(m.getTrailing().trim()) ;
    }

    public CommandParser(String text) {
        parse(text.trim()) ;
    }

    /**
     * @param text String to be parsed
     */
    private void parse(String text) {
        // Actual regex before being escaped: [^\"\p{javaWhitespace]+=[^\"\p{javaWhitespace]+=\"([^\"]+?)\"|^[^\"\p{javaWhitespace]+|[^\"\p{javaWhitespace]+
        // ie match first word (command) OR match someword=anotherword OR match someword="some words"
        // idea is to describe each field instead of the delimiter between them,
        //String commandRegex = "\\w+=\\w+|\\w+=\\\"([^\\\"]+?)\\\"|^\\w+";
        String commandRegex = "[^\\\"\\p{javaWhitespace}]*?=[^\\\"\\p{javaWhitespace}]+|[^\\\"\\p{javaWhitespace}]+?=\\\"([^\\\"]+?)\\\"|^[^\\\"\\p{javaWhitespace}]+";
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
            buf = group.split("=",2);
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
     * Merges another goat query into this one.
     * Any args in this parser are not overwritten by args in the other parser
     * @param otherParser
     */
    public void merge(CommandParser otherParser) {
        remaining=remaining+" "+otherParser.remaining + " " + otherParser.command;
        for(String otherVar : otherParser.vars.keySet()) {
            if(!vars.containsKey(otherVar)) {
                vars.put(otherVar,otherParser.vars.get(otherVar));
            }
        }
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
        return remaining ;
    }


    /**
     * "films search num=20 poopy poop"
     *
     * Gives you "poopy poop" if you call remainingAfterWord("search")
     *
     * @return what remains of the line, after the command and vars and a supplied subcommand have been parsed out
     */
    public String remainingAfterWord(String word) {
        return remaining.replaceFirst(word, "");
    }

    public ArrayList<String> remainingAsArrayList() {
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
    public boolean hasVar(String name) {
        name = name.toLowerCase() ;
        if(vars.containsKey(name))
            return true ;
        else
            return false ;
    }

    /**
     * Checks for a standalone word - not part of an arg - among those remaining after command
     * @param name
     * @return
     */
    public boolean hasWord(String name) {
        name = name.toLowerCase();
        for(String r : remainingAsArrayList) {
            if(name.equals(r))
                return true;
        }
        return false;
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
     * @param defaultVal default to return if the param can't be parsed as an int
     *
     * @return value of var as Int, or default if it can't be parsed
     */
    public int getInt(String name, int defaultVal) {
        name = name.toLowerCase() ;
        int ret = defaultVal ;
        if (vars.containsKey(name)) {
            try {
                ret = Integer.parseInt((String) vars.get(name)) ;
            } catch (NumberFormatException e) {
                e.printStackTrace() ;
            }
        }
        return ret ;
    }

    /**
     * Variable-as-int getter, with default 0
     *
     * mostly for backwards compatibility
     *
     * @param name name of var to be fetched
     *
     * @return value of var as Int, or 0 if it can't be parsed
     */
    public int getInt(String name) {
        return getInt(name, 0);
    }

    /**
     * Convenience method
     *
     * @return int supplied via "number=", "num=" or remaining
     */
    public Double findNumber() throws NumberFormatException {
        Double ret;
        if (hasVar("num"))
            ret = Double.parseDouble(removeFormattingAndColors(get("num")));
        else if (hasVar("number"))
            ret = Double.parseDouble(removeFormattingAndColors(get("number")));
        else if (removeFormattingAndColors(remaining).matches("#" + doubleRegex))
            ret = Double.parseDouble(removeFormattingAndColors(remaining.substring(1)));
        else
            ret = Double.parseDouble(removeFormattingAndColors(remaining));
        return ret;
    }

    /**
     * Convenience method go with findNumber()
     *
     * @return true if we found "number=" or "num=" or remaining
     */
    public boolean hasNumber() {
        return hasVar("num") || hasVar("number") || remaining.matches("#?" + doubleRegex);
    }

    public boolean hasOnlyNumber() {
        return hasNumber() && (remaining.equals("") || remaining.matches("#?" + doubleRegex));
    }

    // this hideous regex is for string representations of Double
    //  see: http://www.regular-expressions.info/floatingpoint.html
    private final String doubleRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

}
