package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.CommandParser;

import java.util.Random;

public class PlotMaker extends Module {

    private final Random random = new Random();

	@Override
    public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	@Override
	public String[] getCommands() {
		return new String[]{"plot","prot"};
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	private final goat.util.TranslateWrapper translator = new goat.util.TranslateWrapper();
	private final goat.util.PlotMaker$ plotmaker = goat.util.PlotMaker$.MODULE$;

	@Override
	public void processChannelMessage(Message m) {
	    CommandParser cp = new CommandParser(m);
	    String genre = plotmaker.genres().$colon$colon("anime").apply(random.nextInt(plotmaker.genres().length() + 1));
	    if (cp.hasVar("genre")) {
	        genre = cp.get("genre");
	        if (!genre.equals("anime") && !plotmaker.hasGenre(genre)){
	            m.reply("I don't know the genre \"" + genre +
	                    "\";  my genres are:  anime, " + plotmaker.genresAsString());
	            return;
	        }
	    } else if("prot".equalsIgnoreCase(m.getModCommand()))
	        genre = "kungfu"; // default to kungfu if no genre supplied for racist-plot?

        String reply = "";
        if (genre.equals("anime"))
            reply = plotmaker.plot("wondermark");
        else
            reply = plotmaker.plot(genre);

		String arg = cp.remaining();
		if (arg != null && !arg.equals(""))
            reply += "  " + Constants.BOLD + "Your Title:" + Constants.NORMAL + "  \"" + arg + "\"";
		else if(genre.equals("anime"))
		    reply += "  " + Constants.BOLD + "Cartoon Title:" + Constants.NORMAL + "  \"" + plotmaker.title("wondermark") + "\"";
		else
            reply += "  " + Constants.BOLD + "Working Title:" + Constants.NORMAL + "  \"" + plotmaker.title(genre) + "\"";

		if(genre.equals("anime"))
		    reply = babbelize(reply);

        if("prot".equalsIgnoreCase(m.getModCommand()))
            reply = addExtraRacism(reply);

		m.reply(reply);
	}

	private String addExtraRacism(String str) {
	    int i = random.nextInt(100);
	    if (i < 40) {
	        str = babbelize(str);
	    } else if(i < 80) {
	        str = disemEl(str);
	    } else {
	        str = disemEl(babbelize(str));
	    }
	    return str;
	}

	private String disemEl(String str) {
	    return str.replaceAll("[Ll][Ll]?", "r");
	}

	private final String[] langs = {"japanese", "korean", "chinese_traditional",
	        "chinese_simplified", "hmong_daw", "thai", "vietnamese"};

	private String babbelize(String str) {
	    String lang = langs[random.nextInt(langs.length)];
	    try {
	        // protect the title header from the transloop
            str = str.replaceAll("Working Title", "1001002");
            str = str.replaceAll("Your Title", "1001003");
            str = str.replaceAll("Cartoon Title", "1001004");

            str = translator.transloop(str, lang);

            // put the header back
            str = str.replaceAll("\\s*1001002[^\\w\"]*", "  " + Constants.BOLD + "Working Title:" + Constants.NORMAL + "  ");
            str = str.replaceAll("\\s*1001003[^\\w\"]*", "  " + Constants.BOLD + "Your Title:" + Constants.NORMAL + "  ");
            str = str.replaceAll("\\s*1001004[^\\w\"]*", "  " + Constants.BOLD + "Cartoon Title:" + Constants.NORMAL + "  ");
        } catch (Exception e) {
            // we still have a string in str, it just hasn't been translooped
            e.printStackTrace();
        }
	    return str;
	}
}
