package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.util.ZScreen;
import goat.util.ZMachine;

import java.io.*;
import java.util.LinkedList;
import java.util.Random;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The ZMachine variously calls all ZScreen methods as it runs. However the ZMachine is very character orientated
 * and is designed to run on machines with control over some display device, so a lot of these methods
 * would make a lot of sense if we were doing a wrapper here for a terminal, a GUI, or something
 * like that, but we're not, cos goat can only output a line at a time and has no control beyond that, can't read
 * individual keys as they are pressed, and so on. So I am just going to ignore as much of that as I can and
 * half-arsedly emulate the remainder and cross my fingers to hope for something that still works.
 *
 * @author bc
 *         Date: 07-Nov-2004
 *         Time: 23:54:04
 */
public class Adventure extends Module implements ZScreen, Runnable {

    private String gameName;    //the name of the game (ie zork, whatever)
    private String saveGameName; //saveGame string

    private ZMachine zm;
    private char nbuf[] = new char[16];
    private Thread zgameTh;
    private Thread zmachineTh;
    private Message target;
    private LinkedList input;
    // private boolean playing = false; // we don't use this anywhere
    private String buffer = "";
    private boolean running = true;

    private int saveSlot;
    private int loadSlot;

    private File gameImage;

    private static ArrayList adventures = new ArrayList(); //all the adventures being played

    //for random numbers, to give to ZMachine
    private Random r = new Random();

    public void processPrivateMessage(Message m) {
        if (m.modCommand.equals("adv")) {
            Iterator it = adventures.iterator();
            while (it.hasNext()) {
                Adventure adv = (Adventure) it.next();
                if (adv.target.channame.equals(m.channame)) {
                    //this channel has a running adventure
                    //first we check for "save" and "restore" commands, because they are going to be treated specially
                    if (m.modTrailing.startsWith("save")) {
                        //look for the int argument
                        String intArg = m.modTrailing.replaceAll("save", "");
                        intArg = intArg.trim();
                        String number = "";
                        String stringArg = "";
                        for (int i = (intArg.length() - 1); i >= 0; i--) {
                            if (intArg.charAt(i) >= '0' && intArg.charAt(i) <= '9')
                                number += intArg.charAt(i);
                            else {
                                stringArg = intArg.substring(0, i);
                                break;
                            }
                        }

                        if (!stringArg.matches("^[a-zA-Z]*$")) {
                            m.createReply("Comment must match ^[a-zA-Z]*$").send();
                            return;
                        }
                        if (stringArg.length() > 25) {
                            m.createReply("No save game comments greater than 25 chars allowed!").send();
                            return;
                        }
                        adv.saveGameName = stringArg;
                        try {
                            adv.saveSlot = Integer.parseInt(number);
                            if (adv.saveSlot > 10 || adv.saveSlot < 1) {
                                m.createReply("Please choose a slot between 1 and 10.").send();
                                return;
                            }
                        } catch (NumberFormatException nfe) {
                            m.createReply("Invalid syntax for save command. Try \"save <int>\" " +
                                    "where int is between 1 and 10.").send();
                            return;
                        }
                        //got this far, everything must be valid
                        adv.input.addLast("save");
                        return;
                    }
                    if (m.modTrailing.startsWith("restore")) {
                        //look for the int argument
                        String intArg = m.modTrailing.replaceAll("restore", "");
                        intArg = intArg.trim();
                        if (intArg.length() == 0) {//oh-ho, no int argument given, so lets use the default slot
                            if (adv.loadSlot >= 1 && adv.loadSlot <= 10)
                                adv.input.addLast("restore");
                            else
                                m.createReply("There doesn't seem to be a default restore point. ").send();
                            return;
                        }
                        try {
                            adv.loadSlot = Integer.parseInt(intArg);
                            if (adv.loadSlot > 10 || adv.loadSlot < 1) {
                                m.createReply("Please choose a slot between 1 and 10.").send();
                                adv.loadSlot = 0;
                                return;
                            }
                        } catch (NumberFormatException nfe) {
                            m.createReply("Invalid syntax for restore command. Try \"restore <int>\" " +
                                    "where int is between 1 and 10.").send();
                            return;
                        }
                        //got this far, everything must be valid
                        adv.input.addLast("restore");
                        return;
                    }
                    adv.input.addLast(m.modTrailing.trim());
                    return;
                }
            }
        } else if (m.modCommand.equals("startadv")) {
            Iterator it = adventures.iterator();
            while (it.hasNext()) {
                Adventure adv = (Adventure) it.next();
                if (adv.target.channame.equals(m.channame)) {
                    m.createReply("Umm, we seem to be already playing an Adventure game in here.").send();
                    return; //this channel already has a running adventure
                }
            }
            File gameImage;
            String intArg = m.modTrailing.replaceAll("startadv", "");
            intArg = intArg.trim();
            try {
                int zmNum = Integer.parseInt(intArg);
                gameImage = getZMFile(zmNum);
                if (gameImage == null) {
                    m.createReply("That's not a valid game number!").send();
                    listFiles(m);
                    return;
                }
            } catch (NumberFormatException nfe) {
                m.createReply("Invalid syntax for startadv command. Try \"startadv <int>\" " +
                        "where int is the number of an adventure game:").send();
                listFiles(m);
                return;
            }

            Adventure adv = new Adventure();
            adv.target = m;
            adv.input = new LinkedList();
            // adv.playing = true; // we don't use this anywhere
            adv.zgameTh = new Thread(adv);
            adv.gameName = gameImage.getName().replaceAll("\\.z3", "").replaceAll("\\.z5", "");
            adv.gameImage = gameImage;
            adventures.add(adv);
            adv.zgameTh.start();
        } else if (m.modCommand.equals("stopadv")) {
            Iterator it = adventures.iterator();
            while (it.hasNext()) {
                Adventure adv = (Adventure) it.next();
                if (adv.target.channame.equals(m.channame)) {
                    //this channel has a running adventure
                    adventures.remove(adv);
                    // adv.playing = false;  //we don't use this anywhere
                    adv.running = false;
                    m.createReply("Game stopped!").send();
                    return;
                }
            }
        } else if (m.modCommand.equals("lsgames")) {
            listFiles(m);
        } else if (m.modCommand.equals("lssaves")) {
            Iterator it = adventures.iterator();
            while (it.hasNext()) {
                Adventure adv = (Adventure) it.next();
                if (adv.target.channame.equals(m.channame)) {
                    listSaves(m, adv);
                    return;
                }
            }
            m.createReply("Not playing a game, not going to list all possible save files.").send();
        }
    }

