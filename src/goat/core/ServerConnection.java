package goat.core;

import java.net.*;
import java.io.*;
import java.util.LinkedList;

/**
 * Maintains the connection with the server. A seperate thread, this.
 * @version <p>Date: 14.12.2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */

public class ServerConnection extends Thread {

    private MessageQueue inqueue; //Queue of messages FROM the server
    private static MessageQueue outqueue;//Queue of messages TO the server
    private Socket IrcServer;
    private InputHandler ih;
    private OutputHandler oh;
    private String serverName;

    /**
     * Connects us to a server.
     * @param serverName The server to connect to.
     * @param inqueue Messages from the server
	 * @param outqueueIn Messages to the server
     */
    public ServerConnection(String serverName, MessageQueue inqueue, MessageQueue outqueueIn) {
        this.inqueue = inqueue;
        outqueue = outqueueIn;
        this.serverName = serverName;
        connect();
    }

    private void connect() {
        //@todo Make it keep trying to reconnect on failure
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
            ih = new InputHandler(br, this);
            oh = new OutputHandler(os, this);
        } catch(IOException ioe) {
            System.out.println("Error opening streams to IRC server: " + ioe.getMessage());
            System.exit(0);
        }
        ih.start();
        oh.start();
        outqueue.enqueue(new Message("", "PASS", "foo", ""));
        outqueue.enqueue(new Message("", "NICK", BotStats.botname, ""));
        outqueue.enqueue(new Message("", "USER", "goat" + " nowhere.com " + serverName, BotStats.version));
    }

    private void reconnect() {
        connect();
    }

    class InputHandler extends Thread {
        BufferedReader in;
        ServerConnection parent;
        public InputHandler(BufferedReader in, ServerConnection parent) {
            this.in=in;
            this.parent=parent;
        }

        public void run() {
            String messageString;
            System.out.println("hi!");
            while(true) {
                try {
                    if(in.ready()) {
                        messageString = in.readLine();
						if(messageString.equals(null))
							continue;
						Message m = new Message(messageString);
						if(m.command.equals("PING")) //@TODO make a note of time between pings, if it exceeds 300 seconds reconnect to server
							outqueue.enqueue(new Message("", "PONG", "", m.trailing));
                        else inqueue.enqueue(m); //add to inqueue
						System.out.println("Inbuffer: prefix: " + m.prefix + " params: " + m.params + " trailing:" + m.trailing + " command:" + m.command + " sender: " + m.sender +
								           "\n    " + "isCTCP:" + m.isCTCP + " isPrivate:" + m.isPrivate + " CTCPCommand:" + m.CTCPCommand + " CTCPMessage:" + m.CTCPMessage);

							  //notify the MessageDispatcher thread that its got a message waiting
						
                    } else {
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
        boolean keeprunning;
        ServerConnection parent;
        int clearcount = 0;

        public OutputHandler(OutputStream out, ServerConnection parent) {
            this.out = out;
            this.parent = parent;
            this.setName("Output Handler (client -> server)");
            keeprunning = true;
        }

        public void pleaseStop() {
            keeprunning = false;
            try {
                join();
            } catch (InterruptedException e) {
            }
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

        /**
         * Flushes the message queue. this is useful for clean disconnection.
         */
        public void flush() {
			synchronized(outqueue) {
            	while (!outqueue.isQueueEmpty()) {
                	sendMessage(outqueue.dequeue());
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
                    parent.reconnect();
                    return;
                }

                bufused += outbuffer.length;
            }

        }
    }


    //dirty testing method
    public static void main(String[] args) {
        //ServerConnection sc = new ServerConnection("coruscant.slashnet.org");
        try {
            sleep(9999999);
        } catch (InterruptedException e) {
            System.out.println("e: " + e.getMessage());
        }
    }

	
}
