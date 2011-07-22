package goat.module;

import java.util.Properties;

import goat.core.Message;
import goat.core.Module;
import goat.util.StringUtil;
import goat.Goat;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;

public class Guru extends Module {
	
	private static final String GOOGLE_TALK_HOST = "talk.google.com";
	private static final int GOOGLE_TALK_PORT = 5222;
	private static final String GOOGLE_TALK_SERVICE = "gmail.com";
	private static final String GURU_ADDRESS = "guru@googlelabs.com";
	private String username = "";
	private String password = "";
	
	private XMPPConnection connection;
	private ChatManager chatmanager;
	private MessageListener responseHandler = new GuruResponseHandler();
	
	public Guru() {
		try {
			connect();
		} catch (XMPPException xe) {
			xe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String[] getCommands() {
		return new String[]{"guru", "gu", "googlebot", "gbot", "gb", "goobot", "gubot"};
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}

	public void processChannelMessage(Message m) {
		if(connection == null || (! connection.isConnected())) {
			try {
				connect();
			} catch (Exception e) {
				m.reply("I can't connect to the guru right now.");
				return;
			}
		}
		String query = m.getModTrailing();
		query = StringUtil.removeFormattingAndColors(query);
		String thread = m.getReplyTo(); 
		org.jivesoftware.smack.packet.Message guMess = new org.jivesoftware.smack.packet.Message();
		guMess.setBody(query);
		try {
			Chat chat = chatmanager.getThreadChat(thread);
			if (chat == null) {
				// System.out.println("no chat found for channel, creating: " + thread);
				chat = chatmanager.createChat(GURU_ADDRESS, thread, responseHandler);
			} 
			chat.sendMessage(guMess);
		} catch (XMPPException xe) {
			m.reply("I couldn't send your query to the guru.");
			xe.printStackTrace();
		}
	}
	
	private void connect() throws XMPPException, Exception {
		System.out.println("Connecting to guru...");
		ConnectionConfiguration config = new ConnectionConfiguration(GOOGLE_TALK_HOST, GOOGLE_TALK_PORT, GOOGLE_TALK_SERVICE);
		// config.setCompressionEnabled(true);
		config.setSASLAuthenticationEnabled(true);
		if (username.equals("")) {
			Properties props = Goat.getPasswords();
			username = props.getProperty("gmail.username");
			password = props.getProperty("gmail.password");
			if (username.equals("")) {
				Exception e = new Exception ("Couldn't load user/pass for Google Talk (gmail)");
				throw e;
			}
		}
		connection = new XMPPConnection(config);
		connection.connect();
		connection.login(username, password);
		chatmanager = connection.getChatManager();
		if (connection.isConnected() && connection.isAuthenticated()) {
			System.out.println("Connected to " + GURU_ADDRESS + " at " + GOOGLE_TALK_HOST);
		}
	}
	
	private class GuruResponseHandler implements MessageListener {
		public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message guruMessage) {
			String recipient = guruMessage.getThread();
			recipient = recipient.trim();
			String body = guruMessage.getBody();
			Message m = Message.createPagedPrivmsg(recipient, body);
			m.send();
		}
	}
}
