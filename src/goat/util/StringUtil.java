package goat.util;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.TimeZone;
import static goat.core.Constants.*;

public class StringUtil {
	private static Random random = new Random() ;
	
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
	
	public static String capitalise(String in) {
		if(in.length() < 1)
			return in;
		else if (1 == in.length())
			return in.toUpperCase() ;
		else
			return in.substring(0, 1).toUpperCase() + in.substring(1) ;
	}
	
	public static String pickRandom(String[] strings) {
		return strings[random.nextInt(strings.length)] ;
	}


    public static String durationString(long intervalInMillis) {
        return durationString(intervalInMillis, false);
    }

    /**
     * Just return the two most significant units.
     * @param intervalInMillis
     * @return
     */
    public static String shortDurationString(long intervalInMillis) {
        return durationString(intervalInMillis, true);
    }
	
	private static String durationString(long intervalInMillis, boolean _short) {
        String durparts[] = new String[] {
            intervalInMillis / YEAR + " year",
            (intervalInMillis / MONTH) % 12 + " month",
            (intervalInMillis / DAY) % (MONTH / DAY) + " day",
            (intervalInMillis / HOUR) % 24 + " hour",
            (intervalInMillis / MINUTE) % 60 + " minute",
            (intervalInMillis / SECOND) % 60 + " second"};
		String durString = "less than one second";
		int partsCount = 0;
		for(int i=0; i<durparts.length; i++) {
			if(Character.isDigit(durparts[i].charAt(0))) {
				int endNum = durparts[i].indexOf(" ");
				int num = new Integer(durparts[i].substring(0,endNum));
				if(num == 0)
					durparts[i] = null;
				else
					partsCount++;
				if (num > 1)
					durparts[i] += "s";
			}
			else
				durparts[i] = null;
		}
		if (partsCount > 0) {
			String temp[] = new String[partsCount];
			int tempIndex = 0;
			for(int i=0; i<durparts.length; i++)
				if(durparts[i] != null)
					temp[tempIndex++] = durparts[i];
			if(temp.length == 1) {
				durString = temp[0];
			} else {
				durString = "";
                int ind;
                if(_short)
                    ind=2;
                else ind=temp.length;
				for(int i=0; i<ind; i++) {
					durString += temp[i];
					if(i != ind - 1)
						if(i == ind - 2)
							durString += " and ";
						else
							durString += ", ";
				}
			}
		}
		return durString;
	}
	
	/**
	 * Split a string in array of predefined size
	 * @param input_string string to split
	 * @param sep_ch separator character
	 * @param size max elements to retrieve, remaining elements will be filled with empty string
	 * @return splitted_array of strings
	 */
	public static String[] splitData(String input_string, char sep_ch, int size) {
		String str1 = ""; // temp var to contain found strings
		String splitted_array[] = new String[size]; // array of splitted string to return
		int element_num = 0; //number of found elements
		// analize string char by char
		for(int i=0; i<input_string.length(); i++) {
			if(input_string.charAt(i) == sep_ch) { //separator found
				splitted_array[element_num] = str1; //put string to array
				str1 = ""; //reinitialize variable
				element_num++; //count strings
				if (element_num >= size) {
					break; //quit if limit is reached
				}
			}
			else {
				str1 += input_string.charAt(i);
			}
		}
		//get last element
		if (element_num < size) {
			splitted_array[element_num] = str1; //put string to vector
			element_num++;
		}
		//fill remaining values with empty string
		for(int i=element_num; i<size; i++) {
			splitted_array[i] = "";
		}
		return splitted_array;
	}
	
	public static String quotedList(ArrayList<String> strings) {
		String ret = "" ;
		Iterator<String> i = strings.iterator();
		if (i.hasNext())
			ret += "\"" + i.next() + "\"";
		while (i.hasNext())
			ret += ", \"" + i.next() + "\"";
		return ret;
	}
	
	public static String timeString(String timezone) {
		TimeZone tz = TimeZone.getTimeZone(timezone);
		GregorianCalendar cal = new GregorianCalendar(tz);
		cal.setTimeInMillis(System.currentTimeMillis());
		int hour = cal.get(GregorianCalendar.HOUR);
		if (hour == 0)
			hour = 12;
		String ret = hour + ":";
		ret += String.format("%02d", cal.get(GregorianCalendar.MINUTE));
		if (cal.get(GregorianCalendar.AM_PM) == GregorianCalendar.AM)
			ret += "am";
		else
			ret += "pm";
		switch (cal.get(GregorianCalendar.DAY_OF_WEEK)) {
		case GregorianCalendar.SUNDAY : ret += ", Sunday"; break;
		case GregorianCalendar.MONDAY : ret += ", Monday"; break;
		case GregorianCalendar.TUESDAY : ret += ", Tuesday"; break;
		case GregorianCalendar.WEDNESDAY : ret += ", Wednesday"; break;
		case GregorianCalendar.THURSDAY : ret += ", Thursday"; break;
		case GregorianCalendar.FRIDAY : ret += ", Friday"; break;
		case GregorianCalendar.SATURDAY : ret += ", Saturday"; break;
		}
		ret += ", " + cal.get(GregorianCalendar.DAY_OF_MONTH) + "/";
		ret += (cal.get(GregorianCalendar.MONTH) + 1) + "/";
		ret += cal.get(GregorianCalendar.YEAR);
		ret += " (" + tz.getID() + " - " + tz.getDisplayName() + ")";
		return ret;
	}
	
	/**
	 * Removes all colours from a line of text. nicked from pircbot
	 */
	public static String removeColors(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer(length);
		int i = 0;
		while (i < length) {
			char ch = line.charAt(i);
			if (ch == '\u0003') {
				i++;
				// Skip "x" or "xy" (foreground color).
				if (i < length) {
					ch = line.charAt(i);
					if (Character.isDigit(ch)) {
						i++;
						if (i < length) {
							ch = line.charAt(i);
							if (Character.isDigit(ch)) {
								i++;
							}
						}
						// Now skip ",x" or ",xy" (background color).
						if (i < length) {
							ch = line.charAt(i);
							if (ch == ',') {
								i++;
								if (i < length) {
									ch = line.charAt(i);
									if (Character.isDigit(ch)) {
										i++;
										if (i < length) {
											ch = line.charAt(i);
											if (Character.isDigit(ch)) {
												i++;
											}
										}
									} else {
										// Keep the comma.
										i--;
									}
								} else {
									// Keep the comma.
									i--;
								}
							}
						}
					}
				}
			} else if (ch == '\u000f') {
				i++;
			} else {
				buffer.append(ch);
				i++;
			}
		}
		return buffer.toString();
	}
	/**
	 * Remove formatting from a line of IRC text. From pircbot
	 *
	 * @param line the input text.
	 * @return the same text, but without any bold, underlining, reverse, etc.
	 */
	public static String removeFormatting(String line) {
		int length = line.length();
		StringBuffer buffer = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			char ch = line.charAt(i);
			if (ch == '\u000f' || ch == '\u0002' || ch == '\u001f' || ch == '\u0016') {
				// Don't add this character.
			} else {
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}
	/**
	 * Removes all formatting and colours from a string.
	 */
	public static String removeFormattingAndColors(String s) {
		s = removeColors(s) ;
		s = removeFormatting(s) ;
		return s ;
	}
}
