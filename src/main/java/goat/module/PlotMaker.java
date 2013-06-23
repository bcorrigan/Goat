package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;
import goat.util.CommandParser;

public class PlotMaker extends Module {

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
	private final goat.util.PlotMaker plotmaker = new goat.util.PlotMaker();

	@Override
	public void processChannelMessage(Message m) {
	    CommandParser cp = new CommandParser(m);
	    String genre = plotmaker.randomGenre();
	    if (cp.hasVar("genre"))
	        if (! plotmaker.hasGenre(cp.get("genre")))
	            if(cp.get("genre").equalsIgnoreCase("anime"))
	                genre = "wondermark";
	            else {
	                m.reply("I don't know the genre \"" + cp.get("genre") + "\";  my genres are:  anime, " + plotmaker.genresAsString());
	                return;
	            }
	        else
	            genre = cp.get("genre");
	    else if("prot".equalsIgnoreCase(m.getModCommand()))
	        genre = "kungfu"; // default to kungfu if no genre supplied for racist-plot?

        String reply = plotmaker.plot(genre);

        if("prot".equalsIgnoreCase(m.getModCommand()) || (cp.hasVar("genre") && cp.get("genre").equals("anime")))
            try {
                reply = translator.transloop(reply, "japanese");
            } catch (Exception e) {
                // we still have a plot in reply, it just hasn't been translooped
                e.printStackTrace();
            }

		String arg = cp.remaining();
		if (arg == null || arg.equals(""))
		    reply += Constants.BOLD + "  Working Title:  " + Constants.NORMAL + "\"" + plotmaker.title(genre) + "\"";
	    else
			reply += Constants.BOLD + "  Your Title:  " + Constants.NORMAL + "\"" + arg + "\"";

		m.reply(reply);
	}
}