    //list the save games for the current game, ie which slots are taken
    private void listSaves(Message m, Adventure adv) {
        m.createReply("The following save slots are taken for this game: ").send();
        File file = new File("resources/adventureData/saves");
        File[] files = file.listFiles();
        // String reply = ""; // unused
        // int counter = 0; //unused
        String slots = "";
        for (int i = 0; i < files.length; i++) {
            String[] parts = files[i].getName().split("\\."); //parts of the file name
            if (parts[0].equals(adv.gameName))
                if (parts[1].equals(m.channame))
                    slots += " " + parts[3] + ")" + parts[2];
        }
        m.createReply("\"" + adv.gameName.replaceAll("_", " ") + "\" slots used: " + slots).send();
    }

    //sends a list of all the available games with their numbers
    private void listFiles(Message m) {
        File file = new File("resources/adventureData");
        File[] files = file.listFiles();
        String reply = "";
        int counter = 0;
        for (int i = 0; i < files.length; i++)
            if (!files[i].isDirectory())
                if (files[i].getName().endsWith(".z3") || files[i].getName().endsWith(".z5")) {
                    counter++;
                    String fileName = files[i].getName().replaceAll("\\.z3", "").replaceAll("\\.z5", "");
                    fileName = fileName.replaceAll("_", " ");
                    reply += counter + ") " + fileName + " ";
                }
        m.createReply(reply).send();
    }

    //has a look at the directory of game files and get's the nth one and returns it
    //the actual ordering of the numbering is irrelevant so long as it is consistent
    private File getZMFile(int zmNum) {
        File file = new File("resources/adventureData");
        File[] files = file.listFiles();
        int counter = 0;
        for (int i = 0; i < files.length; i++)
            if (!files[i].isDirectory())
                if (files[i].getName().endsWith(".z3") || files[i].getName().endsWith(".z5")) {
                    counter++;
                    if (counter == zmNum)
                        return files[i];
                }
        return null;
    }

    public void processChannelMessage(Message m) {
        processPrivateMessage(m);
    }

    public static String[] getCommands() {
        return new String[]{"adv", "startadv", "stopadv", "lsgames", "lssaves"};
    }

