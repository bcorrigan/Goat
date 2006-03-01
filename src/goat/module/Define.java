package goat.module ;

import goat.Goat;
import goat.core.Module;
import goat.core.Message;
import goat.core.ModuleController;
import goat.util.DICTClient;
import goat.util.Definition;
import goat.util.CommandParser;
import goat.module.WordGame;

import java.io.*;
import java.net.* ;
import java.util.Vector;
import java.util.regex.* ;


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
	private static String urbandictionaryDescription = "The somewhat spotty slang dictionary at urbandictionary.com" ;
   
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[] { "define", "randef", "dictionaries", "dictionary", "oed" };
   }
	
	public Define() {
	}

	
	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		//parse out args

		System.out.println("processing command: " + m.modCommand) ;
		if (m.modCommand.equalsIgnoreCase("define")) {
			define(m) ;
		} else if (m.modCommand.equalsIgnoreCase("randef")) { 
			randef(m) ;
		} else if (m.modCommand.equalsIgnoreCase("dictionaries")) { 
			dictionaries(m) ;
		} else if (m.modCommand.equalsIgnoreCase("dictionary")) {
			dictionary(m) ;
		} else if (m.modCommand.equalsIgnoreCase("oed")) {
			m.createReply(oedUrl(m.modTrailing)).send() ;
		}
	}

	private void define(Message m) {
		CommandParser parser = new CommandParser(m) ;
		String dictionary = "*" ;
		if (parser.has("dictionary")) 
			dictionary = parser.get("dictionary") ;
		else if (parser.has("dict") ) 
			dictionary = parser.get("dict") ;
		int num = 1 ;
		if (parser.has("number"))
			num = parser.getInt("number") ;
		else if (parser.has("num")) 
			num = parser.getInt("num") ;
		// check for num not negative, not zero (will be zero if specified, or if parsing input threw an Num Exception)
		if (num <= 0 ) {
			m.createReply("Very funny.  Go back to your cubicle, nerd.").send() ;
			return ;
		}
		String word = parser.remaining() ;
		String text = "" ;
		//make sure we've got a word, if not, ask wordgame mod what its last answer for this channel is, if any, and use that.
		if(null == word || word.equals("") ) {
			WordGame wordgameMod = (WordGame) Goat.modController.get("WordGame") ;
			if ((wordgameMod != null) && (wordgameMod.inChannel(m.params))) {
				String lastWord = wordgameMod.getLastAnswer(m.params) ;
				if(lastWord != null)
					word = lastWord ;
				else {
					m.createReply("Didn't find last answer; maybe you haven't played a round of wordgame yet?").send() ;
					return ;
				}
			} else {
				m.createReply("Er, define what, exactly?").send() ;
				return ;
			}
		}

      Vector definitionList = null ;
      String[][] matchList = null ;
         
      // This next block is thuggish.  If we keep tacking dictionaries onto the project,
      // we might want to rearrange stuff here and elsewhere to do things consistently
      if (dictionary.equalsIgnoreCase("urban")) {
         definitionList = getUrbanDefinitions(word) ;
         // this next is ugly, but urbandictionary.com doesn't have a guess-mis-spelt-word feature
         if (definitionList.isEmpty())
            matchList =  new String[0][0];
         else {
            matchList = new String[1][1] ;
            matchList[0][0] = word ;
         }
      } 
      else if (dictionary.equalsIgnoreCase("oed")) {
         // If we were clever monkeys, we could get goat to ask bugmenot for 
         //   a login/password for oed.com, and then use that to get the page, 
         //   and then parse the page for definitionList and matchList.
         //   instead, we'll just burp up a URL, and let the user do the legwork.
			m.createReply(oedUrl(word)).send() ;
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
             for (int i=0; i<dbList.length ;i++) {
                if (dictionary.equals(dbList[i][0])) {
                   found = true ;
                   break ;
                }
             }
   				if( ! found ) {
   					m.createReply("\"" + dictionary + "\" is not a valid dictionary.").send() ;
   					dictionaries(m) ;
   					return ;
   				}
   			}
   			definitionList = dc.getDefinitions( new String[] {dictionary}, word) ;
   			matchList = dc.getMatches(new String[] {dictionary}, ".", word) ;
   		} catch (ConnectException e) {
   			m.createReply("Something went wrong while connecting to DICT server at " + host ) ;
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
				m.createPagedReply(reply + "  Couldn't find any alternate spelling suggestions.").send() ;
			} else {
				String suggestions = "" ;
				for(int i=0;i<matchList.length;i++) 
					suggestions += " " + matchList[i][1] ;
				suggestions = suggestions.replaceAll("\"", "") ;
				suggestions = suggestions.trim() ;
				m.createPagedReply(reply + "  Suggestions: " + suggestions).send() ;
			}
			return ;
		}
		// check num not greater than number of elements in list
		if (num > definitionList.size() ) {
			String line = "I don't have " + num + " definitions for \"" + word ;
			if (! dictionary.equals("*") ) 
				line = line + "\" in dictionary \"" + dictionary + "\"." ;
			else
				line = line + "\"." ;
			m.createReply(line).send() ;
			return ;
		}
      
		Definition d = (Definition) definitionList.get(num - 1) ;
		text = d.getWord() + " (" + d.getDatabaseShort() + "): " + d.getDefinition() ;
		m.createPagedReply(text).send() ;
		// show available definitions, if more than one.
		if (definitionList.size() > 1) {
			int perDict = 0 ;
			Definition thisDef = (Definition) definitionList.get(0) ;
			String thisDict = thisDef.getDatabaseShort() ;
			String msg = "Definitions available: " + thisDict + "(" ;
			for(int i=0;i<definitionList.size();i++) {
				thisDef = (Definition) definitionList.get(i) ;
				if (! thisDict.equals(thisDef.getDatabaseShort())) {
					thisDict = thisDef.getDatabaseShort() ;
					msg += perDict + ") " + thisDict + "(" ;
					perDict = 1 ;
				} else {
					perDict++ ;
				}
			}
			msg += perDict + ")" ;
			m.createReply(msg).send() ;
		}
	}
	
	private void randef(Message m) {
		m.createReply("Not implmemented, please stand by").send() ; 
	}

	private String oedUrl(String word) {
		return "http://dictionary.oed.com/cgi/findword?query_type=word&queryword=" + word.replaceAll(" ", "%20") ;
	}
	
	private String urbanUrl(String word) {
		return "http://www.urbandictionary.com/define.php?term=" + word.replaceAll(" ", "%20") ;
	}
        
	private Vector getUrbanDefinitions(String word) {
      Vector definitionList = null;
      HttpURLConnection connection = null;
      try {
         URL urban = new URL(urbanUrl(word));
         connection = (HttpURLConnection) urban.openConnection();
         /* incompatible with 1.4
         * It seems java.net.Socket supports socket timeouts, but URLConnection does not expose the
         * underlying socket's timeout ability before j2se1.5. So can either rework this code here to use Socket,
         * or leave this commented out till 1.5 is released and more prevalent. Think this latter option is the best
         * one (ie the one that involves least work).
         */
         connection.setConnectTimeout(3000);  //just three seconds, we can't hang around
         if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            System.out.println("Fuckup at urbandictionary, HTTP Response code: " + connection.getResponseCode());
            return null ;
         }
         definitionList = parseUrbanPage(new BufferedReader(new InputStreamReader(connection.getInputStream())));
      } catch (IOException e) {
         e.printStackTrace();
          return null ;
      } finally {
          if(connection!=null) connection.disconnect();
      }
      return definitionList ;
   }   
   
   private Vector parseUrbanPage(BufferedReader br) throws IOException {
      // In which we use java regexps, the painful way
      Vector definitionList = new Vector();
      String inputLine;
      String word;
      String definition;
      String example = "";
      Matcher matcher ;
      
      Pattern wordPattern = Pattern.compile("^\\s*<td class=\"word\">[0-9. ]*(.*)+?</td>\\s*$") ;
      Pattern definitionStartPattern = Pattern.compile("^\\s*<div class=\"definition\">(.*)") ;
      Pattern exampleStartPattern = Pattern.compile("^\\s*<div class=\"example\">(.*)") ;
      Pattern endPattern = Pattern.compile("(.*)</div>\\s*$") ;
      
		while ((inputLine = br.readLine()) != null) {
         // Do stuff...
         matcher = wordPattern.matcher(inputLine) ;
         if (matcher.find()) { // word found 
            // parse out word
            word = matcher.group(1) ;
            // parse out definition
            matcher = definitionStartPattern.matcher(br.readLine()) ;
            while (! matcher.find()) 
               matcher = definitionStartPattern.matcher(br.readLine()) ;
            definition = matcher.group(1) ;
            matcher = endPattern.matcher(definition) ;
            while (! matcher.find()) {
               definition += br.readLine() ;
               matcher = endPattern.matcher(definition) ;
            }
            definition = matcher.group(1) ;
            // and example
            matcher = exampleStartPattern.matcher(br.readLine()) ;
            while (! matcher.find()) 
               matcher = exampleStartPattern.matcher(br.readLine()) ;
            example = matcher.group(1) ; 
            matcher = endPattern.matcher(example) ;
            while (! matcher.find()) {
               example += br.readLine() ;
               matcher = endPattern.matcher(example) ;
            }
            example = matcher.group(1) ;
            // massage our definition into one line of readable ascii
            if (! example.equals(""))
               definition += Message.BOLD + " Ex:" + Message.NORMAL + " \"" + example + "\"" ;
            definition = definition.replaceAll("\\n", " ") ;
            definition = definition.replaceAll("&quot;", "\"");
            definition = definition.replaceAll("&amp;", "&");
            definition = definition.replaceAll("<br />", " ") ;
            definition = definition.replaceAll("<a .*?>", Message.UNDERLINE) ;
            definition = definition.replaceAll("</a>", Message.NORMAL) ;
            // insert new Definition object into definitionList
            definitionList.addElement(new Definition("urban", urbandictionaryDescription, word, definition)) ;
         }
      }
      return definitionList ;
   }
	
	private void dictionaries(Message m) {
		DICTClient dc = getDICTClient(m) ;
		String[][] dbList = dc.getDatabases() ;
		dc.close() ;
		String line = "" ;
		for (int i = 0; i < dbList.length ; i++) {
			if (line.equals(""))
				line = dbList[i][0] ;
			else
				line = line + ", " + dbList[i][0] ;
		}
      line += ", urban, oed" ;
		m.createPagedReply(line).send() ;
	}
	
	private void dictionary(Message m) {
		DICTClient dc = getDICTClient(m) ;
		String code = m.modTrailing.trim() ;
		String line = "" ;
		String[][] dbList = dc.getDatabases() ;
		dc.close() ;
		boolean found = false ;
      if (code.equalsIgnoreCase("oed")) {
         line = "oed: The One True English Dictionary" ;
         found = true ;
      }
      else if (code.equalsIgnoreCase("urban") ) {
         line = "urban:  " + urbandictionaryDescription ;
         found = true ;
      }
      else {
         for (int i = 0; i < dbList.length ; i++ ) {
            if (dbList[i][0].equals(code)) {
               found = true ;
               line = code + ": " + dbList[i][1] ;
            	break ;
   			}
      		if (line.equals(""))
         		line = dbList[i][0] ;
   			else
      			line = line + ", " + dbList[i][0] ;
         	line = line.trim() ;
         }
      }
		if (! found) 
			line = "Dictionary \"" + code + "\" not found; available dictionaries: " + line ;
		m.createPagedReply(line).send() ;
	}

	private DICTClient getDICTClient(Message m) {
		DICTClient dc = null ;
		try {
			dc = new DICTClient(host) ;
		} catch (UnknownHostException e) {
			e.printStackTrace() ;
			m.createReply("Couldn't talk to dict server: host \"" + host + "\" unknown").send() ;
			System.exit(1) ;
		} catch (ConnectException e) {
			e.printStackTrace() ;
			m.createReply("Couldn't talk to dict server.").send() ;
		}
		return dc ;
	}
		
	public static void main(String[] arg) {
		Define define = new Define() ;
		DICTClient dc = define.getDICTClient(new Message("","","","")) ;
		System.out.println("Starting main()") ;
		String[][] dbList = dc.getDatabases() ;
		String line = "" ;
		for (int i = 0 ; i < dbList.length ; i++) {
			line = line + " " + dbList[i][0] ;
		}
		line = line.trim() ;
		System.out.println(line) ;
		dc.close() ;
	}
}
