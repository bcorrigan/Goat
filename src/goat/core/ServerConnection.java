package goat.core;

import goat.Goat;

import java.net.*;
import java.nio.charset.Charset;
import java.io.*;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maintains the connection with the server. A seperate thread, this.
 *
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 14.12.2003</p>
 */

public class ServerConnection extends Thread {

    private static LinkedBlockingQueue<Message> inqueue = Goat.inqueue; //Queue of messages FROM the server
    private static LinkedBlockingQueue<Message> outqueue = Goat.outqueue; //Queue of messages TO the server
    private Socket IrcServer;
    private InputHandler ih;
    private OutputHandler oh;
    private String serverName;
    private boolean connected = false;
    private boolean alreadySeenMOTD = false;

    /**
     * Connects us to a server.
     *
     * @param serverName The server to connect to.
     */
    public ServerConnection(String serverName) {
        this.serverName = serverName;
        reconnect();
    }

    private void connect() throws UnknownHostException, IOException {
        IrcServer = new Socket(serverName, 6667);

        BufferedReader br = new BufferedReader(new InputStreamReader(IrcServer.getInputStream(), BotStats.getInstance().getCharset()));
        PrintWriter pw = new PrintWriter( new OutputStreamWriter( IrcServer.getOutputStream(), BotStats.getInstance().getCharset() ), true);
        ih = new InputHandler(br);
        oh = new OutputHandler(pw);

        ih.start();
        oh.start();
        /*  wtf  --rs
        new Message("", "PASS", "foo", "").send();
        */
        new Message("", "NICK", BotStats.getInstance().getBotname(), "").send();
        new Message("", "USER", "goat" + " nowhere.com " + serverName, BotStats.getInstance().getVersion()).send();
        //we sleep until we are connected, don't want to send these next messages too soon
        while (!connected) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String[] channels = BotStats.getInstance().getChannels();
        for (String channel : channels) new Message("", "JOIN", channel, "").send();
    }

    private void reconnect() {
        connected = false;
        while (true) {
            try {
                connect();
                return;
            } catch (UnknownHostException uhe) {
                System.out.println("Hmmn unknown host, will wait 305 seconds then try connecting again.. ");
            } catch (IOException ioe) {
                System.out.println("IOException, waiting 305 secs then retry. ");
            }

            try {
                sleep(305000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    class InputHandler extends Thread {
        BufferedReader in;
        private boolean keeprunning = true;
        int namecount = 1;
        private String botdefaultname = BotStats.getInstance().getBotname();

        public InputHandler(BufferedReader in) {
            this.in = in;
        }

        void disconnect() {
            keeprunning = false;
        }

        public void run() {
            String messageString;
            long lastActivity = System.currentTimeMillis();
            while (keeprunning) {
                try {
                    if (in.ready()) {
                        messageString = in.readLine();
                        lastActivity = System.currentTimeMillis();
                        if (messageString == null )
                            continue;
                        Message m = new Message(messageString);

                        if (!connected) {
                            try {
                                int intcommand = Integer.parseInt(m.getCommand());
                                if (intcommand == Constants.RPL_ENDOFMOTD)
                                    connected = true;
                                else if (intcommand == Constants.ERR_NICKNAMEINUSE) {
                                    namecount++;
                                    BotStats.getInstance().setBotname( botdefaultname + namecount );
                                    new Message("", "NICK", BotStats.getInstance().getBotname(), "").send();
                                    new Message("", "USER", BotStats.getInstance().getBotname() + " nowhere.com " +
                                            BotStats.getInstance().getServername(), BotStats.getInstance().getClientName() +
                                            " v." + BotStats.getInstance().getVersion()).send();
                                }
                            } catch (NumberFormatException nfe) {
                                //we ignore this
                            }
                        }

                        if (m.getCommand().equals("PING"))
                            outqueue.add(new Message("", "PONG", "", m.getTrailing()));
                        else
                            inqueue.add(m); //add to inqueue
                        // System.out.println("Inbuffer: prefix: " + m.prefix + " params: " + m.params + " trailing:" + m.trailing + " command:" + m.command + " sender: " + m.sender +
                        //		           "\n    " + "isCTCP:" + m.isCTCP + " isPrivate:" + m.isPrivate + " CTCPCommand:" + m.CTCPCommand + " CTCPMessage:" + m.CTCPMessage);
                    } else {
                        if (System.currentTimeMillis() - lastActivity > 305000) {
                            in.close();
                            oh.disconnect();
                            keeprunning = false;
                            reconnect();
                            return;
                        }
                        sleep(100);
                    }
                } catch (IOException ioe) {
                    System.out.println("EOF on connection: " + ioe.getMessage());
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted: " + ie.getMessage());
                }
            }
        }

        protected void setBR( BufferedReader br) {
        	in = br;
        }
    }

    class OutputHandler extends Thread {

        PrintWriter out;
        private boolean keeprunning;

        void disconnect() {
            keeprunning = false;
        }

        public OutputHandler(PrintWriter out) {
            this.out = out;
            setName("Output Handler (client -> server)");
            keeprunning = true;
        }

        public void run() {
            while (keeprunning) {
                try {
                	Message m = outqueue.take();
                	if(!keeprunning)
                		break;
            		//synchronized (out) { // shouldn't need to synchronize here, there's only one thread running this
        			out.println(m.toString());
        			//}
                } catch (InterruptedException e) {
                }
            }
        }

        protected void setOSW( PrintWriter pw) {
        	out = pw;
        }
    }

    protected void setCharset( Charset cs) {
    	try {
			BufferedReader br = new BufferedReader(new InputStreamReader(IrcServer.getInputStream(), cs));
			PrintWriter pw = new PrintWriter( new OutputStreamWriter( IrcServer.getOutputStream(), cs), true);
			ih.setBR(br);
			oh.setOSW(pw);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	public void setAlreadySeenMOTD(boolean alreadySeenMOTD) {
		this.alreadySeenMOTD = alreadySeenMOTD;
	}

	public boolean alreadySeenMOTD() {
		return alreadySeenMOTD;
	}
}
