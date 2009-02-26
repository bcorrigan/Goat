package goat.module;

import goat.Goat;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * User: bc
 * Date: 05-Feb-2005
 * Time: 13:07:57
 */
public class Quiz extends Module {

    private static final File QUESTION_FILE = new File("resources/questions");
    private static RandomAccessFile questions;
    private boolean playing;
    private String answer;
    private ArrayList<Integer> hiddenTipChars;     //the current tip. List of indexes of letters (not spaces) in answers
    private Message target;
    private boolean answered;
    private QuizRunner runner = null;

    //TODO huge bug-- one game/many channels, 
    
    private ExecutorService pool = Goat.modController.getPool();

    static {
        try {
            questions = new RandomAccessFile(QUESTION_FILE, "r");
        } catch (FileNotFoundException fnfe) {
            System.out.println("questions file does not exist.");
            //System.exit(1);   //um...
        }
    }
    
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if (!playing) {
            playing = true;
            target = m;
            runner = new QuizRunner(this);
            pool.execute(runner);
        } else {
            if(m.getModCommand().equals("stopquiz")) {
                playing = false;
                if(runner != null)
                	runner.InterruptRound();
                return;
            }
            if (m.getTrailing().toLowerCase().trim().matches(answer.toLowerCase().trim()) && !answered) {
                m.createReply(m.getSender() + ": Congratulations, you got the correct answer, \"" + answer + "\".").send();
                answered = true;
                if(runner != null)
                	runner.InterruptRound();
            }
        }
    }

    public int messageType() {
        if (!playing)
            return WANT_COMMAND_MESSAGES;
        else
            return WANT_UNCLAIMED_MESSAGES;
    }

    public static String[] getCommands() {
        return new String[]{"quiz"};
    }

    private class QuizRunner implements Runnable {
    	private Quiz thisGame;
    	public QuizRunner(Quiz quiz) {
    		thisGame = quiz;
    	}
    	
    	private Thread myThread = null;
    	private int rounds = 10;
    	private boolean sleeping = false;
    	
    	public synchronized void run() {
    		myThread = Thread.currentThread();
    		int round = 0;
    		thisGame.target.createReply("Starting a " + rounds + " round quiz in 3\u2026").send();
    		try {
    			Thread.sleep(1000);
    			thisGame.target.createReply("2\u2026").send();
    			Thread.sleep(1000);
    			thisGame.target.createReply("1\u2026").send();
    			Thread.sleep(1000);
    		} catch (InterruptedException ie) {}

    		while (thisGame.playing && ++round <= rounds) {
    			thisGame.target.createReply("Question #" + round);
    			thisGame.target.createReply(getNewQuestion()).send();
    			thisGame.answered = false;
    			thisGame.hiddenTipChars = null;
    			//now sleep until either a new tip is needed, or the question is answered
    			while (!thisGame.answered) {
    				try {
    					sleeping = true;
    					Thread.sleep(10000);
    					sleeping = false;
    				} catch (InterruptedException ie) {
    					sleeping = false;
    					//ie.printStackTrace();
    				}
    				if (!thisGame.answered&&thisGame.playing)
    					if(thisGame.hiddenTipChars==null || (thisGame.canTip() && thisGame.hiddenTipChars.size()>= answer.length()/3)) {
    						thisGame.target.createReply(Constants.BOLD + "tip: " + Constants.BOLD + thisGame.getTip()).send();
    					} else {
    						System.out.println("ending round...");
    						thisGame.target.createReply("Nobody got the answer! it was \"" + thisGame.answer + "\".").send();
    						thisGame.answered = true;
    					}
    			}
    		}
    	}
    	public synchronized void InterruptRound() {
    		if(myThread != null && sleeping)
    			myThread.interrupt();
    	}
    }

    private void initTip() {
        hiddenTipChars = new ArrayList<Integer>(answer.length());
        for (int i = 0; i < answer.length(); i++) {
            if (Character.isLetter(answer.charAt(i)))
                hiddenTipChars.add(i);
        }
        Collections.shuffle(hiddenTipChars);
    }
    
    private String getTip() {
        if (!canTip())
        	initTip();
        
        //now take two letters away
        hiddenTipChars.remove(0);
        if (hiddenTipChars.size() >= 1)
        	hiddenTipChars.remove(0);

        //now convert to a StringBuf
        StringBuffer tipStb = new StringBuffer(answer);
        for ( int i=0; i< hiddenTipChars.size(); i++) {
        	if(Character.isLetter(tipStb.charAt(hiddenTipChars.get(i))))
        		tipStb.setCharAt(hiddenTipChars.get(i), '_');
        }
        return (tipStb.toString());
    }

    private boolean canTip() {
        return !(hiddenTipChars == null || hiddenTipChars.size() == 0);
        }

    private String getNewQuestion() {
        try {
            questions.seek((long) (Math.random() * (QUESTION_FILE.length() - 200)));
            String result;
            do {
                result = questions.readLine();
            } while (!result.startsWith("Category:"));
            result = Constants.BOLD + result.replaceFirst("Category: ", "") + Constants.BOLD;
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
