package goat.core;
import java.io.*;
import java.security.*;

public class SavePassword {
	public static void main(String[] args) {
		PrintWriter w;

		MessageDigest d;

		if (args.length < 1) {
			System.out.println("Usage: java tools/SavePassword <password>. Will leave password.txt in resources dir, best run from top dir of project.");
			return;
		}

		try {
			d = MessageDigest.getInstance("MD5");
			d.update(args[0].getBytes());
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Could not open Message Digest algorithm.");
			return;
		}

		byte[] digest = d.digest();

		try {
			w = new PrintWriter(new FileWriter("resources/password.txt"));
		} catch (IOException e) {
			System.err.println("Couldn't open file");
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

		System.out.println("Authentication Tokens Updated Successfully");
	}
}