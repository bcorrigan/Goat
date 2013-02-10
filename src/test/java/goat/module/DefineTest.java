package goat.module;

import goat.module.Define;
import java.util.Vector;
import java.net.*;

public class DefineTest {

	
	public static void testUrban() {
		testUrbanWord("manky") ;
		testUrbanWord("chav");
		testUrbanWord("wuffie");
	}
	
	private static void testUrbanWord(String word) {
		Define defMod = new Define() ;
		defMod.debug = true ;
		Vector v;
		try {
			v = defMod.getUrbanDefinitions(word) ;
		} catch (SocketTimeoutException e) {
			System.out.println("Timed out while trying to fetch urbandictionary derinition") ;
			return ;
		}
		//assertTrue(v.size() != 0) ;
		System.out.printf("\nDefinitions found for %s: %d\n\n", word, v.size()) ;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
