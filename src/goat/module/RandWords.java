package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.util.Dict;
import goat.util.CommandParser;
import static goat.core.Constants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import java.io.FileReader;
import javax.script.Invocable;
import javax.script.ScriptException;

/**
 * Title:
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * <p/>
 * @author encontrado
 *
 * @version 1.0
 */

public class RandWords extends Module {

    private Random random = new Random() ;
    private Dict dict = new Dict() ;

    public int messageType() {
        return WANT_COMMAND_MESSAGES;
    }
    public String[] getCommands() {
        return new String[]{
            "randword",
            "randwords",
            "bandname",
            "headline",
            "emoji",
            "goatji",
            "brofist",
            "slowclap",
        };
    }

    public RandWords() {
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m) ;
    }

    public void processChannelMessage(Message m) {
        int num = 1 ;
        CommandParser parser = new CommandParser(m) ;
        if (m.getModCommand().equalsIgnoreCase("randword")
            || m.getModCommand().equals("randwords")) {
            try {
                if (parser.hasNumber())
                    num = getNumber(m);
                if (num > 30)
                    num = 30 ;
                m.reply(randWordString(num)) ;
            } catch (NumberFormatException nfe) {}
        } else if (m.getModCommand().equalsIgnoreCase("bandname")) {
            String arg = m.getModTrailing().trim() ;
            String words;
            if (arg.equals("")) {
                words = randWordString(2);
            } else if (random.nextBoolean())
                words = arg + ' ' + getWord();
            else
                words = getWord() + ' ' + arg;
            m.reply(words);
            gisSearch(m, words);
        } else if (m.getModCommand().equalsIgnoreCase("headline")) {
            ArrayList<String> seeds = parser.remainingAsArrayList() ;
            String words;
            if (seeds.isEmpty())
                words = randWordString(4);
            else if (seeds.size() > 3) {
                seeds.add(getWord());
                Collections.shuffle(seeds);
                words = al2str(seeds);
            } else {
                while (seeds.size() < 4)
                    seeds.add(getWord());
                Collections.shuffle(seeds);
                words = al2str(seeds);
            }
            m.reply(words);
            gisSearch(m, words);
        } else if (m.getModCommand().equalsIgnoreCase("emoji")) {
            try {
                if(parser.hasNumber())
                    num = getNumber(m);
                if (num == 1) {
                    m.reply(randEmojiWithName());
                } else {
                    if (num > emoji.size())
                        num = emoji.size();
                    ArrayList<Integer> tmp = new ArrayList<Integer>(emoji);
                    Collections.shuffle(tmp);
                    String ret = "";
                    for (int i = 0; i < num; i++)
                        ret += new String(Character.toChars(tmp.get(i))) + " ";
                    m.reply(ret) ;
                }
            } catch (NumberFormatException nfe) {}
        } else if (m.getModCommand().equalsIgnoreCase("goatji")) {
            m.reply(GOATJI);
        } else if (m.getModCommand().equalsIgnoreCase("brofist")) {
            m.reply(new String(Character.toChars(128074)));
        } else if (m.getModCommand().equalsIgnoreCase("slowclap")) {
            try {
                Thread.sleep(1500);
                m.reply(new String(Character.toChars(128079)));
                Thread.sleep(2000);
                m.reply("  " + new String(Character.toChars(128079)));
                Thread.sleep(2500);
                m.reply("  " + NORMAL + "  " + new String(Character.toChars(128079)));
            } catch(InterruptedException tie) {}
         }
    }

    private int getNumber(Message m) throws NumberFormatException {
        String scold = "Don't fuck with me, tough guy.";
        try {
            CommandParser parser = new CommandParser(m) ;
            int num = parser.findNumber().intValue();
            if (num > 10000) {
                scold = "Don't be ridiculous.";
                throw new NumberFormatException();
            } else if (num > 1000) {
                scold = "You're being unreasonable.";
                throw new NumberFormatException();
            } else if (num < 1) {
                scold = "Is that your idea of a joke, nerd?";
                throw new NumberFormatException();
            }
            return num;
        } catch (NumberFormatException nfe) {
            m.reply(scold);
            throw nfe;
        }
    }

    private String randWordString(int num) {
        if (0 == num)
            return "um...";
        else if( 1 == num )
            return getWord();
        else
            return al2str(getWords(num));
    }

    private String al2str(ArrayList<String> words) {
        String ret = "";
        for (String word : words) {
            ret += word + " ";
        }
        return ret.trim() ;
    }

    private String getWord() {
        return dict.getWord(random.nextInt(dict.numWords));
    }

    private ArrayList<String> getWords(int num) {
        ArrayList<String> wordList = new ArrayList<String>(num);
        for (int i=0; i < num; i++) {
            wordList.add(dict.getWord(random.nextInt(dict.numWords)));
        }
        return wordList;
    }

    // this module is a strange place to stash the emoji stuff,
    //   but it's not really big enough for its own home yet
    private static ArrayList<Integer> emoji = new ArrayList<Integer>();

    private void initEmoji() {
        int ranges[][] = {{127744, 128511}, {128512, 128591}, {128640, 128767}, {9728, 9983}};
        for (int j = 0; j < ranges.length; j++)
            for (int i = ranges[j][0]; i <= ranges[j][1]; i++)
                if (Character.isDefined(i)) // for some reason, this is hugely expensive
                    emoji.add(i);
    }

    private String randEmojiWithName() {
        if (emoji.isEmpty())
            initEmoji();
        int ch = emoji.get(random.nextInt(emoji.size()));
        return new String(Character.toChars(ch)) + "  " + Character.getName(ch);
    }

    // Try to load a python
    private Invocable inv = null;

    private void initEngine() throws Exception {
        if (inv == null) {
            FileReader f = new FileReader("libpy/goatpy/tumblr.py");
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            engine.eval(f);
            inv = (Invocable) engine;
        }
    }

    private void gisSearch(Message m, String result) {
        try {
            initEngine();
        } catch (Exception e) {
            m.reply("error making a python: " + e.getMessage());
            return;
        }
        Object ret;
        try {
            // TODO add tags.
            ret = inv.invokeFunction("gis_search", result);
        } catch (Exception e) {
            m.reply("error gis searching: " + e.getMessage());
            return;
        }
        if (ret instanceof String) {
            String message = (String) ret;
            if (message.length() > 0)
                m.reply(message);
        }
    }
}
