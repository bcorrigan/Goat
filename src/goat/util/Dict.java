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

	private RandomAccessFile rafDict ;
	private final static File DICTFILE = new File("resources/words");
	private RandomAccessFile rafIndex ;
	private final static File INDEXFILE = new File("resources/words.index") ;
	private int numWords = 0 ;
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
	 *
	 * And to build the index file if it doesn't exist
	 *
	 * And to rebuild the index file if the length recorded in its header doesn't match the length of the Dict resource.
	 */
	public Dict() {
		try {
			rafDict = new RandomAccessFile(DICTFILE, "r");
		} catch (FileNotFoundException fnfe) {
			System.out.println("dict file does not exist.");
			fnfe.printStackTrace();
			System.exit(1);
		}
		try {
			rafIndex = new RandomAccessFile(INDEXFILE, "rw");
		} catch (FileNotFoundException fnfe) {
			System.out.println("Could not open file \'" 
					+ DICTFILE.toString() 
					+ "\" for writing.");
			fnfe.printStackTrace();
			System.exit(1);
		}
		try {
			if (0 == rafDict.length()) {
				System.out.println("I refuse to work with zero-length dictionary files!") ;
				System.exit(1) ;
			}
			if (0 == rafIndex.length()) {
				System.out.println("I refuse to work with zero-length index files.  Rebuilding index...") ;
				if ( ! buildIndex() ) {
					System.out.println("Oh, screw it.  I can't index that.  I quit.");
					System.exit(1) ;
				}
			}
			// we don't need this variable, but I can't resist.
			long dictLength = rafDict.length() ;
			rafIndex.seek(0) ;
			// rebuild index if length of header doesn't match length of... Dict.
			if (dictLength != ( (long) rafIndex.readInt())) {
				System.out.println("Dict length mismatch!  Rebuilding index...") ;
				// do you still wonder why there aren't more women in this field?
				if ( ! buildIndex() ) {
					System.out.println("Alas!  I could not build ze index!  I expire!") ;
					System.exit(1) ;
				}
			}
			// kill this next test once we're all confident and debugged
			rafIndex.seek(0) ;
			if (dictLength != ( (long) rafIndex.readInt())) {
				System.out.println("Something has gone horribly wrong...") ;
				System.out.println("dictLength: " + dictLength) ;
				rafIndex.seek(0) ;
				System.out.println("rafIndex.readInt() : " + rafIndex.readInt() ) ;
				System.exit(1) ;
			}
			numWords = (int) rafIndex.length() / 4 - 1 ;
		} catch (IOException e) {
			e.printStackTrace() ;
			System.exit(1) ;
		}
	}

	/**
	 * Just gets a totally random word from the dictionary file.
	 *
	 * @return A random word
	 */
	public String OldgetRandomWord() {
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
	 * Just gets a totally random word from the dictionary file.
	 *
	 * Now with hyper-index functioning!
	 *
	 * @return A random word
	 */
	public String getRandomWord() {
		return getWord( (int) (Math.random() * numWords) + 1 ) ;
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
	public boolean oldContains(String word) {
		word.toLowerCase();
		word.trim();
		//OK lets use a bsearch
		// TODO Make faster by cacheing data about file and using it to set an initial floor and ceiling. Investiagate, mibbie not worth it.
		long guessPos = DICTFILE.length() / 2;
		long ceiling = DICTFILE.length(), floor = 0, oldCeiling = 1, oldFloor = 2;
		try {
			do {
				rafDict.seek(guessPos);
				// the next two lines are screwing us up when we try to look up
				// a word that would come after the last word in the dict file.
				//
				// this should be reworked using the index-based functions
				// 
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
	
	public boolean contains(String wordparam) {
		String word = wordparam.toLowerCase();
		word.trim();
		//OK lets use a bsearch
		// TODO Make faster by cacheing data about file and using it to set an initial floor and ceiling. Investiagate, mibbie not worth it.
		int guessPos = numWords / 2;
		int ceiling = numWords, floor = 1, oldCeiling = 1, oldFloor = 2;
		do {
			// the next two lines are screwing us up when we try to look up
			// a word that would come after the last word in the dict file.
			//
			// this should be reworked using the index-based functions
			// 
			String guessWord = getWord(guessPos);
			guessWord = guessWord.toLowerCase();
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
	//	return false;
	}
	
	private int compare(String word1, String word2) {
		//made this case insensitive, to no purpose
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
		// don't need to test this, it's built into the constructor, if needed
		//System.out.println("\n\nBuilding index...\n\n");
		//dict.buildIndex() ;
		System.out.println("\n\nSmall index file check:\n\n");
		System.out.println("first word: " + dict.getWord(1) ) ;
		System.out.println("Word #666: " + dict.getWord(666)) ;
		System.out.println("indexed words: " + dict.numWords ) ;
		System.out.println("last word : " + dict.getWord( dict.numWords )) ;
		System.out.println("trying to produce an error by requesting word #" + (int) (dict.numWords + 1) + " : ") ;
		System.out.println( dict.getWord(dict.numWords + 1)) ;
		System.out.println("this should get another error, requesting word #0 (i.e., before #1) :") ;
		System.out.println( dict.getWord(0) ) ;

		System.out.println("\nDone.") ;
	}

	protected void finalize() throws Throwable {
		try {
			rafDict.close();
			rafIndex.close();
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
	
	/** (re)Builds the word index.  Note raw word file should not exceed INT.MAX_VALUE bytes
	 *
	 * @return True if index is successfully built
	 */
	private boolean buildIndex() {

		try {
			rafDict.seek(0) ;
			rafIndex.seek(0) ;	
			//write the 'header' of the index
			//length of file
			rafIndex.writeInt( (int) rafDict.length() ) ;
			int count = 0 ;
			while ( ( (int) rafDict.getFilePointer() < Integer.MAX_VALUE) 
					&& ( rafDict.getFilePointer() < rafDict.length() ) ) {
				rafIndex.writeInt( (int) rafDict.getFilePointer() ) ;
				rafDict.readLine() ;
				++count ;
			}
			rafIndex.setLength( rafIndex.getFilePointer() ) ;
			System.out.println("indexed " + count + " words.") ;
			return true ;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	return false;
	}

	/** get nth word in word file, using index file
	 *
	 * @param num word number
	 * @return String
	 */

	public String getWord(int num) {
		if ( num > numWords ) {
			//complain
			System.out.println("I don't have " + num + " words!") ;
			// the java-like thing to do here is probably to throw 
			// an exception, but that just seems excessive.  Also,
			// I don't know how to monkey with exceptions yet.
			return null ;
		} else if (num < 1) {
			System.out.println("word number must be 1 or greater, and no more than " + numWords) ;
			return null ;
		}
		try {
			rafIndex.seek((long) (num*4)) ;
			rafDict.seek((long) rafIndex.readInt()) ;
			return rafDict.readLine() ;
		} catch (IOException e) {
			e.printStackTrace() ;
			System.exit(1) ;
		}
		return null ;
	}
}
