package goat.core;

import goat.Goat;

import java.net.*;
import java.io.*;

/**
 * Maintains the connection with the server. A seperate thread, this.
 * @version <p>Date: 14.12.2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */

public class ServerConnection extends Thread {

    private static MessageQueue inqueue = Goat.inqueue; //Queue of messages FROM the server
    private static MessageQueue outqueue = Goat.outqueue; //Queue of messages TO the server
    private Socket IrcServer;
    private InputHandler ih;
    private OutputHandler oh;
    private String serverName;

    /**
     * Connects us to a server.
     * @param serverName The server to connect to.
     */
    public ServerConnection(String serverName) {
        this.serverName = serverName;
        connect();
    }

    private void connect() {
        try {
            IrcServer = new Socket(serverName, 6667);
        } catch(UnknownHostException uhe) {
            System.out.println("UnknownHostException: " + uhe.getMessage());
            System.exit(0);
        } catch(IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
            System.exit(0);
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(IrcServer.getInputStream()));
            OutputStream os = IrcServer.getOutputStream();
            ih = new InputHandler(br);
            oh = new OutputHandler(os);
        } catch(IOException ioe) {
            System.out.println("Error opening streams to IRC server: " + ioe.getMessage());
            System.exit(0);
        }
        ih.start();
        oh.start();
        new Message("", "PASS", "foo", "").send();
        new Message("", "NICK", BotStats.botname, "").send();
        new Message("", "USER", "goat" + " nowhere.com " + serverName, BotStats.version).send();
		String[] channels = BotStats.getChannels();
		for(int i=0; i<channels.length; i++)
			new Message("", "JOIN", channels[i], "").send();
    }

    private void reconnect() {
		try {
			sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connect();
    }

    class InputHandler extends Thread {
        BufferedReader in;
		private boolean keeprunning = true;
        public InputHandler(BufferedReader in) {
            this.in=in;
        }

		void disconnect() {
			keeprunning=false;
		}

        public void run() {
            String messageString;
			long lastActivity=System.currentTimeMillis();
            while(keeprunning) {
                try {
                    if(in.ready()) {
                        messageString = in.readLine();
						lastActivity=System.currentTimeMillis();
						if(messageString.equals(null))
							continue;
						Message m = new Message(messageString);
						if(m.command.equals("PING"))
							outqueue.enqueue(new Message("", "PONG", "", m.trailing));
                        else inqueue.enqueue(m); //add to inqueue
						//System.out.println("Inbuffer: prefix: " + m.prefix + " params: " + m.params + " trailing:" + m.trailing + " command:" + m.command + " sender: " + m.sender +
						//		           "\n    " + "isCTCP:" + m.isCTCP + " isPrivate:" + m.isPrivate + " CTCPCommand:" + m.CTCPCommand + " CTCPMessage:" + m.CTCPMessage);
                    } else {
						if((System.currentTimeMillis()-lastActivity)>305000) {
							in.close();
							oh.disconnect();
							reconnect();
							return;
						}
                        sleep(100);
                    }
                } catch (IOException ioe) {
                    System.out.println("EOF on connection: " + ioe.getMessage());
                } catch(InterruptedException ie) {
                    System.out.println("Interrupted: " + ie.getMessage());
                }
            }
        }
    }

    class OutputHandler extends Thread {

        OutputStream out;
        private boolean keeprunning;
        int clearcount = 0;

		void disconnect() {
			keeprunning=false;
		}

        public OutputHandler(OutputStream out) {
            this.out = out;
            this.setName("Output Handler (client -> server)");
            keeprunning = true;
        }

        public void run() {
            while (keeprunning) {
				synchronized(outqueue) {
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

        int bufused = 0;

        //should block until posting won't flood us off
        public void sendMessage(Message m) {
            byte[] outbuffer;

            synchronized (out) {
                outbuffer = m.toByteArray();
                if ((bufused + outbuffer.length) > 1024) {
                    //hope that sleeping for two seconds will empty the buffer.
                    try {
                        sleep(2000 + bufused * 3);
                    } catch (InterruptedException e) {
                    }

                    bufused /= 2;
                }

                try {
                    out.write(outbuffer);
					System.out.println("Outbuffer: prefix: " + m.prefix + " params: " + m.params + " trailing:" + m.trailing);
                } catch (IOException e) {
                    System.out.println("Write error: " + e.getMessage());
                    reconnect();
                    return;
                }

                bufused += outbuffer.length;
            }
        }
    }
}
