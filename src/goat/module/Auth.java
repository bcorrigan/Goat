package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;

/**
 * <p>Date: 18-Dec-2003</p>
 * @author <p><b>? Barry Corrigan</b> All Rights Reserved.</p>
 */
public class Auth extends Module {

    private String passwordhash;
	 private static final String passwordFile = "resources/password.txt" ;

    public Auth() {
        loadPassword();
    }

    public void processPrivateMessage(Message m) {
        if (checkPassword(m.getModTrailing().trim().toLowerCase())) {
            BotStats.getInstance().setOwner( m.getPrefix() );
            m.createReply("Authorisation successful.").send();
            m.createReply("You (" + m.getPrefix() + ") are now my registered owner.").send();	//TODO: This still needs to watch the user to determine if they drop.
            new Message("", "MODE", m.getChanname() + " +o " + BotStats.getInstance().getOwner(), "").send();
        } else
            m.createReply("Invalid login.").send();
    }

    public void processChannelMessage(Message m) {
    }

    public String[] getCommands() {
        return new String[]{"auth"};
    }

    private boolean checkPassword(String input) {
        // next line should make goat pick up password changes
		  // that happen outside the running goat jvm.
		  loadPassword() ;
        MessageDigest d;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Could not open Message Digest algorithm.");
            return false;
        }
        d.update(input.getBytes());
        byte[] digest = d.digest();
        byte current, hibits, lobits;
        String out = "";
        for (byte aDigest : digest) {
            current = aDigest;
            hibits = (byte) ((current & 0xf0) >> 4);
            lobits = (byte) (current & 0x0f);
            out += Integer.toString((int) hibits, 16);
            out += Integer.toString((int) lobits, 16);
        }
        return out.equals(passwordhash);
    }

    private void loadPassword() {
        BufferedReader r;
        try {
            r = new BufferedReader(new FileReader(passwordFile));
            passwordhash = r.readLine();
            r.close();
        } catch (IOException e) {
            System.err.println("Could not open password file \"" + passwordFile + "\".");
            passwordhash = "";
            return;
        }
    }

    public void updatePassword(String newpassword, String ownername) {
		  if (updatePassword(newpassword)) {
            new Message("", "NOTICE", ownername, "Authentication tokens updated successfully.").send();
		  } else {
            new Message("", "NOTICE", ownername, "There was an error updating the authentication tokens;  new password not set.").send();
		  }
    }
	 
    public boolean updatePassword(String newpassword) {
        PrintWriter w;
        MessageDigest d;
        try {
            d = MessageDigest.getInstance("MD5");
            d.update(newpassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("PASSWORD UPDATE for FAILED -- Could not open Message Digest algorithm.") ;
            return false;
        }
        byte[] digest = d.digest();
        try {
            w = new PrintWriter(new FileWriter(passwordFile));
        } catch (IOException e) {
            System.out.println("PASSWORD UPDATE FAILED -- Couldn't open file \"" + passwordFile + "\"") ;
            return false;
        }
        byte current, hibits, lobits;
        String out = "";
        for (byte aDigest : digest) {
            current = aDigest;
            hibits = (byte) ((current & 0xf0) >> 4);
            lobits = (byte) (current & 0x0f);
            out += Integer.toString((int) hibits, 16);
            out += Integer.toString((int) lobits, 16);
        }
        w.println(out);
        w.close();
		  passwordhash = out ;
		  return true ;
    }
	 
	/**
	 * Sets new password.
	 * <p/>
	 * goat is running-- goat won't pick up the new password until he restarts.
	 *
	 * @param	args[] first cli argument should be the old password, second the new.
	 */
	 public static void main (String [] args) {
		 if (! (2 == args.length)) {
			 System.out.println("Usage: {command} oldpass newpass") ;
			 return ;
		 }
		 System.out.println("Changing password from " + args[0] + " to " + args[1]) ;
		 Auth a = new Auth() ;
		 if (a.checkPassword(args[0])) {
			 if (a.updatePassword(args[1])) {
			 	System.out.println("New password set.") ;
			 }
		 } else {
			 System.out.println("Old password does not match, new password not set.") ;
		 }
	 }
}
