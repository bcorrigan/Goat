package goat.core;

import goat.Goat;

import java.net.*;
import java.nio.charset.Charset;
import java.io.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maintains the connection with the server. A seperate thread, this.
 *
 * @author <p><b>Barry Corrigan</b>
 * @version <p>Date: 14.12.2003</p>
 */

public class ServerConnection extends Thread {

    public boolean debug = false;

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

        joinChannels();
    }

    private void reconnect() {
        connected = false;
        while (true) {
            try {
                connect();
                return;
            } catch (UnknownHostException uhe) {
                System.out.println("Hmmn unknown host, will wait 400 seconds then try connecting again.. ");
            } catch (IOException ioe) {
                System.out.println("IOException, waiting 400 secs then retry. ");
            } catch (Exception e) {
		System.err.println("Unexpected exception while trying reconnect() :");
		e.printStackTrace();
	    }

            try {
                sleep(400000);
            } catch (InterruptedException e) {
		System.err.println("Interrupted from sleep between reconnect attempts :");
                e.printStackTrace();
            } catch (Exception e) {
		System.err.println("Unexpected exception while sleeping between reconnects :");
		e.printStackTrace();
	    }

        }
    }

    class InputHandler extends Thread {
        BufferedReader in;
        private volatile boolean keeprunning = true;
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
                        
                        if (messageString == null )
                            continue;
                        Message m = new Message(messageString);

                        if(m.getCommand().matches("\\d+"))
                            try {
                                int intcommand = Integer.parseInt(m.getCommand());
                                if (intcommand == Constants.RPL_ENDOFMOTD) {
                                    System.err.println("MOTD SEEN, must be connected.");
                                    if(connected)
                                        joinChannels();
                                    connected = true;
                                } else if (intcommand == Constants.ERR_NICKNAMEINUSE) {
                                    System.err.println("NICKNAMEINUSE");
                                    namecount++;
                                    BotStats.getInstance().setBotname( botdefaultname + namecount );
                                    new Message("", "NICK", BotStats.getInstance().getBotname(), "").send();
                                    new Message("", "USER", BotStats.getInstance().getBotname() + " nowhere.com " +
                                            BotStats.getInstance().getServername(), BotStats.getInstance().getClientName() +
                                            " v." + BotStats.getInstance().getVersion()).send();
                                    System.err.println("Setting nick to:" + botdefaultname + namecount);
                                }
                            } catch (NumberFormatException nfe) {
                                //we ignore this
                                System.err.println("Unknown command:" + m.getCommand());
                            }

                        if (m.getCommand().equals("PING")) {
                            outqueue.add(new Message("", "PONG", "", m.getTrailing()));
                            if (debug)
                                System.out.println("PUNG at " + new Date());
                        }
                        else if (BotStats.getInstance().containsIgnoreName(m.getSender())) {
                            if (debug)
                                System.out.println("Ignored: " + m);
                        } else {
                            inqueue.add(m); //add to inqueue
                            if (debug)
                                System.out.println(m.toString());
                            // System.out.println("Inbuffer: prefix: " + m.prefix + " params: " + m.params + " trailing:"
                            // + m.trailing + " command:" + m.command + " sender: " + m.sender + "\n    "
                            // + "isCTCP:" + m.isCTCP + " isPrivate:" + m.isPrivate + " CTCPCommand:" + m.CTCPCommand
                            // + " CTCPMessage:" + m.CTCPMessage);
                        }
                        
                        lastActivity = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - lastActivity > 400000) {
                            System.err.println("400 seconds since last activity! Attempting reconnect");
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
                } catch (Exception e) {
		    System.err.println("Unexpected exception in InputHandler :" );
		    e.printStackTrace();
		}
            }
        }

        protected void setBR( BufferedReader br) {
        	in = br;
        }
    }

    class OutputHandler extends Thread {

        PrintWriter out;
        private volatile boolean keeprunning;

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
                } catch (Exception e) {
		    System.err.println("unexpected exception in OuputHandler");
		    e.printStackTrace();
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
	} catch (Exception e) {
	    System.err.println("Unexpected exception in setCharset :");
	    e.printStackTrace();
	}
    }

	public void setAlreadySeenMOTD(boolean alreadySeenMOTD) {
		this.alreadySeenMOTD = alreadySeenMOTD;
	}

	public boolean alreadySeenMOTD() {
		return alreadySeenMOTD;
	}

    private void joinChannels() {
        String[] channels = BotStats.getInstance().getChannels();
        for (String channel : channels) new Message("", "JOIN", channel, "").send();
    }
}
