package goat.module;

import goat.core.Module;
import goat.core.Message;

import java.io.*;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User: bc
 * Date: 05-Feb-2005
 * Time: 13:07:57
 */
public class Quiz extends Module implements Runnable {

    private static final File QUESTION_FILE = new File("resources/questions");
    private static RandomAccessFile questions;
    private boolean playing;
    private String answer;
    private ArrayList<Integer> tip;     //the current tip. List of indexes of letters (not spaces) in answers
    private Message target;
    private boolean answered;

    static {
        try {
            questions = new RandomAccessFile(QUESTION_FILE, "r");
        } catch (FileNotFoundException fnfe) {
            System.out.println("questions file does not exist.");
            System.exit(1);
        }
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if (!playing) {
            playing = true;
            target = m;
            Thread t = new Thread(this);
            t.start();
        } else {
            if(m.modCommand.equals("stopquiz")) {
                playing = false;
                return;
            }
            if (m.trailing.toLowerCase().trim().contains(answer.toLowerCase().trim()) && !answered) {
                m.createReply(m.sender + ": Congratulations, you got the correct answer, \"" + answer + "\".").send();
                answered = true;
            }
        }
    }

    public int messageType() {
        if (!playing)
            return WANT_COMMAND_MESSAGES;
        else
            return WANT_UNCLAIMED_MESSAGES;
    }

    public String[] getCommands() {
        return new String[]{"quiz"};
    }

    public void run() {
        while (playing) {
            target.createReply(getNewQuestion()).send();
            answered = false;
            tip = null;
            //now sleep until either a new tip is needed, or the question is answered
            while (!answered) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!answered&&playing)
                    if(tip==null || (canTip() && tip.size()>2))
                        target.createReply(Message.BOLD + "tip: " + Message.BOLD + getTip()).send();
                    else {
                        target.createReply("Nobody got the answer! it was \"" + answer + "\".").send();
                        answered = true;
                    }
            }
        }
    }

    private String getTip() {
        if (!canTip()) {
            tip = new ArrayList<Integer>(answer.length());
            for (int i = 0; i < answer.length(); i++) {
                if (Character.isLetter(answer.charAt(i)))
                    tip.add(i);
            }
            Collections.shuffle(tip);
        }

        //now take two letters away
        if (tip.size() >= 2) {
            tip.remove(0);
            tip.remove(0);
        } else {
            tip.remove(0);
        }

        //now convert to a StringBuf
        StringBuffer tipStb = new StringBuffer(answer);
        for (int i : tip) {
            tipStb.setCharAt(i, '_');
        }
        return (tipStb.toString());
    }

    private boolean canTip() {
        if (tip == null || tip.size() == 0)
            return false;
        return true;
    }

    private String getNewQuestion() {
        try {
            questions.seek((long) (Math.random() * (QUESTION_FILE.length() - 200)));
            String result;
            do {
                result = questions.readLine();
            } while (!result.startsWith("Category:"));
            result = Message.BOLD + result.replaceFirst("Category: ", "") + Message.BOLD;
            result += ". " + questions.readLine().replaceFirst("Question:", "");
            answer = questions.readLine().replaceAll("Answer: ", "");
            answer = answer.replaceAll("#", "");
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
