package goat.module ;

import goat.Goat;
import goat.core.Module;
import goat.core.Message;
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
	public boolean debug = false ;
   
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public static String[] getCommands() {
		return new String[] { "define", "randef", "dictionaries", "dictionary", "oed" };
   }
	
	public Define() {
	}

	
	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		//parse out args

		if(debug)
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
         definitionList = getUrbanDefinitions(word, m) ;
         // this next is ugly, but urbandictionary.com doesn't have a guess-mis-spelt-word feature
         if ((null == definitionList) || definitionList.isEmpty())
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
                   for (String[] aDbList : dbList) {
                       if (dictionary.equals(aDbList[0])) {
                           found = true;
                           break;
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
                for (String[] aMatchList : matchList) suggestions += " " + aMatchList[1];
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
        
	public Vector getUrbanDefinitions(String word, Message m) {
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
         if(debug)
        	 System.out.println("Page retrieved from urbandictionary for word \"" + word + "\"") ;
         definitionList = parseUrbanPage(new BufferedReader(new InputStreamReader(connection.getInputStream())));
		} catch (SocketTimeoutException e) {
			m.createReply("Connection to urbandictionary timed out.").send() ;
			System.err.println("Connection to urbandictionary timed out.") ;
			return null ;
      } catch (IOException e) {
    	  if(debug)
    		  System.err.println("IOException caught.") ;
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
		int defNumber = -1;
		Matcher matcher;

		Pattern def_numberPattern = Pattern.compile("^\\s*<td class=\"def_number\" width=\"20\">([0-9]+)\\.</td>\\s*$");
		Pattern def_wordPattern = Pattern.compile("^\\s*<td class=\"def_word\">(.+)</td>\\s*$");
		Pattern def_pStartPattern = Pattern.compile("^\\s*<div class=\"def_p\">(.*)\\s*$");
		Pattern def_pBodyStartPattern = Pattern.compile("^\\s*<p>(.+?)(</p>)*\\s*$");
		Pattern def_pBodyEndPattern = Pattern.compile("(.*)</p>\\s*$") ;
		Pattern exampleStartPattern = Pattern.compile("^\\s*<p style=\"font-style: italic\">(.*)(<br />\\s*|</p>.*)$");
		Pattern exampleEndPattern = Pattern.compile("(.*)</p>.*$") ;
		
		Pattern startPattern = def_numberPattern;
		Pattern endPattern = Pattern.compile("^\\s*</div>\\s*$");

		// this has grown and grown, and now it is completely ludicrous.
		while ((inputLine = br.readLine()) != null) {
			// Do stuff...
			matcher = startPattern.matcher(inputLine);
			if (!matcher.find()) {
				continue ;
			} else { // word found
				if(debug)
					System.out.println("Start of definition block located.") ;
				// parse out word number ;
				defNumber = Integer.parseInt(matcher.group(1));
				if(debug)
					System.out.println("  definition #" + defNumber) ;
				// parse out word
				matcher = def_wordPattern.matcher(br.readLine());
				while (!matcher.find())
					matcher = def_wordPattern.matcher(br.readLine());
				word = matcher.group(1);
				if(debug)
					System.out.println("  word found: " + word) ;
				// parse out definition
				String tempLine = br.readLine() ;
				matcher = def_pStartPattern.matcher(tempLine);
				while (!matcher.find()) {
					tempLine = br.readLine() ;
					matcher = def_pStartPattern.matcher(tempLine);
				}
				matcher = def_pBodyStartPattern.matcher(tempLine);
				while(!matcher.find()) {
					tempLine = br.readLine() ;
					matcher = def_pBodyStartPattern.matcher(tempLine);
				}
				definition = matcher.group(1) ;
				matcher = def_pBodyEndPattern.matcher(tempLine) ;
				boolean multiline = false ;
				while(!matcher.find()) {
					if(multiline)
						definition += tempLine ;
					tempLine = br.readLine() ;
					matcher = def_pBodyEndPattern.matcher(tempLine) ;
					multiline = true ;
				}
				if(multiline)
					definition += matcher.group(1);
				if(debug)
					System.out.println("  raw definition: " + definition);
				
				// and example. 
				// this is ugly, as we don't want to run through to the next
				// definition's examples, or EOF, if there are no examples for this def.
				// 
				
				matcher = exampleStartPattern.matcher(tempLine) ;
				Matcher endMatcher = endPattern.matcher(tempLine) ;
				boolean definitionDone = false ;
				while (true) {
					if (matcher.find()) {
						break ;
					}
					if (endMatcher.find()) {
						definitionDone = true ;
						break ;
					}
					tempLine = br.readLine() ;
					matcher = exampleStartPattern.matcher(tempLine) ;
					endMatcher = endPattern.matcher(tempLine) ;
				}
				
				if (!definitionDone) {					
					example = matcher.group(1);				
					matcher = exampleEndPattern.matcher(tempLine) ;
					multiline = false ;
					while(!matcher.find()) {
						if (multiline)
							example += tempLine ;
						tempLine = br.readLine() ;
						matcher = exampleEndPattern.matcher(tempLine) ;
						multiline = true ;
					}
					if(multiline)
						example += matcher.group(1);
					if(debug)
						System.out.println("  raw example: " + example);
				}

				// massage our definition into one line of readable ascii
				if (!example.equals(""))
					definition += Message.BOLD + " Ex:" + Message.NORMAL
							+ " \"" + example + "\"";
				definition = definition.replaceAll("\\n", " ");
				definition = definition.replaceAll("&quot;", "\"");
				definition = definition.replaceAll("&amp;", "&");
				definition = definition.replaceAll("<br />", " ");
				definition = definition
						.replaceAll("<a .*?>", Message.UNDERLINE);
				definition = definition.replaceAll("</a>", Message.NORMAL);
				// insert new Definition object into definitionList
				definitionList.addElement(new Definition("urban",
						urbandictionaryDescription, word, definition));
			}
		}
		return definitionList;
	}
	
	private void dictionaries(Message m) {
		DICTClient dc = getDICTClient(m) ;
		String[][] dbList = dc.getDatabases() ;
		dc.close() ;
		String line = "" ;
        for (String[] aDbList : dbList) {
            if (line.equals(""))
                line = aDbList[0];
            else
                line = line + ", " + aDbList[0];
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
        for (String[] aDbList : dbList) {
            line = line + " " + aDbList[0];
        }
        line = line.trim() ;
		System.out.println(line) ;
		dc.close() ;
	}
}
