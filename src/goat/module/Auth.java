package goat.module;

import goat.core.Module;
import goat.core.Message;
import goat.core.BotStats;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;

/**
 * <p>Date: 18-Dec-2003</p>
 * @author <p><b>� Barry Corrigan</b> All Rights Reserved.</p>
 */
public class Auth extends Module {

    private String passwordhash;

    public Auth() {
        loadPassword();
    }

    public void processPrivateMessage(Message m) {
        if (checkPassword(m.modTrailing.trim().toLowerCase())) {
            BotStats.owner = m.prefix;
            sendMessage(m.createReply("Authorisation successful."));
            sendMessage(m.createReply("You (" + m.prefix + ") are now my registered owner."));	//TODO: This still needs to watch the user to determine if they drop.
            sendMessage(new Message("", "MODE", m.channame + " +o " + BotStats.owner, ""));
        } else
            sendMessage(m.createReply("Invalid login."));
    }

    public void processChannelMessage(Message m) {
    }

    public String[] getCommands() {
        return new String[]{"auth"};
    }

    private boolean checkPassword(String input) {
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
        for (int i = 0; i < digest.length; i++) {
            current = digest[i];
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
            r = new BufferedReader(new FileReader("password.txt"));
            passwordhash = r.readLine();
            r.close();
        } catch (IOException e) {
            System.err.println("Could not open password file \"password.txt\".");
            passwordhash = "";
            return;
        }
    }

    public void updatePassword(String newpassword, String ownername) {
        PrintWriter w;
        MessageDigest d;
        try {
            d = MessageDigest.getInstance("MD5");
            d.update(newpassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            sendMessage(new Message("", "NOTICE", ownername, "Could not open Message Digest algorithm."));
            return;
        }
        byte[] digest = d.digest();
        try {
            w = new PrintWriter(new FileWriter("password.txt"));
        } catch (IOException e) {
            sendMessage(new Message("", "NOTICE", ownername, "Couldn't open file."));
            return;
        }
        byte current, hibits, lobits;
        String out = "";
        for (int i = 0; i < digest.length; i++) {
            current = digest[i];
            hibits = (byte) ((current & 0xf0) >> 4);
            lobits = (byte) (current & 0x0f);
            out += Integer.toString((int) hibits, 16);
            out += Integer.toString((int) lobits, 16);
        }

        w.println(out);
        w.close();

        sendMessage(new Message("", "NOTICE", ownername, "Authentication tokens updated successfully."));
        passwordhash = out;
    }
}
