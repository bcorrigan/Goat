package goat.module;

import goat.core.Message;
import goat.core.Module;
import java.util.Random;



/**
 * Module to tell people to shut up.
 * 
 * @author rs
 *
 */
public class ShutUp extends Module {

	public static final String[] joeyisms = {
			"dogs are fat",	
			"proliferate", 
			"proliferating", 
			"proliferation", 
			"Erica Campbell", 
			"huge boobs", 
			"regina", 
			"katie", 
			"marge", 
			"nerd virgin", 
			"nergin", 
			"true pimp", 
			"melons", 
			"power up", 
			"saiyan", 
			"vegeta ",
			"freethinking atheists", 
			"totally gay" } ;
	
	public static final String[] kipisms = {
			"bliar",
			"wogs",
			"labour",
            "muslims",
            "mooslims",
            "darkies",
            "islam",
            "immigrants",
            "poles",
            "nulab",
            "prescott",
            "brown",
            "tax",
            "straw",
            "reid",
            "market",
            "mortgage",
            "gambling",
            "gambles",
            "money",
            "conservatives",
            "tories",
            "cameron",
            "clarke",
            "pounds",
            "dollars",
            "scum",
            "chav",
            "yob",
            "asbo"
	} ;
	
	// ideally, these should be in order of increasing severity, and not look
	//   odd when prefixed with "[uname], " or appended with ", uname." or "."
	public static final String[] responses = {
			"shut up",
			"shut the fuck up",
			"fucking shut up",
			"fuck off.  Now",
			"give it a fucking rest",
			"please shut the fuck up",
			"you're being fucking boring",
			"you're fucking boring us.  " + Message.BOLD + "Again" + Message.NORMAL,
			"I think you've forgotten to take your fucking medicine", 
			"get your greasy little fingers away from the fucking computer",
	} ;

	// see caveats for responses[] above; they apply to this, too
	public static final String[] overworkWhinges = {
		"my feet hurt",
		"you're making me work too hard",
		"give me a break",
		"stop bothering me",
		"stop it.  STOP IT",
		"please let someone else have a turn",
		"the other people in this channel are starting to hate you",
		"what a dull life you must lead",
		"I'm starting to hate you",
		"don't be such a goddamned ballbuster",
		"you're getting on my nerves",
		"you're " + Message.BOLD + "really" + Message.NORMAL + " getting on my nerves",
		"you're being a little compulsive",
		"you must get sexually aroused, pushing me around like this"
	} ;
	
	private Random random = new Random() ;
	
	private String blatherer = "";     // who is blathering?
	private int blatherCount = 0;      // how long have they been blathering?
	private int blatherThreshold = 4 ; // how much of this blather will we put up with?
	private int kipismCount = 0;       // and is it that bastard kip again?
	
	private int commandCount = 0;                        // has someone been bossing goat around?		
	private int commandThreshold = 2*blatherThreshold ;  // and how much will goat tolerate?
	
	public ShutUp() {
	}
	
	public int messageType() {
		return WANT_ALL_MESSAGES ;
	}
	
	@Override
	public void processPrivateMessage(Message m) {
		// do nothing here, people can yammer at goat in private to their heart's content.
	}
	
	@Override
	public void processChannelMessage(Message m) {

		// per-line joey shutting-up
		int joeyscore = score(m.trailing, joeyisms) ;
		if (joeyscore > 0) {
			// debug
			// System.out.println("JOEYISM detected from umask: " + m.prefix) ;
			if(m.prefix.matches(".*\\.pacbell\\.net.*")) {
				// for now, we'll only respond to joey himself occasionally, 
				//  since he seems to be getting off on this.
				if (random.nextInt(100) < (30 + 5*joeyscore) )
					randomReply(responses, m) ;
				// debug
				//System.out.println("JOEY detected!") ;
			} else {
				m.createReply("Shut up, joey.").send() ;
			}
		}
		
		// blather monitor housekeeping
		if (blatherer.equalsIgnoreCase(m.sender)) {
			kipismCount += score(m.trailing, kipisms) ;
		} else {
			blatherer = m.sender ;
			blatherCount = 0;
			commandCount = 0;
			kipismCount = 0;
		}
		if(goat.Goat.modController.isLoadedCommand(m.modCommand)) 
			commandCount++;
		else
			blatherCount++;
		// blatherer shutting-up
		if (blatherCount >= blatherThreshold) {
			// feel free to tweak the formula below
			if (random.nextInt(100) < 20 + (blatherCount - blatherThreshold)*10 + kipismCount*20 + commandCount*5)
				randomReply(responses, m);
		// commands-abuser whinging	
		} else if(commandCount >= commandThreshold) {
			if (random.nextInt(100) < 20 + (commandCount - commandThreshold)*5)
				randomReply(overworkWhinges, m) ;
		}
		//debug
		//System.out.println("blatherer: " + blatherer + "  count: " + blatherCount + "  commands: " + commandCount + "  kipisms: " + kipismCount) ;
	}

	private int score(String target, String[] triggers) {
		int ret = 0;
        for (String trigger : triggers) {
            if (target.toLowerCase().contains(trigger.toLowerCase()))
                ret++;
        }
        return ret;
	}

	private String pickRandom(String[] strings) {
		return strings[random.nextInt(strings.length)] ;
	}
	
	public void randomReply(String[] replies, Message m) {
		String reply = pickRandom(replies) ;
		int rand = random.nextInt(100) ;
		if (rand < 60) 
			reply = m.sender + ", " + reply + "." ;
		else if(rand < 80)
			reply = m.sender + ": " + reply + "." ;
		else if(rand < 90)
			reply = capitalise(reply) + ", " + m.sender + "." ;
		else 
			reply = capitalise(reply) + "." ;
		m.createReply(reply).send();
	}
	
	public String capitalise(String in) {
		if(in.length() < 1)
			return in;
		else if (1 == in.length())
			return in.toUpperCase() ;
		else
			return in.substring(0, 1).toUpperCase() + in.substring(1) ;
	}
}
