package goat.module;

import goat.core.Constants;
import goat.core.Module;
import goat.core.BotStats;
import goat.util.StringUtil;

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
	"atheist",
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

    public static final String[] emisms = {
	"zeiss",
	"focal plane",
	"vignetting",
	"shadow detail",
	"san mateo",
	"culantro",
	"f1.2",
	"f1.4",
	"sencha",
	"matcha",
	"sofrito",
	"pink beans",
	//"friday",
	"rebecca black",
	"which seat",
	//joeyisms that em has been whinging about particularly
	// "assdroid",	// see next
	// "sonic",		// christ, joey, give it a rest already
	"lady gaga",
	"katy perry",
	"emily autumn"
    };




    // ideally, these should be in order of increasing severity, and not look
    //   odd when prefixed with "[uname], " or appended with ", uname"
    //   periods will be added if you don't supply one, and question marks
    //   will be moved to the end if the uname is appended to the question.
    //
    public static final String[] responses = {
	"shut up",
	"yadda yadda yadda",
	"la la la la NOT LISTENING",
	"pipe down",
	"blag blag blag",
	"I'm not your bartender",
	"let it rest",
	"give us a little peace and quiet",
	"have you had too much coffee?",
	"go bother your mother",
	"god, you're boring",
	"maybe you should go outside and get some fresh air",
	"isn't it a lovely day?  To go outside, away from the computer?",
	"go cry about it on your livejournal",
	"do you ever get the feeling that no-one listens to you?",
	"don't your hands start to hurt when you do that?",
	"you do realize there are other people trying to use this channel, don't you?",
	"leave us alone and put it on your goddamned blog",
	"has anyone ever walked away from you while you were in the middle of saying something?",
	"fascinating.  Really",
	"nobody is listening to your stoned ramblings",
	"Look! Over there! Away from the computer",
	"There is nothing so pathetic as a bore who claims attention - luckily you're not getting it",
	"Rather than drowning us in a sea of words, perhaps you should drown yourself in the bath",
	"Don't you have carpal tunnel yet?",
	"enough already",
	"have you considered not typing anymore?",
	"have you considered therapy?",
	"have you considered suicide?",
	"shut your pie-hole",
	"you must be feeling lonely",
	"tell it to someone who cares",
	"I don't want to hear about it",
	"would you mind shutting the fuck up?",
	"shut the fuck up",
	"fucking shut up",
	"do you think you could go fuck off for a little while?",
	"eat a bag of dicks",
	"give it a fucking rest",
	"please shut the fuck up",
	"you're being fucking boring",
	"you're fucking boring us.  " + Constants.BOLD + "Again" + Constants.NORMAL,
	"I think you've forgotten to take your fucking medicine",
	"get your greasy little fingers away from the fucking computer",
	"have you ever wondered why your keyboards keep wearing out?",
	"who let the bore in? Zzz. Zzz. Zzz zzz",
	"You've succeeded in making a bot suicidal with your fucking tedious rambling - well done",
	"I'd rather watch paint dry than listen to anymore of your shit",
	"Look. Just FUCK OFF",
	"your lonliness is making me uncomfortable",
	"does everyone ignore you like this at work, too?",
	"does someone need a hug?",
	"does the narcissism feel good today?",
	"I think you left the oven on",
	"let's have a you party, for you, to celebrate yourself",
	"oh look, it's raining words, have you seen my slumberella?",
	"LGAGAGHLAGGAH HLGHGHGLGHLGH GAHLAGHAG LHGAGLG MY JESUS GOD THIS IS NOT HEAVEN AT ALL",
	"HAW HAWAHWH AWIH LAFIALFDH AILFSH DICKS"
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
	"you're " + Constants.BOLD + "really" + Constants.NORMAL + " getting on my nerves",
	"you're being a little compulsive",
	"you must get sexually aroused, pushing me around like this"
    } ;

    private Random random = new Random() ;

    private String blatherer = "";     // who is blathering?
    private int blatherCount = 0;      // how long have they been blathering?
    private int blatherThreshold = 7 ; // how much of this blather will we put up with?
    private int kipismCount = 0;       // and is it that bastard kip again?

    private int commandCount = 0;      // has someone been bossing goat around?
    private int commandThreshold = blatherThreshold + 5;  // and how much will goat tolerate?

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
	int joeyscore = score(m.getTrailing(), joeyisms) ;
	if (joeyscore > 0) {
	    // debug
	    // System.out.println("JOEYISM detected from umask: " + m.prefix) ;
	    if(m.getPrefix().matches(".*\\.pacbell\\.net.*")) {
		// for now, we'll only respond to joey himself occasionally,
		//  since he seems to be getting off on this.
		if (random.nextInt(100) < (30 + 5*joeyscore) )
		    randomReply(responses, m) ;
		// debug
		//System.out.println("JOEY detected!") ;
	    } else {
		m.reply("Shut up, joey.") ;
	    }
	}

	int emscore = score(m.getTrailing(), emisms);
	if (emscore > 0) {
	    m.reply("Shut up, em.");
	}

	// blather monitor housekeeping
	if (blatherer.equalsIgnoreCase(m.getSender())) {
	    kipismCount += score(m.getTrailing(), kipisms) ;
	} else {
	    blatherer = m.getSender() ;
	    blatherCount = 0;
	    commandCount = 0;
	    kipismCount = 0;
	}
	if(BotStats.getInstance().isLoadedCommand(m.getModCommand()))
	    commandCount++;
	else
	    blatherCount++;
	// blatherer shutting-up
	if (blatherCount >= blatherThreshold) {
	    // feel free to tweak the formula below
	    if (random.nextInt(100) < 10 + (blatherCount - blatherThreshold)*5 + kipismCount*20 + commandCount*5)
		randomReply(responses, m);
	    // commands-abuser whinging
	} else if(commandCount >= commandThreshold) {
	    if (random.nextInt(100) < 20 + (commandCount - commandThreshold)*5)
		randomReply(overworkWhinges, m) ;
	}


	if(m.getTrailing().toLowerCase().matches(".*i miss (joey|bc)\\.?\\s*"))
	    m.reply("We all do.");
	else if(m.getTrailing().toLowerCase().matches(".*i miss joey.*"))
	    m.reply("We all miss joey.");
	else if(m.getTrailing().toLowerCase().matches(".*i miss bc.*"))
	    m.reply("We all miss bc.");


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



    public void randomReply(String[] replies, Message m) {
	String reply = StringUtil.pickRandom(replies).trim() ;
	boolean isQuestion = false;
	if (reply.substring(reply.length() - 1).equals("?")) {
	    reply = reply.substring(0, reply.length() - 1);
	    isQuestion = true;
	}
	if (reply.substring(reply.length() - 1).equals(".")) {
	    reply = reply.substring(0, reply.length() - 1);
	}
	int rand = random.nextInt(100) ;
	if (rand < 60)
	    reply = m.getSender() + ", " + reply;
	else if(rand < 80)
	    reply = m.getSender() + ": " + reply;
	else if(rand < 90)
	    reply = StringUtil.capitalise(reply) + ", " + m.getSender();
	else
	    reply = StringUtil.capitalise(reply);
	if(isQuestion)
	    reply += "?";
	else
	    reply += ".";
	m.reply(reply);
    }



    public String[] getCommands() { return new String[0]; }
}
