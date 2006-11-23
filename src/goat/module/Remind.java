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
import java.util.regex.*;
import java.io.*;

import goat.core.*;
import goat.util.Reminder;

public class Remind extends Module implements Runnable {

    private static final String REMINDER_FILE = "resources/reminders";
    private static goat.core.Users users;	//all the users
    
    public Remind() {
        loadReminders();
        //setName(name);
        //setAutoNickChange(true);
        dispatchThread = new Thread(this);
        dispatchThread.start();
        users = goat.Goat.getUsers() ;
    }
    
    public void processChannelMessage(Message m) {
        User user ;
		if (users.hasUser(m.sender)) {
			user = users.getUser(m.sender) ;
		} else {
			user = new User(m.sender) ;
		}
        Pattern messagePattern = Pattern.compile("^\\s*\\w*\\s+in\\s+(((\\d+\\.?\\d*|\\.\\d+)\\s+(weeks?|days?|hours?|hrs?|minutes?|mins?|seconds?|secs?)[\\s,]*(and)?\\s+)+)(.*)\\s*$");
        Matcher matcher = messagePattern.matcher(m.modTrailing);
        if (matcher.matches()) {
            String reminderMessage = matcher.group(6);
            String periods = matcher.group(2);
            
            long set = System.currentTimeMillis();
            long due = set;


            GregorianCalendar cal;
            String timeZone;
            if (! user.getTimeZone().equals("")) {
                TimeZone tz = TimeZone.getTimeZone(user.getTimeZone());
                cal = new GregorianCalendar(tz);
                timeZone = user.getTimeZone();
            } else {
                cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                timeZone = "GMT";
            }



            try {
                double weeks = getPeriod(periods, "weeks|week");
                double days = getPeriod(periods, "days|day");
                double hours = getPeriod(periods, "hours|hrs|hour|hr");
                double minutes = getPeriod(periods, "minutes|mins|minute|min");
                double seconds = getPeriod(periods, "seconds|secs|second|sec");
                due += (weeks * 604800 + days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000;
            }
            catch (NumberFormatException e) {
                m.createReply("I can't quite deal with numbers like that!").send();
                return;
            }
            
            if (due == set) {
                m.createReply("Example of correct usage: \"Remind me in 1 hour, 10 minutes to check the oven.\"  I understand all combinations of weeks, days, hours, minutes and seconds.").send();
                return;
            }

            Reminder reminder = new Reminder(m.channame, getName(m), m.sender, reminderMessage, set, due);
            String name = getName(m);
            String replyName = null;
            if(name.equals(m.sender))
                replyName = "you";
            else
                replyName = name;

            cal.setTimeInMillis( reminder.getDueTime() );
            
            String date = String.format(Locale.UK, "%1$td/%1$tm/%1$ty %1$tR", cal);
            m.createReply(m.sender + ": Okay, I'll remind " + replyName + " about that on " + date + " " + timeZone).send();
            reminders.add(reminder);
            dispatchThread.interrupt();
        }
    }

    private String getName(Message m) {
        String[] words = m.modTrailing.split(" ");
        if(words[0].equals("me"))
            return m.sender;
        return words[0];
    }

    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public static String[] getCommands() {
        return new String[]{"remind"};
    }

    public double getPeriod(String periods, String regex) throws NumberFormatException {
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
    
    public synchronized void run() {
        boolean running = true;
        while (running) {

            // If the list is empty, wait until something gets added.
            if (reminders.size() == 0) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    // Do nothing.
                }
            }

            Reminder reminder = (Reminder) reminders.getFirst();
            long delay = reminder.getDueTime() - System.currentTimeMillis();
            if (delay > 0) {
                try {
                    wait(delay);
                }
                catch (InterruptedException e) {
                    // A new Reminder was added. Sort the list.
                    Collections.sort(reminders);
                    saveReminders();
                }
            }
            else {
                String replyName = reminder.getReminder();

                if(replyName==null || replyName.equals(reminder.getNick()))
                    replyName = "You";
                Message.createPrivmsg(reminder.getChannel(), reminder.getNick() + ": " + replyName + " asked me to remind you " + reminder.getMessage()).send();
                reminders.removeFirst();
                saveReminders();
            }
            
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
            reminders = (LinkedList) in.readObject();
            in.close();
        }
        catch (Exception e) {
            // If it doesn't work, no great loss!
        }
    }    
    
    private Thread dispatchThread;
    private LinkedList reminders = new LinkedList();
    
}