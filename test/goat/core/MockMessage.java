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
        sentMessage = getTrailing();
    }

    public Message createReply(String trailing) {
        this.setTrailing(trailing);
        return this; //don't create new Message on createReply
    }
    
    public void setModTrailing(String modTrailing) {
    	super.setModTrailing(modTrailing);
    }
    
    public void setModCommand(String modCommand) {
    	super.setModCommand(modCommand);
    }
    
    public void setPrivate(boolean isPrivate) {
    	super.setPrivate(isPrivate);
    }
 
    public void setSender(String sender) {
    	super.setSender(sender);
    }
}
