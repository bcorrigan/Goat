package goat.module;

import goat.core.Constants;
import goat.core.Message;
import goat.core.Module;

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

    private final goat.util.PlotMaker plotmaker = new goat.util.PlotMaker();

	@Override
	public void processChannelMessage(Message m) {
	    String genre = "wondermark";
	    if("prot".equals(m.getModCommand().toLowerCase()))
	        genre = "kungfu";

        String reply = plotmaker.plot(genre);

		String arg = m.getModTrailing().trim();
		if (arg == null || arg.equals(""))
		    reply += Constants.BOLD + "  Working Title:  " + Constants.NORMAL + "\"" + plotmaker.title(genre) + "\"";
	    else
			reply += Constants.BOLD + "  Your Title:  " + Constants.NORMAL + "\"" + arg + "\"";

		m.reply(reply);
	}
}
