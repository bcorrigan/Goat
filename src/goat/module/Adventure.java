package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.util.ZScreen;
import goat.util.ZMachine;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Arrays;

/**
 * The ZMachine variously calls all ZScreen methods as it runs. However the ZMachine is very character orientated
 * and is designed to run on machines with control over some display device, so a lot of these methods
 * would make a lot of sense if we were doing a wrapper here for a terminal, a GUI, or something
 * like that, but we're not, cos goat can only output a line at a time and has no control beyond that, can't read
 * individual keys as they are pressed, and so on. So I am just going to ignore as much of that as I can and
 * half-arsedly emulate the remainder and cross my fingers to hope for something that still works.
 *
 * @author bc
 * Date: 07-Nov-2004
 * Time: 23:54:04
 */
public class Adventure extends Module implements ZScreen, Runnable {

    private ZMachine zm;
    private char nbuf[] = new char[16];
    private Thread zgameTh;
    private Thread zmachineTh;
    private Message target;
    private LinkedList input;
    private boolean playing=false;
    private String buffer = new String();

    //for random numbers, to give to ZMachine
    private Random r = new Random();

    public void processPrivateMessage(Message m) {
        if(m.modCommand.equals("adv") && playing==true) {
            input.addLast(m.modTrailing.trim());
        } else if(m.modCommand.equals("startadv") && playing==false) {
            target = m;
            input = new LinkedList();
            playing = true;
            zgameTh = new Thread(this);
            zgameTh.start();
        } else if(m.modCommand.equals("stopadv") && playing==true) {
            zgameTh.stop();         //TODO bad to use stop() for a thread, fix, blah blah
            playing = false;
        }
    }

    public void processChannelMessage(Message m) {
        processPrivateMessage(m);
    }

    public String[] getCommands() {
		return new String[]{"adv", "startadv", "stopadv"};
	}

    public void run() {
        //we need to read in the zmachine data file here
        byte[] data;
        try {
            FileInputStream is = new FileInputStream("resources/leather_goddessen_of_phobos.z5");
            data = new byte[is.available()];
            is.read(data);
        } catch (FileNotFoundException e) {
            System.err.println("Adventure: game file not found");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        zm = new ZMachine(this, data);
        zmachineTh = new Thread(zm);
        zmachineTh.start();
        //zm.run();

        //here we want to poll the buffer and print it out when we notice something, which should hopefully chunkify it
        for( ; ; ) {
            while(buffer.length()==0)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            target.createPagedReply(buffer).send();
            buffer = new String();
        }
    }


    //All subsequent methods are implementations of methods defined in goat.util.ZScreen.

    public void NewLine() {
        //no reason to implement this
    }

    //convert data to a String, send it out
    public void Print(char data[], int len) {
        char[] outData = new char[len];
        for(int i=0; i<len; i++)
            outData[i] = data[i];
        String m = new String(outData);
        if(!m.startsWith(">"))
            buffer += m;
    }

    //let's emulate the topmost line being a sequence of pressed characters
    public int Read() {
        while(input.size()==0)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        String line = (String) input.removeFirst();

        char returnChar = line.charAt(0);
        line = line.substring(1, line.length());
        if(line.length()>0)
            input.addFirst(line);
        return returnChar;
    }

    //peel the oldest string off the input stack, convert it to array of char for value-return
    //if it isn't there, wait until it is.
    //some interesting facts: the return value here is apparantly the length of the data you have put into buffer, not
    //a success code or anything
    public int ReadLine(char buffer[]) {
        while(input.size()==0)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        String line = (String) input.removeFirst();
        char[] newBuffer = line.toCharArray();
        for(int i=0;i<newBuffer.length; i++)
            buffer[i] = newBuffer[i];
        return line.length();
    }

    //stop and exit
	public void exit() {
        playing = false;
        zgameTh.stop();     //TODO why am I so lazy and keep using stop()'s ??
    }

    //not sure what's going on here, stole this bit. Appears to return a random number but don't
    //know why it is ANDing with 0x8000 or what exactly limit is
	public int Random(int limit) {
        if((limit & 0x8000) != 0) {
			r.setSeed(limit & 0x7fff);
			return 0;
		}
		if(limit == 0) {
			r = new Random();
			return 0;
		}
		return r.nextInt(limit) + 1;
    }

    //don't know what this does, so I am ignoring it
    public void SetStatus(char line[], int len) {

    }

    public int GetWidth() { return 40; }
	public int GetHeight() { return 15; }

    //ignoring this
    public void Restart() {

    }

    //this is for saving and restoring games I think, I am going to ignore this for now
    //but could be useful later
    public boolean Save(byte state[]) { return false; }
	public byte[] Restore() { return null; }

    //ignoring all this screen-orientated stuff
    public void SetWindow(int num) {

    }

    public void SplitWindow(int height) {

    }

    public void EraseWindow(int number) {

    }

    public void MoveCursor(int x, int y) {

    }
    //don't knwo what this does, stolen from the implementation. Was actually in ZScreen before I made I made it an interface,
    //so it must be default in some way. Why can't people comment code? Fuck.
    public void PrintNumber(int num) {
		/*int i = 16;
		int j;

		do {
			nbuf[--i] = (char) ('0' + (num % 10));
			num = num / 10;
		} while(num > 0);

		for(j = 0; i < 16; j++){
			nbuf[j] = nbuf[i++];
		}
		Print(nbuf, j); */
	}

	public void PrintChar(int ch) {
		nbuf[0] = (char) ch;
		Print(nbuf, 1);
	}
}
