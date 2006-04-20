package goat.module;

import goat.GoatTest;
import goat.module.Define;
import java.util.Vector;

public class DefineTest extends GoatTest {

	
	public static void testUrban() {
		testUrbanWord("manky") ;
		testUrbanWord("chav");
		testUrbanWord("wuffie");
	}
	
	private static void testUrbanWord(String word) {
		Define defMod = new Define() ;
		defMod.debug = true ;
		Vector v = defMod.getUrbanDefinitions(word) ;
		assertTrue(v.size() != 0) ;
		System.out.printf("\nDefinitions found for %s: %d\n\n", word, v.size()) ;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
