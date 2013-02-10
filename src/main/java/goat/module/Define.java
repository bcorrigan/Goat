package goat.module ;

import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;
import goat.util.DICTClient;
import goat.util.Definition;
import goat.util.CommandParser;
import goat.module.WordGame;

import java.io.*;
import java.net.* ;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.* ;

import org.json.JSONArray;
import org.json.JSONObject;


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

   public String[] getCommands() {
		return new String[] { "define", "randef", "dictionaries", "dictionary", "oed", "thesaurus"};
   }

	public Define() {
	}


	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

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
			return ;
		}
		// check num not greater than number of elements in list
		if (num > definitionList.size() ) {
			String line = "I don't have " + num + " definitions for \"" + word ;
			if (! dictionary.equals("*") )
				line = line + "\" in dictionary \"" + dictionary + "\"." ;
			else
				line = line + "\"." ;
			m.reply(line) ;
			return ;
		}

		Definition d = (Definition) definitionList.get(num - 1) ;
		text = d.getWord() + " (" + d.getDatabaseShort() + "): " + d.getDefinition() ;
		m.pagedReply(text) ;
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
			m.reply(msg) ;
		}
	}

	private void randef(Message m) {
		m.reply("Not implmemented, please stand by") ;
	}

	private String oedUrl(String word) {
		return "http://dictionary.oed.com/cgi/findword?query_type=word&queryword=" + word.replaceAll(" ", "%20") ;
	}

	private String urbanUrl(String word) {
		return "http://www.urbandictionary.com/define.php?term=" + word.replaceAll(" ", "%20") ;
	}

	public Vector<Definition> getUrbanDefinitions(String word) throws SocketTimeoutException {
      Vector<Definition> definitionList = null;
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
			System.err.println("Connection to urbandictionary timed out.") ;
			throw new SocketTimeoutException(e.toString()) ;
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

	public Vector<Definition> getUrbanDefinitions(String word, Message m) {
		try {
			return getUrbanDefinitions(word) ;
		} catch (SocketTimeoutException e) {
			m.reply("Connection to urbandictionary timed out.") ;
		}
		return null;
	}

   private Vector<Definition> parseUrbanPage(BufferedReader br) throws IOException {
		// In which we use java regexps, the painful way
		Vector<Definition> definitionList = new Vector<Definition>();
		String inputLine;
		String word;
		String definition = "";
		String example = "";
		int defNumber = -1;
		Matcher matcher;

		Pattern numberStartPattern = Pattern.compile("^\\s*<td class='index'>\\s*$");
		Pattern numberBodyPattern = Pattern.compile("\\s*<a href=\"http:\\/\\/[a-z0-9-]+\\.urbanup\\.com\\/\\d+\">(\\d+)\\.<\\/a>\\s*");

//		Pattern numberEndPattern = Pattern.compile("^\\s*</td>\\s*$");
		Pattern wordStartPattern = Pattern.compile("^\\s*<td class='word'>\\s*$");
//		Pattern wordEndPattern = numberEndPattern;
//		Pattern def_pStartPattern = Pattern.compile("^\\s*<div class=\"def_p\">(.*)\\s*$");
//		Pattern def_pBodyStartPattern = Pattern.compile("^\\s*<p>(.+?)(</p>)*\\s*$");
//		Pattern def_pBodyEndPattern = Pattern.compile("(.*)</p>\\s*$") ;

		Pattern definitionStartPattern = Pattern.compile("^\\s*<div class=\"definition\">\\s*(.*)\\s*$");
		Pattern definitionEndPattern = Pattern.compile("^\\s*(.*?)\\s*</div>.*");

		Pattern exampleStartPattern = Pattern.compile("<div class=\"example\">\\s*(.*)\\s*$");
		Pattern exampleEndPattern = Pattern.compile("^\\s*(.*?)\\s*</div>\\s*$") ;

		Pattern startPattern = numberStartPattern;
		//Pattern endPattern = Pattern.compile("^\\s*</div>\\s*$");

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
				matcher = numberBodyPattern.matcher(br.readLine());
				matcher.find();
				defNumber = Integer.parseInt(matcher.group(1));
				// skip detection of numberEndPattern, not needed
				if(debug)
					System.out.println("  definition #" + defNumber) ;
				// parse out word
				matcher = wordStartPattern.matcher(br.readLine());
				while (!matcher.find())
					matcher = wordStartPattern.matcher(br.readLine());
				word = br.readLine().trim(); // word or phrase should be alone on a line, no parse necessary
				if(debug)
					System.out.println("  word found: " + word) ;
				// Definition and example have moved, now can be on same line.
				// or not.  Go fuck yourself, urbandictionary.
				String dline = "";
				// parse out definition
				definition = "";
				dline = br.readLine();
            matcher = definitionStartPattern.matcher(dline);
            while(!matcher.find()) {
					dline = br.readLine();
               matcher = definitionStartPattern.matcher(dline);
				}
            definition = matcher.group(1);
            matcher = definitionEndPattern.matcher(definition);
            while(!matcher.find()) {
					dline = br.readLine();
               definition += " " + dline;
               matcher = definitionEndPattern.matcher(definition);
            }
            definition = matcher.group(1);
            // parse out example
            example = "";
            matcher = exampleStartPattern.matcher(dline);
            while(!matcher.find()) {
					dline = br.readLine();
               matcher = exampleStartPattern.matcher(dline);
				}
            example = matcher.group(1);
            matcher = exampleEndPattern.matcher(example);
            while(!matcher.find()) {
					dline = br.readLine();
               example += " " + dline;
               matcher = exampleEndPattern.matcher(example);
            }
            example = matcher.group(1);

/* old site design made this hideous, rewriting from scratch

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
*/

				// massage our definition into one line of readable ascii
				if (!example.equals(""))
					definition += Constants.BOLD + " Ex:" + Constants.NORMAL
							+ " \"" + example + "\"";
				definition = definition.replaceAll("\\n", " ");
				definition = definition.replaceAll("&quot;", "\"");
				definition = definition.replaceAll("&amp;", "&");
				definition = definition.replaceAll("<br\\s*/>", "  ");
				definition = definition
						.replaceAll("<a .*?>", Constants.UNDERLINE);
				definition = definition.replaceAll("</a>", Constants.NORMAL);
				// insert new Definition object into definitionList
				definitionList.addElement(new Definition("urban",
						urbandictionaryDescription, word, definition));
			}
		}
		return definitionList;
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
		ret += "urban, oed, trends" ;
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