    public void run() {
        //we need to read in the zmachine data file here
        byte[] data;
        FileInputStream is = null;
        try {
            is = new FileInputStream(gameImage);
            data = new byte[is.available()];
            is.read(data);
        } catch (FileNotFoundException e) {
            System.err.println("Adventure: game file not found");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            if(is!=null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        zm = new ZMachine(this, data);
        zmachineTh = new Thread(zm);
        zmachineTh.start();
        Thread.currentThread().getName();
        //zm.run();

        //here we want to poll the buffer and print it out when we notice something, which should hopefully chunkify it
        for (; ;) {
            while (buffer.length() == 0)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            target.createPagedReply(buffer).send();
            buffer = "";

            //for gracefully exiting the threads. No stop()s, for once.
            if (!running) {
                zm.running = false;
                return;
            }
        }
    }


    //All subsequent methods are implementations of methods defined in goat.util.ZScreen.

    public void NewLine() {
        //no reason to implement this
    }

    //convert data to a String, send it out
    public void Print(char data[], int len) {
        char[] outData = new char[len];
        System.arraycopy(data, 0, outData, 0, len);
        String m = new String(outData);
        if (!m.startsWith(">"))
            buffer += m;
    }

    //let's emulate the topmost line being a sequence of pressed characters
    public int Read() {
        while (input.size() == 0)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        String line = (String) input.removeFirst();

        char returnChar = line.charAt(0);
        line = line.substring(1, line.length());
        if (line.length() > 0)
            input.addFirst(line);
        return returnChar;
    }

    //peel the oldest string off the input stack, convert it to array of char for value-return
    //if it isn't there, wait until it is.
    //some interesting facts: the return value here is apparantly the length of the data you have put into buffer, not
    //a success code or anything
    public int ReadLine(char buffer[]) {
        while (input.size() == 0)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        String line = (String) input.removeFirst();
        char[] newBuffer = line.toCharArray();
        System.arraycopy(newBuffer, 0, buffer, 0, newBuffer.length);
        return line.length();
    }

    //stop and exit
    public void exit() {
        // playing = false; // we don't use this anywhere
        zgameTh.stop();     //TODO why am I so lazy and keep using stop()'s ??
    }

    //not sure what's going on here, stole this bit. Appears to return a random number but don't
    //know why it is ANDing with 0x8000 or what exactly limit is
    public int Random(int limit) {
        if ((limit & 0x8000) != 0) {
            r.setSeed(limit & 0x7fff);
            return 0;
        }
        if (limit == 0) {
            r = new Random();
            return 0;
        }
        return r.nextInt(limit) + 1;
    }

    //don't know what this does, so I am ignoring it
    public void SetStatus(char line[], int len) {

    }

    public int GetWidth() {
        return 40;
    }

    public int GetHeight() {
        return 15;
    }

    //ignoring this
    public void Restart() {

    }

    public boolean Save(byte state[]) {
        File file = new File("resources/adventureData/saves");
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            String[] parts = files[i].getName().split("\\."); //parts of the file name
            if (parts[0].equals(gameName))
                if (parts[1].equals(target.channame))
                    if (parts[3].equals(Integer.toString(saveSlot))) {
                        file = files[i];                             //slot is taken! delete old occupant of slot
                        file.delete();
                    }
        }
        File savefile = new File("resources/adventureData/saves/" + gameName + "." + target.channame + "." + saveGameName + "." + saveSlot);
        loadSlot = saveSlot; //set default restore point
        try {
            savefile.createNewFile();
            FileOutputStream fos = new FileOutputStream(savefile);
            fos.write(state);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] Restore() {

        File file = new File("resources/adventureData/saves");
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            String[] parts = files[i].getName().split("\\."); //parts of the file name
            if (parts[0].equals(gameName))
                if (parts[1].equals(target.channame))
                    if (parts[3].equals(Integer.toString(loadSlot)))
                        file = files[i];
        }

//File file = new File("resources/adventureData/saves/" + gameName + "." + target.channame + "." + loadSlot);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //ignoring all this screen-orientated stuff
    public void SetWindow(int num) {

    }

    public void SplitWindow(int height) {

    }

    public void EraseWindow(int number) {

    }

    public void MoveCursor(int x, int y) {

    }

    public void PrintNumber(int num) {
        String st = Integer.toString(num);
        char[] chars = st.toCharArray();
        Print(chars, chars.length);
    }

    public void PrintChar(int ch) {
        nbuf[0] = (char) ch;
        Print(nbuf, 1);
    }
}
