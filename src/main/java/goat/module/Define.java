package goat.module ;

import goat.Goat;
import goat.core.IrcMessage;
import goat.core.Module;
import goat.util.CommandParser;
import goat.util.DICTClient;
import goat.util.Definition;
import goat.util.UrbanDictionary;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Vector;


/**
 * Module to provide access to a dict-protocol dictionary server.
 *
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * <p/>
 * This fucker needs some serious reorganizing before anything more is added to it (2005-05-23)
 * <p/>
 *
 *	@author encontrado
 *
 * @version 1.0
 */
public class Define extends Module {

    private static String host = "dict.org" ;
    public boolean debug = false ;
    @Override
    public int messageType() {
        return WANT_COMMAND_MESSAGES;
    }

    @Override
    public String[] getCommands() {
        return new String[] { "define", "randef", "dictionaries", "dictionary", "oed", "thesaurus"};
    }

    public Define() {
    }


    @Override
    public void processPrivateMessage(Message m) {
        processChannelMessage(m) ;
    }

    @Override
    public void processChannelMessage(Message m) {
        //parse out args

        if(debug)
            System.out.println("processing command: " + m.getModCommand()) ;
        if (m.getModCommand().equalsIgnoreCase("define") || m.getModCommand().equalsIgnoreCase("thesaurus")) {
            define(m) ;
        } else if (m.getModCommand().equalsIgnoreCase("randef")) {
            randef(m) ;
        } else if (m.getModCommand().equalsIgnoreCase("dictionaries")) {
            dictionaries(m) ;
        } else if (m.getModCommand().equalsIgnoreCase("dictionary")) {
            dictionary(m) ;
        } else if (m.getModCommand().equalsIgnoreCase("oed")) {
            m.reply(oedUrl(m.getModTrailing())) ;
        }
    }

    private void define(Message m) {
        CommandParser parser = new CommandParser(m) ;
        String dictionary = "*" ;
        if (parser.hasVar("dictionary"))
            dictionary = parser.get("dictionary") ;
        else if (parser.hasVar("dict") )
            dictionary = parser.get("dict");
        if (m.getModCommand().equalsIgnoreCase("thesaurus"))
            dictionary = "moby-thes";
        int num = 1 ;
        if (parser.hasVar("number"))
            num = parser.getInt("number") ;
        else if (parser.hasVar("num"))
            num = parser.getInt("num") ;
        // check for num not negative, not zero (will be zero if specified, or if parsing input threw an Num Exception)
        if (num <= 0 ) {
            m.reply("Very funny.  Go back to your cubicle, nerd.") ;
            return ;
        }
        String word = parser.remaining() ;
        String text = "" ;
        //make sure we've got a word, if not, ask wordgame mod what its last answer for this channel is, if any, and use that.
        if(null == word || word.equals("") ) {
            WordGame wordgameMod = (WordGame) Goat.modController.getLoaded("WordGame") ;
            if (wordgameMod != null && (wordgameMod.inAllChannels || wordgameMod.getChannels().contains(m.getChanname()))) {
                String lastWord = WordGame.getLastWinningWord(m.getChanname()) ;
                if(lastWord != null)
                    word = lastWord ;
                else {
                    m.reply("I can't decide which word I'm supposed to define for you.") ;
                    return ;
                }
            } else {
                m.reply("Er, define what, exactly?") ;
                return ;
            }
        }

        Vector<Definition> definitionList = null ;
        String[][] matchList = null ;

        if (dictionary.equalsIgnoreCase("urban")) {
            UrbanDictionary urban = new UrbanDictionary(word.replaceAll("[\"']",""));
            if (!urban.error.equals("")) {
                m.reply(urban.error);
                return;
            }
            definitionList = urban.definitions;
            matchList = urban.matchList;
        }
        else if (dictionary.equalsIgnoreCase("oed")) {
            m.reply(oedUrl(word)) ;
            return ;  //naughty!
        }
        else {
            String[][] dbList = null ;
            DICTClient dc = getDICTClient(m) ;
            if(null == dc)
                return ;
            try {
                dbList = dc.getDatabases() ;
                // Check to see if we were given a valid dictionary
                if(! dictionary.equals("*")) {
                    boolean found = false ;
                    for (String[] aDbList : dbList) {
                        if (dictionary.equals(aDbList[0])) {
                            found = true;
                            break;
                        }
                    }
                    if( ! found ) {
                        m.reply("\"" + dictionary + "\" is not a valid dictionary.") ;
                        dictionaries(m) ;
                        return ;
                    }
                }
                definitionList = dc.getDefinitions( new String[] {dictionary}, word) ;
                matchList = dc.getMatches(new String[] {dictionary}, ".", word) ;
            } catch (ConnectException e) {
                m.reply("Something went wrong while connecting to DICT server at " + host ) ;
                e.printStackTrace() ;
            } finally {
                dc.close() ;
            }
        }
        // check not-null definition list and match list
        if(null == definitionList || null == matchList) {
            System.out.println("I'm sorry, Dave, something has gone horribly wrong.") ;
            return ;
        }
        // check for empty definition list
        if (definitionList.isEmpty()) {
            // check match list not empty
            String reply = "No definitions found for \"" + word + "\"" ;
            if (! dictionary.equals("*"))
                reply += " in dictionary " + dictionary + "." ;
            else
                reply += "." ;
            if (0 == matchList.length) {
                m.pagedReply(reply + "  Couldn't find any alternate spelling suggestions.") ;
            } else {
                String suggestions = "" ;
                for (String[] aMatchList : matchList) suggestions += " " + aMatchList[1];
                suggestions = suggestions.replaceAll("\"", "") ;
                suggestions = suggestions.trim() ;
                m.pagedReply(reply + "  Suggestions: " + suggestions) ;
            }
        }
        // check num not greater than number of elements in list
        else if (num > definitionList.size() ) {
            String line = "I don't have " + num + " definitions for \"" + word ;
            if (! dictionary.equals("*") )
                line = line + "\" in dictionary \"" + dictionary + "\"." ;
            else
                line = line + "\"." ;
            m.reply(line) ;
        } else {
            Definition d = definitionList.get(num - 1) ;
            text = d.getWord() + " (" + d.getDatabaseShort() + "): " + d.getDefinition() ;
            // System.out.println("Definition for '" + d.getWord() + "'\n\t" + d.getDefinition());
            m.reply(text) ;
            // show available definitions, if more than one.
            if (definitionList.size() > 1) {
                int perDict = 0 ;
                Definition thisDef = definitionList.get(0) ;
                String thisDict = thisDef.getDatabaseShort() ;
                String msg = "Definitions available: " + thisDict + "(" ;
                for (Object aDefinitionList : definitionList) {
                    thisDef = (Definition) aDefinitionList;
                    if (!thisDict.equals(thisDef.getDatabaseShort())) {
                        thisDict = thisDef.getDatabaseShort();
                        msg += perDict + ") " + thisDict + "(";
                        perDict = 1;
                    } else {
                        perDict++;
                    }
                }
                msg += perDict + ")" ;
                m.reply(msg) ;
            }
        }
    }

