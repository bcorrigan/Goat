package goat.core;
import java.awt.event.*;

/**
 * <P>There was a small need for a simpler timer class than that of java.util.Timer.</P> <P>One problem is that the
 * existing timer must subclass java.util.TimerTask, which is a more complex process than adding an implementation of
 * ActionListener to your class.</P>
 */

public class SimpleTimer extends Thread {

	private ActionListener listener;

	private boolean keeprunning;
	private boolean runevent = true;

	private int delay;

	private boolean recurring;

	/**
	 * Creates a timer that fires once.
	 * 
	 * @param delay    The time in seconds until the task executes.
	 * @param listener The action listener to fire when the timer expires.
	 */
	public SimpleTimer(int delay, ActionListener listener) {
		this.recurring = true;

		this.delay = delay;
		this.listener = listener;

		start();
	}

	/**
	 * Creates a timer.
	 * 
	 * @param delay     The time in seconds until the task executes.
	 * @param listener  The action listener to fire when the timer expires.
	 * @param recurring Should this timer cease after firing once or continue to fire at regular intervals
	 */
	public SimpleTimer(int delay, ActionListener listener, boolean recurring) {
		this.recurring = recurring;

		this.delay = delay;
		this.listener = listener;

		start();
	}

	public void run() {
		keeprunning = recurring;

		while (keeprunning) {
			try {
				sleep(1000 * delay);
			} catch (InterruptedException e) {
			}

			if (runevent) {
				listener.actionPerformed(new ActionEvent(this, 0, "Timer Expired."));
			} else
				runevent = true;
		}
	}

	public void pleaseStop() {
		keeprunning = false;
		restart();
	}

	private void restart() {
		runevent = false;
		interrupt();
	}
}