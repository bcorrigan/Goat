package goat.core;

import java.util.LinkedList;

/**
 * <p>A threadsafe queue of Messages.
 * @author <p><b>© Barry Corrigan</b> All Rights Reserved.</p>
 * 
 * @version 1.0
 */

public class MessageQueue {

	private LinkedList messages = new LinkedList();

    /**
     * Add a Message to the queue. Thread-safe. When the message is added, any threads in a wait()
	 * state in hasNext() are notified.
     * @param m The Message to be added to the queue.
     */
	public synchronized void enqueue(Message m) {
        messages.add(m);
		notifyAll();
    }
    /**
     * Get the first Message off the queue. Thread-safe..
     * @return The first message object on the queue.
     */
    public synchronized Message dequeue() {
        return (Message) messages.removeFirst();
    }
    /**
	 * Is the queue empty?
	 * @return
	 */
    public synchronized boolean isQueueEmpty() {
        return messages.isEmpty();
    }

	/**
	 * Checks if there is another Message. Is there isn't, this thread goes into a wait() state
	 * until another thread calls enqueue() and notifies this thread.
	 * @return
	 */
    public synchronized boolean hasNext() {
		if(messages.isEmpty())
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("e: " + e.getMessage());
			}
        return !messages.isEmpty();
    }
}