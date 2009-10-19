package goat.module;

import goat.Goat;
import goat.core.Constants;
import goat.core.IrcUser;
import goat.core.Message;
import goat.core.Module;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerCommands extends Module {

	ExecutorService pool; 
	
	public int messageType() {
		return WANT_ALL_MESSAGES;
	}
	
	public String[] getCommands() {
		return new String[]{"servercommand"};
	}
	
	@Override
	public void processChannelMessage(Message m) {
		if("servercommand".equalsIgnoreCase(m.getModCommand()))
			execute(m, m.getModTrailing());
		
	
	}

	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}
	
	public void processOtherMessage(Message m) {
		if(m.getCommand().matches("\\d+"))  { // numeric command response {
			int command = Integer.parseInt(m.getCommand());
			if(Constants.RPL_WHOREPLY == command) {
				whoReplies.add(m);
			} else if(Constants.RPL_ENDOFWHO == command) {
				buildingWhoReply = false;
			}
		}
	}

	private static boolean buildingWhoReply = false;
	private static List<Message> whoReplies = Collections.synchronizedList(new ArrayList<Message>());
	private long whoWaitInterval = 20; //short
	private long whoMaxWait = 3000; // 3 seconds, no waiting around
	private Object lockWHO = new Object();
	
	public List<IrcUser> who(String channel) throws SocketTimeoutException {
		List<Message> temp;
		synchronized (lockWHO) {
			buildingWhoReply = true;
			new Message("","WHO",channel,"");
			int i = 0;
			while((true == buildingWhoReply) && (i*whoWaitInterval < whoMaxWait)) {
				try {
					Thread.sleep(whoWaitInterval);
				} catch (InterruptedException ie) {}
				i++;
			}
			if (i*whoWaitInterval >= whoMaxWait) {
				whoReplies = Collections.synchronizedList(new ArrayList<Message>());
				buildingWhoReply = false;
				throw new SocketTimeoutException();
			}
			temp = whoReplies;
			whoReplies = Collections.synchronizedList(new ArrayList<Message>());
			buildingWhoReply = false;
		}	
		List<IrcUser> ret = new ArrayList<IrcUser>();
		Iterator<Message> it = temp.iterator();
		while(it.hasNext())
			ret.add(IrcUser.getNewInstanceFromWHOReply(it.next()));
		return ret;
	}
	
	private void execute(Message m, String command) {
		if (null == pool)
			pool = Goat.modController.getPool();
		if (null == pool) 
			pool = Executors.newCachedThreadPool();
		pool.execute(new CommandExecutor(m, command));
	}
	
	private class CommandExecutor implements Runnable {
		Message m;
		String command;
		public CommandExecutor(Message message, String serverCommand) {
			m = message;
			command = serverCommand.trim();	
		}
		public void run() {
			if("WHO".equals(command)) {
				try {
					List<IrcUser> ircUsers = who(m.getChanname());
					String reply = "";
					for(IrcUser iu: ircUsers) {
						reply += iu.getNick() + " ";
					}
					m.reply(reply);
				} catch (SocketTimeoutException ste) {
					m.reply("Timed out waiting for WHO response");
				}
			} else {
				m.reply("I don't know that command.");
			}
		}
	}
	
}