    private void randef(Message m) {
        m.reply("Not implmemented, please stand by") ;
    }

    private String oedUrl(String word) {
        return "http://dictionary.oed.com/cgi/findword?query_type=word&queryword=" + word.replaceAll(" ", "%20") ;
    }

    private String dictionaries() {
    	String ret = "" ;
    	try {
            DICTClient dc = new DICTClient(host) ;
            String[][] dbList = dc.getDatabases() ;
            dc.close() ;

            for (String[] aDbList : dbList) {
                if (ret.equals(""))
                    ret = aDbList[0];
                else
                    ret = ret + ", " + aDbList[0];
            }
            ret += ", ";
    	} catch (Exception e) {
            ret = "Couldn't talk to dict server.  Other dictionaries:  ";
    	}
        ret += "urban, oed" ;
        return ret;
    }

    private void dictionaries(Message m) {
    	m.pagedReply(dictionaries()) ;
    }

    private void dictionary(Message m) {
        DICTClient dc = getDICTClient(m) ;
        String code = m.getModTrailing().trim() ;
        String line = "" ;
        String[][] dbList = dc.getDatabases() ;
        dc.close() ;
        boolean found = false ;
        if (code.equalsIgnoreCase("oed")) {
            line = "oed: The One True English Dictionary" ;
            found = true ;
        }
        else if (code.equalsIgnoreCase("urban") ) {
            line = "urban:  " + UrbanDictionary.dictionaryDescription ;
            found = true ;
        }
        else {
            for (String[] aDbList : dbList) {
                if (aDbList[0].equals(code)) {
                    found = true;
                    line = code + ": " + aDbList[1];
                    break;
                }
                if (line.equals(""))
                    line = aDbList[0];
                else
                    line = line + ", " + aDbList[0];
                line = line.trim();
            }
        }
        if (! found)
            line = "Dictionary \"" + code + "\" not found; available dictionaries: " + dictionaries() ;
        m.pagedReply(line) ;
    }

    private DICTClient getDICTClient(Message m) {
        DICTClient dc = null ;
        try {
            dc = new DICTClient(host) ;
        } catch (UnknownHostException e) {
            e.printStackTrace() ;
            m.reply("Couldn't talk to dict server: host \"" + host + "\" unknown") ;
            // System.exit(1) ; // um.
        } catch (ConnectException e) {
            e.printStackTrace() ;
            m.reply("Couldn't talk to dict server.") ;
        }
        return dc ;
    }

    public static void main(String[] arg) {
        Define define = new Define() ;
        DICTClient dc = define.getDICTClient(new IrcMessage("","","","")) ;
        System.out.println("Starting main()") ;
        String[][] dbList = dc.getDatabases() ;
        String line = "" ;
        for (String[] aDbList : dbList) {
            line = line + " " + aDbList[0];
        }
        line = line.trim() ;
        System.out.println(line) ;
        dc.close() ;
    }
}
