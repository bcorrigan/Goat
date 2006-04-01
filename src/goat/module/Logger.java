package goat.module;

import goat.core.Message;
import goat.core.Module;
import goat.util.IRCLogger;

public class Logger extends Module {

	public IRCLogger logger = new IRCLogger() ;
	
	public int messageType() {
		return Module.WANT_ALL_MESSAGES ;
	}
	

	public void processPrivateMessage(Message m) {
		// TODO Auto-generated method stub
		// Do nothing; we won't log private messages.  It could make for some
		// lovely mayhem, but it could also result in goat burping up his
		// own password, if we're not careful.
	}


	public void processChannelMessage(Message m) {
		int id = -1;
		try {

			// TODO "slashnet" should not be hard-coded here.
			id = logger.logIncomingMessage(m, "slashnet");

			// uncomment the next line if you want to have all your loggings
			// dumped to your console

			logger.printResultSet(logger.getMessage(id));
		} catch (Exception e) {
			System.err.println("ERROR -- Problem logging message");
			e.printStackTrace();
		}
	}

	public void processOtherMessage(Message m) {
		processChannelMessage(m) ;
	}
}