package goat.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import goat.core.Message;
import goat.core.Module;
import goat.countdown.Solver;
import goat.jcalc.Calculator;
import goat.jcalc.CalculatorException;
/**
 * Created on 25-Feb-2006
 * A little puzzle game. Generates puzzles in the style of popular 
 * Channel 4 nerd show, CountDown, and includes a solver that
 * generates best possible solution, swiped from somewhere on the web.
 * @author bc
 */
public class CountDown extends Module implements Runnable {
    
    //is a game of countdown in progress?
    private boolean gameOn = false;
    //the available big numbers to draw from
    private ArrayList bigPool;
    //the available small numbers to draw from
    private ArrayList smallPool;
    //the target number.
    private int targetNumber;
    //the source numbers. 6 numbers drawn from above pools
    private int[] sourceNumbers;
    //the timing thread
    private Thread timerThread;
    //target channel
    private Message target;
    //we use this to evaluate user's attempts at an answer
    Calculator calc = new Calculator();
    private Answer bestAnswer;
    //the best possible answer
    private int bestPossibleAnswer;
    
    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }
    
    /**
     * If we are playing a game, we evaluate every message as a possible answer.
     * It is accepted if it contains only the following operators: +-/*() and
     * the numbers in the game, each not more than once. If the input passes this test,
     * it is evaluated. The closest to the answer is acknowledged by goat. If
     * the answer is the correct answer, the user wins it. If the time runs out,
     * Solver class is used to display correct answer.
     * If we are nto playing a game, we pick 6 random numbers weighted the countdown
     * way, and a 3 digit target number. We display these and kick the game off.
     */
    public void processChannelMessage(Message m) {
        if(gameOn) {
            String attempt = m.trailing;
            //first, check attempt is valid. if so, evaluate, else ignore
            if( checkAttemptValid( attempt ) ) {
                //it is valid, so we evaluate
                try {
                    String answer = calc.evaluate_equation( m.trailing );
                    Answer possibleAnswer;
                    try {
                        possibleAnswer = new Answer( Integer.parseInt( answer ), m.sender );
                    } catch( NumberFormatException nfe) {
                        m.createReply("You used a formula that resulted in a non-int answer. This is not allowed!").send();
                        return;
                    }
                    if(bestAnswer == null || bestAnswer.getDistance( targetNumber )>possibleAnswer.getDistance( targetNumber ) ) {
                        bestAnswer = possibleAnswer;
                        if(bestAnswer.getAnswer() == bestPossibleAnswer) {
                            finaliseGame();
                            return;
                        }
                        m.createReply( Message.BOLD + m.sender + Message.BOLD 
                                        + " has the best answer so far: " 
                                        + possibleAnswer.getAnswer() + ". " + "Just " 
                                        + possibleAnswer.getDistance( targetNumber ) 
                                        + " off the target of " 
                                        + targetNumber + "!" ).send();
                    }
                } catch(CalculatorException ce) {
                    m.createReply(ce.getLocalizedMessage()).send();
                }
            }
        } else {
            //generate some random numbers.
            initialisePools();
            initialiseNumbers();
            //game on
            gameOn = true;
            target = m;
            timerThread = new Thread(this);
            timerThread.start();
            bestPossibleAnswer = Solver.getBestVal( sourceNumbers, targetNumber);
            m.createReply(Message.REVERSE + "***" + Message.REVERSE
                    + " New Numbers: " + Message.BOLD
                    + formatNumbers( sourceNumbers ) 
                    + Message.BOLD + " Target: " + Message.BOLD + targetNumber).send();
        }
    }
    
    /**
     * Finalises the game. So, reinitialises things for the next game and 
     * displays the correct solution if the user has not worked it out already.
     */
    private void finaliseGame() {
        gameOn = false;
        if(bestAnswer==null) {
            target.createReply("Nobody got an answer. The best answer was: " + Solver.Solve( sourceNumbers, targetNumber )).send();
            return;
        }
        if( bestAnswer.getAnswer() == bestPossibleAnswer ) {
            String reply = Message.BOLD + bestAnswer.getUsername() + Message.BOLD
                                + " has won with " + bestAnswer.getAnswer() + "!";
            if( bestPossibleAnswer!=targetNumber)
                reply+=" This was the best possible answer.";
            target.createReply(reply).send();
        } else {
            target.createReply( "The best answer was " + bestAnswer.getAnswer() + " by " + bestAnswer.getUsername() + "."
                                + " But the best possible answer was: " + Solver.Solve( sourceNumbers, targetNumber )).send(); 
        }
        bestAnswer=null;
        timerThread.stop();
    }
    
    /**
     * Checks that the string param is valid as per the rules of the game.
     * So, checks that it contains only the allowed numbers, and operators.
     * @param attempt The string to be checked for validity
     * @return <code>true</code> if the string is valid, <code>false</code> if not.
     */
    private boolean checkAttemptValid( String attempt ) {
        /*Pattern p = Pattern.compile("\\d*"); //any sequence of numbers
        Matcher m = p.matcher(attempt);
        while(m.find()) {
            String number = m.group();
            System.out.println( "number: " + number);
            int num = Integer.parseInt( number );
            if( !sourceNumbersContain( num ))
                return false;
        }*/
        String tokenString = attempt.replaceAll( "[^\\d]", " ");
        StringTokenizer st = new StringTokenizer(tokenString);
        int[] userNums = new int[st.countTokens()];
        int i=0;
        while(st.hasMoreTokens()) { 
            int num = Integer.parseInt( st.nextToken());
            userNums[i] = num;
            i++;
        }
        if( !numbersCorrect( userNums )) {
            target.createReply("You used a number not in the selection!").send();
            return false;
        }
        //all numbers are correct, so now check remaining operators.
        //we gradually strip away everything allowed. If anything is left after that,
        //the attempt is invalid.
        attempt = attempt.replaceAll("\\d", "");             //strip all numbers
        attempt = attempt.replaceAll("/", ""); //strip \ sign
        attempt = attempt.replaceAll("\\*", "");  //strip *
        attempt = attempt.replaceAll("\\+", "");  //strip +
        attempt = attempt.replaceAll("\\-", "");  //strip -
        attempt = attempt.replaceAll("\\)", "");  //strip )
        attempt = attempt.replaceAll("\\(", "");  //strip (
        attempt = attempt.replaceAll(" ", "");  //zap spaces
        
        if(attempt.length()>0)
            return false;
        return true;
    }
    
    /**
     * Fill each ArrayList with Integers representing the pools of big and small numbers
     */
    private void initialisePools() {
        bigPool = new ArrayList();
        smallPool = new ArrayList();
        int[] bigPoolInts = {25, 50, 75, 100};
        int[] smallPoolInts = {1,2,3,4,5,6,7,8,9,10,1,2,3,4,5,6,7,8,9,10};
        for(int i=0; i<bigPoolInts.length; i++) {
            bigPool.add( new Integer(bigPoolInts[i]));
        }
        for(int i=0; i<smallPoolInts.length; i++) {
            smallPool.add( new Integer(smallPoolInts[i]));
        }
        //mix them up
        Collections.shuffle( bigPool );
        Collections.shuffle( smallPool );
    }
    
    /**
     * Fills the source numbers array from the pools.
     * For the moment has an 80% chance of filling it with
     * one big number and 5 small numbers, and a 20% chance
     * of 2 big numbers and 4 small. Will mix this up a bit later.
     * Also sets the target number.
     */
    private void initialiseNumbers() {
        double choice = Math.random();
        sourceNumbers = new int[6];
        if(choice<0.8) {
            //one big number, 5 small.
            sourceNumbers[0] = ((Integer) bigPool.remove(0)).intValue();
            for( int i=1; i<6; i++) {
                sourceNumbers[i] = ((Integer) smallPool.remove(0)).intValue();
            }
        } else {
            //two big numbers, 5 small
            sourceNumbers[0] = ((Integer) bigPool.remove(0)).intValue();
            sourceNumbers[1] = ((Integer) bigPool.remove(0)).intValue();
            for( int i=2; i<6; i++) {
                sourceNumbers[i] = ((Integer) smallPool.remove(0)).intValue();
            }
        }
        
        //for target all numbers from 101 to 999 are equally likely
        targetNumber = 101 + (int) (899 * Math.random());
    }
    
    public int messageType() {
        if (!gameOn)
            return WANT_COMMAND_MESSAGES;
        else
            return WANT_UNCLAIMED_MESSAGES;
    }
    
    public String[] getCommands() {
        return new String[]{"countdown"};
    }
    
    public void run() {
        //wait 30 seconds
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
        }

        target.createReply(Message.BOLD + "10 secs..").send();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        gameOn = false;
        finaliseGame();
    }
    
    //TODO move to some sort of goat-wide util class?
    private String formatNumbers(int[] numbers) {
        String formattedResponse = "";
        for( int i=0; i<numbers.length; i++) {
            formattedResponse += numbers[i] + " ";
        }
        return formattedResponse;
    }
    
    private boolean numbersCorrect( int[] num ) {
        //first, create an ArrayList from sourceNumbers
        ArrayList sourceNumList = new ArrayList();
        for( int i=0; i<sourceNumbers.length; i++) {
            sourceNumList.add( new Integer( sourceNumbers[i] ));
        }
        for(int i=0; i<num.length;i++) {
            boolean removed = sourceNumList.remove( new Integer( num[i] ) );
            if(!removed)
                return false;
        }
        return true;
    }
    
    //bean for answers, makes it easy to serialise later and keep track of things generally
    private class Answer {
        private int answer;
        private String username;
        
        public Answer( int answer, String username) {
            this.answer = answer;
            this.username = username;
        }
        
        public void setAnswer(int answer) {
            this.answer = answer;
        }
        public int getAnswer() {
            return answer;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getUsername() {
            return username;
        }
        
        public int getDistance( int target ) {
            if(target>answer)
                return target - answer;
            else if(target<answer)
                return answer-target;
            else return 0;
        }
    }
}
