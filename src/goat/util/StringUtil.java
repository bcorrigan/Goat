package goat.util;

public class StringUtil {
	public static boolean stringInArray(String s, String [] array) {
		boolean found = false ;
        for (String anArray : array) {
            if (s.equalsIgnoreCase(anArray)) {
                found = true;
                break;
            }
        }
        return found ;
	}
}
