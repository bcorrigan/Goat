package goat.core;
/**
 * Extends Message and overrides send() method for testing purposes.
 * Cached what gets sent to send() so test classes can see what output is.
 * @author bc
 * Date: 12-Mar-2007
 * Time: 21:35:00
 */
public class MockMessage extends Message {

    public String sentMessage;

    public MockMessage(String prefix, String command, String params, String trailing) {
        super(prefix, command, params, trailing);
    }

    public MockMessage(String messagestring) {
        super(messagestring);
    }

    public void send() {
        sentMessage = trailing;
    }

    public Message createReply(String trailing) {
        this.trailing = trailing;
        return this; //don't create new Message on createReply
    }
}
