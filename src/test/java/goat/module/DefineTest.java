package goat.module;

import goat.util.UrbanDictionary;


public class DefineTest {


	public static void testUrban() {
		testUrbanWord("manky") ;
		testUrbanWord("chav");
		testUrbanWord("wuffie");
	}

	private static void testUrbanWord(String word) {
		Define defMod = new Define() ;
		defMod.debug = true ;
		UrbanDictionary ud = new UrbanDictionary(word);
		System.out.printf("\nDefinitions found for %s: %d\n\n", word, ud.definitions.size()) ;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
