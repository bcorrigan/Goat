package goat.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Barry Corrigan
 *         <p/>
 *         A general utility class for accessing a dictionary
 */
public class Dict {

	private RandomAccessFile rafDict;
	private final static File DICTFILE = new File("resources/words");
	/* private final static int[][] METADATA = new int[25][25]; 	//an array holding locations in file

	static {
		try {
			String word;
			BufferedReader br = new BufferedReader(new FileReader(DICTFILE));
			while ((word = br.readLine()) != null) {

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * This no-args constructor exits if for whatever reason the Dict resource does not exist.
	 */
	public Dict() {
		try {
			rafDict = new RandomAccessFile(DICTFILE, "r");
		} catch (FileNotFoundException fnfe) {
			System.out.println("dict file does not exist.");
			fnfe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Just gets a totally random word from the dictionary file.
	 *
	 * @return A random word
	 */
	public String getRandomWord() {
		String word;
		long seekLoc = (long) (Math.random() * DICTFILE.length());
		try {
			rafDict.seek(seekLoc);
			rafDict.readLine();	//chuck away first, it's garbage
			word = rafDict.readLine();
			return word;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/**
	 * Gets all the words in the dictionary which have letters that match the supplied string. Ignores spaces around the string, and case.
	 *
	 * @param targetWord The word you are seeking all matches with.
	 * @return An ArrayList of the matching words
	 */
	public ArrayList getMatchingWords(String targetWord) {
		targetWord.trim();
		targetWord.toLowerCase();
		String word;
		ArrayList validWords = new ArrayList();
		try {
			BufferedReader br = new BufferedReader(new FileReader(DICTFILE));
			while ((word = br.readLine()) != null) {
				word.toLowerCase();
				if (checkWord(word, targetWord))
					validWords.add(word);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return validWords;
	}

	/**
	 * Checks if the supplied String is a word in the dict or not. Ignores spaces around the word, if it has any, and case.
	 *
	 * @param word
	 * @return <code>true</code> if a word contained in dictionary file, <code>false</code> otherwise.
	 */
	public boolean contains(String word) {
		word.toLowerCase();
		word.trim();
		//OK lets use a bsearch
		// TODO Make faster by cacheing data about file and using it to set an initial floor and ceiling. Investiagate, mibbie not worth it.
		long guessPos = DICTFILE.length() / 2;
		long ceiling = DICTFILE.length(), floor = 0, oldCeiling = 1, oldFloor = 2;
		try {
			do {
				rafDict.seek(guessPos);
				rafDict.readLine();	//throw away the first
				String guessWord = rafDict.readLine();
				guessWord.toLowerCase();
				if (guessWord == null)
					return false;
				int ComparisonResult = compare(word, guessWord);
				if (ComparisonResult < 0)
					ceiling = guessPos;
				else if (ComparisonResult > 0)
					floor = guessPos;
				else
					return true;
				guessPos = ((ceiling - floor) / 2) + floor;
				if (ceiling == floor || ((oldCeiling == ceiling) && (oldFloor == floor)))
					return false;
				oldCeiling = ceiling;
				oldFloor = floor;
			} while (true);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return false;
	}

	private int compare(String word1, String word2) {
		if (word1.equals(word2))
			return 0;
		String[] ordered = {word1, word2};
		Arrays.sort(ordered);
		if (ordered[0].equals(word1))
			return -1;
		else if (ordered[0].equals(word2))
			return 1;
		return 0;
	}

	/**
	 * Main method here for debugging
	 */
	public static void main(String[] args) {
		Dict dict = new Dict();
		System.out.println("Some random words, hopefully:\n\n");
		for (int i = 0; i < 100; i++)
			System.out.print(dict.getRandomWord() + " ");
		//now check bsearch
		System.out.println("\n\nAll these values should be true:\n\n");
		for (int i = 0; i < 100; i++) {
			String word = dict.getRandomWord();
			System.out.println(word + " :contains() returns: " + dict.contains(word));
		}
		System.out.println("\n\nAnd these false:\n\n");
		System.out.println("poopmastah" + " :contains() returns: " + dict.contains("poopmastah"));
		System.out.println("assedsr" + " :contains() returns: " + dict.contains("assedsr"));
		System.out.println("zogg" + " :contains() returns: " + dict.contains("zogg"));
		System.out.println("aaaaa" + " :contains() returns: " + dict.contains("aaaaa"));
		System.out.println("zzzzz" + " :contains() returns: " + dict.contains("zzzzz"));
	}

	protected void finalize() throws Throwable {
		try {
			rafDict.close();
		} finally {
			super.finalize();
		}
	}

	private int lineCount(File file) {
		int count = 0;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			while (line != null) {
				count++;
				line = in.readLine();
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("The dictionary seems to have vanished!");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return count;
	}

	/**
	 * Checks if a given word is valid for current char arraylist.
	 *
	 * @param word Word to be checked.
	 * @return True if matches, false if not.
	 */
	private boolean checkWord(String word, String targetWord) {
		ArrayList targetLetters = new ArrayList();
		for (int i = 0; i < targetWord.length(); i++) {
			targetLetters.add(new Character(targetWord.charAt(i)));
		}
		Iterator it = targetLetters.iterator();
		ArrayList wordLetters = new ArrayList();
		for (int i = 0; i < word.length(); i++) {
			wordLetters.add(new Character(word.charAt(i)));
		}
		while (it.hasNext()) {
			char letter = ((Character) it.next()).charValue();
			for (int i = 0; i < wordLetters.size(); i++) {
				if (wordLetters.size() == 0)
					return false;
				char wordLetter = ((Character) wordLetters.get(i)).charValue();
				if (wordLetter == letter) {
					wordLetters.remove(i);
					break;
				}
			}
		}
		if (wordLetters.size() == 0)
			return true;
		return false;
	}
}
