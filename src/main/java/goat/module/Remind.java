/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of ReminderBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ReminderBot.java,v 1.3 2004/05/29 19:44:30 pjm2 Exp $

*/

package goat.module;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.*;
import java.io.*;

import goat.Goat;
import goat.core.*;
import goat.util.Reminder;

public class Remind extends Module {

    private static final String REMINDER_FILE = "resources/reminders";
    private static goat.core.Users users;	//all the users
    private ReminderTimer timer;
    private LinkedList<Reminder> reminders = new LinkedList<Reminder>();
    private ExecutorService pool = Goat.modController.getPool();
    
    public Remind() {
        loadReminders();
        //setName(name);
        //setAutoNickChange(true);
        
        timer = new ReminderTimer(this);
        pool.execute(timer);
        users = goat.Goat.getUsers() ;
    }
    
    public void processChannelMessage(Message m) {
        //User user ;
		//if (users.hasUser(m.sender)) {
		//	user = users.getUser(m.sender) ;
		//} else {
		//	user = new User(m.sender) ;
		//}
        Pattern messagePattern = Pattern.compile("^\\s*\\w*\\s+in\\s+(((\\d+\\.?\\d*|\\.\\d+)\\s+(weeks?|days?|hours?|hrs?|minutes?|mins?|seconds?|secs?)[\\s,]*(and)?\\s+)+)(.*)\\s*$");
        Matcher matcher = messagePattern.matcher(m.getModTrailing());
        if (matcher.matches()) {
            String reminderMessage = matcher.group(6);
            String periods = matcher.group(2);
            
            long set = System.currentTimeMillis();
            long due = set;


            GregorianCalendar cal;
            String timeZone = "GMT";
            if (users.hasUser(m.getSender()) && ! users.getUser(m.getSender()).getTimeZoneString().equals(""))
            	timeZone = users.getUser(m.getSender()).getTimeZoneString();
            cal = new GregorianCalendar(TimeZone.getTimeZone(timeZone));



            try {
                double weeks = getPeriod(periods, "weeks|week");
                double days = getPeriod(periods, "days|day");
                double hours = getPeriod(periods, "hours|hrs|hour|hr");
                double minutes = getPeriod(periods, "minutes|mins|minute|min");
                double seconds = getPeriod(periods, "seconds|secs|second|sec");
                due += (weeks * 604800 + days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000;
            }
            catch (NumberFormatException e) {
                m.reply("I can't quite deal with numbers like that!");
                return;
            }
            
            if (due == set) {
                m.reply("Example of correct usage: \"Remind me in 1 hour, 10 minutes to check the oven.\"  I understand all combinations of weeks, days, hours, minutes and seconds.");
                return;
            }

            Reminder reminder = new Reminder(m.getChanname(), getName(m), m.getSender(), reminderMessage, set, due);
            String name = getName(m);
            String replyName = null;
            if(name.equals(m.getSender()))
                replyName = "you";
            else
                replyName = name;

            cal.setTimeInMillis( reminder.getDueTime() );
            
            String date = String.format(Locale.UK, "%1$td/%1$tm/%1$ty %1$tR", cal);
            m.reply(m.getSender() + ": Okay, I'll remind " + replyName + " about that on " + date + " " + timeZone);
            reminders.add(reminder);
            timer.interrupt();
        }
    }

    private String getName(Message m) {
        String[] words = m.getModTrailing().split(" ");
        if(words[0].equals("me"))
            return m.getSender();
        return words[0];
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public String[] getCommands() {
        return new String[]{"remind"};
    }

    private double getPeriod(String periods, String regex) throws NumberFormatException {
        Pattern pattern = Pattern.compile("^.*?([\\d\\.]+)\\s*(?i:(" + regex + ")).*$");
        Matcher m = pattern.matcher(periods);
        m = pattern.matcher(periods);
        if (m.matches()) {
            double d = Double.parseDouble(m.group(1));
            if (d < 0 || d > 1e6) {
                throw new NumberFormatException("Number too large or negative (" + d + ")");
            }
            return d;
        }
        return 0;
    }
    
    private class ReminderTimer implements Runnable {
    	
    	private Remind secretary;
    	ReminderTimer(Remind reminder) {
    		secretary = reminder;
    	}
    	private Thread myThread = null;
    	
    	public synchronized void run() {
    		myThread = Thread.currentThread();
    		boolean running = true;
    		while (true) {

    			// If the list is empty, wait until something gets added.
    			if (secretary.reminders.size() == 0) {
    				try {
    					wait();
    				}
    				catch (InterruptedException e) {
    					// Do nothing.
    				}
    			}

    			Reminder reminder = secretary.reminders.getFirst();
    			long delay = reminder.getDueTime() - System.currentTimeMillis();
    			if (delay > 0) {
    				try {
    					wait(delay);
    				}
    				catch (InterruptedException e) {
    					// A new Reminder was added. Sort the list.
    					Collections.sort(secretary.reminders);
    					secretary.saveReminders();
    				}
    			}
    			else {
    				String replyName = reminder.getReminder();

    				if(replyName==null || replyName.equals(reminder.getNick()))
    					replyName = "You";
    				Message.createPrivmsg(reminder.getChannel(), reminder.getNick() + ": " + replyName + " asked me to remind you " + reminder.getMessage());
    				secretary.reminders.removeFirst();
    				secretary.saveReminders();
    			}
    		}
    	}
    	
    	public synchronized void interrupt() {
    		if(myThread != null)
    			myThread.interrupt();
    	}
    }
    
    private void saveReminders() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(REMINDER_FILE)));
            out.writeObject(reminders);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            // If it doesn't work, no great loss!
        }
    }
    
    private void loadReminders() {
        try {
        	ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(REMINDER_FILE)));
        	Object ob = in.readObject();
        	if(ob instanceof LinkedList) {
        		LinkedList<?> ll = (LinkedList<?>) ob;
        		for(Object item: ll)
        			if(item instanceof Reminder)
        				reminders.add((Reminder) item);
        	}
        	in.close();
        }
        catch (Exception e) {
            // If it doesn't work, no great loss!
        }
    }    
    

    
}