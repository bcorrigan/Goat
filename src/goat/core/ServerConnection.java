package goat.core;

import goat.Goat;
import goat.module.Logger;

import java.net.*;
import java.nio.charset.Charset;
import java.io.*;
import java.sql.SQLException;

/**
 * Maintains the connection with the server. A seperate thread, this.
 *
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 * @version <p>Date: 14.12.2003</p>
 */

public class ServerConnection extends Thread {

    private static MessageQueue inqueue = Goat.inqueue; //Queue of messages FROM the server
    private static MessageQueue outqueue = Goat.outqueue; //Queue of messages TO the server
    private Socket IrcServer;
    private InputHandler ih;
    private OutputHandler oh;
    private String serverName;
    private boolean connected = false;

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
        
        BufferedReader br = new BufferedReader(new InputStreamReader(IrcServer.getInputStream(), BotStats.getCharset()));
        PrintWriter pw = new PrintWriter( new OutputStreamWriter( IrcServer.getOutputStream(), BotStats.getCharset() ), true);
        ih = new InputHandler(br);
        oh = new OutputHandler(pw);

        ih.start();
        oh.start();
        /*  wtf  --rs
        new Message("", "PASS", "foo", "").send();
        */
        new Message("", "NICK", BotStats.botname, "").send();
        new Message("", "USER", "goat" + " nowhere.com " + serverName, BotStats.version).send();
        //we sleep until we are connected, don't want to send these next messages too soon
        while (!connected) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String[] channels = BotStats.getChannels();
        for (int i = 0; i < channels.length; i++)
            new Message("", "JOIN", channels[i], "").send();
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
        private String botdefaultname = BotStats.botname;

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
                        if (messageString.equals(null))
                            continue;
                        Message m = new Message(messageString);

                        if (!connected) {
                            try {
                                int intcommand = Integer.parseInt(m.command);
                                if (intcommand == Message.RPL_ENDOFMOTD)
                                    connected = true;
                                else if (intcommand == Message.ERR_NICKNAMEINUSE) {
                                    namecount++;
                                    BotStats.botname = botdefaultname + namecount;
                                    new Message("", "NICK", BotStats.botname, "").send();
                                    new Message("", "USER", BotStats.botname + " nowhere.com " +
                                            BotStats.servername, BotStats.clientName +
                                            " v." + BotStats.version).send();
                                }
                            } catch (NumberFormatException nfe) {
                                //we ignore this
                            }
                        }

                        if (m.command.equals("PING"))
                            outqueue.enqueue(new Message("", "PONG", "", m.trailing));
                        else
                            inqueue.enqueue(m); //add to inqueue
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
        int clearcount;

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
                synchronized (outqueue) {
                    if (!outqueue.isQueueEmpty()) {
                        sendMessage(outqueue.dequeue());

                        clearcount = 0;
                    } else
                        clearcount++;
                }
                if (clearcount == 100 && bufused > 0) {	//guess that the server has flushed the buffer if I haven't written a message in under two seconds.
                    clearcount = 0;
                    bufused -= 512;
                    if (bufused < 0)
                        bufused = 0;
                }

                try {
                    sleep(20);    //TODO: only do this if we've sent our fill of proper messages
                } catch (InterruptedException e) {
                }
            }
        }

        int bufused;

        // should block until posting won't flood us off
		public void sendMessage(Message m) {
			byte[] outbuffer;
			synchronized (out) {
				// System.out.println("outbuffer:" + String.valueOf(
				// m.toString() ));
				outbuffer = m.toByteArray();
				if (bufused + outbuffer.length > 1024) {
					// hope that sleeping for two seconds will empty the buffer.
					// System.out.println(2000 + bufused * 3);
					try {
						sleep(2000 + bufused * 3);
					} catch (InterruptedException e) {
					}

					bufused >>= 1;
				}

				out.println(m.toString());
				// System.out.println("Outbuffer: prefix: " + m.prefix + "
				// params: " + m.params + " trailing:" + m.trailing);

				bufused += outbuffer.length;
			}

			/*
			 * Log the message, if the Logger module is loaded. Doing DB stuff
			 * in a blocking subroutine might be a little short-sighted, but
			 * this is the only point in the goat code that I'm sure will
			 * capture all outgoing goat messages. There might be a better spot
			 * for this in goat.core.Message, but my eyes go all swimmy when I
			 * try to read that file. --rs
			 */
			Logger lm = (Logger) Goat.modController.get("Logger");
			int id;
			if (null != lm) {
				try {
					// TODO "slashnet" should not be hard-coded here.
					id = lm.logger.logOutgoingMessage(m, "slashnet");

					// uncomment the next bit if you want everything you log
					// retrieved from the db and echoed to the console for debuggo.
					/*
					if (id > -1) 
						lm.logger.printResultSet(lm.logger.getMessage(id));
					else
						if (m.isCTCP) 
							System.out.println("(Did not log outgoing CTCP " + m.CTCPCommand + " message)") ;
						else
							System.out.println("(Did not log outgoing " + m.command + " message)") ;
					*/
				} catch (SQLException e) {
					System.out.println("ERROR -- DB problem while trying to log outgoing message");
					e.printStackTrace();
				}
			} else {
				// uncomment below to console-view messages sent before the Logger is loaded
				// security note:  passwords.
				/*
				System.err.println("Couldn't find Logger module for outgoing message, skipping:");
				System.err.println("   " + m.toString()) ;
				*/
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
}
