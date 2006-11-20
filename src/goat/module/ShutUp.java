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
	//   odd when prefixed with "[uname], "
	public static final String[] responses = {
			"shut up.",
			"shut the fuck up.",
			"fucking shut up.",
			"fuck off.  Now.",
			"give it a fucking rest.",
			"please shut the fuck up.",
			"you're being fucking boring.",
			"you're fucking boring us.  " + Message.BOLD + "Again.",
			"have you forgotten to take your fucking medicine?", 
			"get your greasy little fingers away from the fucking computer.",
			} ;

	private Random random = new Random() ;
	
	private String blatherer = "";     // who is blathering?
	private int blatherCount = 0;      // how long have they been blathering?
	private int blatherThreshold = 4 ; // how much of this blather will we put up with?
	private int kipismCount = 0;       // and is it that bastard kip again?
	
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
					m.createReply("joey, " + pickRandom(responses)).send() ;
				// debug
				//System.out.println("JOEY detected!") ;
			} else {
				m.createReply("Shut up, joey.").send() ;
			}
		}
		
		// blather monitor housekeeping
		if (blatherer.equalsIgnoreCase(m.sender)) {
			kipismCount += score(m.trailing, kipisms) ;
			blatherCount++;
		} else {
			blatherer = m.sender ;
			blatherCount = 1;
			kipismCount = 0;
		}		
		// blatherer shutting-up
		if (blatherCount >= blatherThreshold) {
			// feel free to tweak the formula below
			if (random.nextInt(100) < 20 + (blatherCount - blatherThreshold)*15 + kipismCount*30)
				m.createReply(m.sender + ", " + pickRandom(responses)).send();
		}
	}

	private int score(String target, String[] triggers) {
		int ret = 0;
		for(int i=0;i<triggers.length;i++) {
			if (target.toLowerCase().contains(triggers[i].toLowerCase()))
				ret++ ;
		}
		return ret;
	}

	private String pickRandom(String[] strings) {
		return strings[random.nextInt(strings.length)] ;
	}
}
