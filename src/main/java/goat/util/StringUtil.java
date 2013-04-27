package goat.util;

import goat.core.BotStats;
import goat.core.Users;
import goat.core.User;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DurationFormatUtils;

import static goat.core.Constants.*;

public class StringUtil {

    private static Random random = new Random();

    public static boolean stringInArray(String s, String[] array) {
        boolean found = false;
        for (String anArray : array) {
            if (s.equalsIgnoreCase(anArray)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static String capitalise(String in) {
        if (in.length() < 1) {
            return in;
        } else if (1 == in.length()) {
            return in.toUpperCase();
        } else {
            return in.substring(0, 1).toUpperCase() + in.substring(1);
        }
    }

    public static String pickRandom(String[] strings) {
        return strings[random.nextInt(strings.length)];
    }

    public static String durationString(long intervalInMillis) {
        return durationString(intervalInMillis, false, false);
    }

    /**
     * Abbreviated version & most significant two only = eg 2h 3m
     * @param duration
     * @return
     */
    public static String vvshortDurationString(long intervalInMillis) {
        return durationString(intervalInMillis, true, true);
    }

    /**
     * Abbreviated version = eg 1d 2h 3m 5s
     * @param duration
     * @return
     */
    public static String vshortDurationString(long intervalInMillis) {
        return durationString(intervalInMillis, true, false);
    }

    /**
     * Just return the two most significant units.
     * @param intervalInMillis
     * @return
     */
    public static String shortDurationString(long intervalInMillis) {
        return durationString(intervalInMillis, false, true);
    }

    private static String durationString(long intervalInMillis, boolean _short, boolean significantOnly) {
    	long startMillis = System.currentTimeMillis() - intervalInMillis;
    	//just express years as before now
    	String formatted = DurationFormatUtils.formatPeriod(startMillis, System.currentTimeMillis(), "y:M:d:H:m:s");
    	String[] periods = formatted.split(":");
    	assert(periods.length==6);
        final String YEAR_STR, MONTH_STR, DAY_STR, HOUR_STR, MINUTE_STR, SECOND_STR;
        if (_short) {
            YEAR_STR = "Y";
            MONTH_STR = "M";
            DAY_STR = "d";
            HOUR_STR = "h";
            MINUTE_STR = "m";
            SECOND_STR = "s";
        } else {
            YEAR_STR = " year";
            MONTH_STR = " month";
            DAY_STR = " day";
            HOUR_STR = " hour";
            MINUTE_STR = " minute";
            SECOND_STR = " second";
        }

        String durparts[] = new String[]{
            periods[0] + YEAR_STR,
            periods[1] + MONTH_STR,
            periods[2] + DAY_STR,
            periods[3] + HOUR_STR,
            periods[4] + MINUTE_STR,
            periods[5] + SECOND_STR};
        String durString = "less than one second";
        int partsCount = 0;

        Pattern pattern = Pattern.compile("\\d*([^0-9]).*");

        for (int i = 0; i < durparts.length; i++) {
            if (Character.isDigit(durparts[i].charAt(0))) {
                Matcher matcher = pattern.matcher(durparts[i]);//start();
                matcher.matches();
                int endNum = durparts[i].indexOf(matcher.group(1));
                int num = new Integer(durparts[i].substring(0, endNum));
                if (num == 0) {
                    durparts[i] = null;
                } else {
                    partsCount++;
                }
                if (num > 1 && !_short) {
                    durparts[i] += "s";
                }
            } else {
                durparts[i] = null;
            }
        }
        if (partsCount > 0) {
            String temp[] = new String[partsCount];
            int tempIndex = 0;
            for (int i = 0; i < durparts.length; i++) {
                if (durparts[i] != null) {
                    temp[tempIndex++] = durparts[i];
                }
            }
            if (temp.length == 1) {
                durString = temp[0];
            } else {
                durString = "";
                int ind;
                if (significantOnly) {
                    ind = 2;
                } else {
                    ind = temp.length;
                }
                for (int i = 0; i < ind; i++) {
                    durString += temp[i];
                    if (i != ind - 1 && !_short) {
                        if (i == ind - 2) {
                            durString += " and ";
                        } else {
                            durString += ", ";
                        }
                    }
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
        for (int i = 0; i < input_string.length(); i++) {
            if (input_string.charAt(i) == sep_ch) { //separator found
                splitted_array[element_num] = str1; //put string to array
                str1 = ""; //reinitialize variable
                element_num++; //count strings
                if (element_num >= size) {
                    break; //quit if limit is reached
                }
            } else {
                str1 += input_string.charAt(i);
            }
        }
        //get last element
        if (element_num < size) {
            splitted_array[element_num] = str1; //put string to vector
            element_num++;
        }
        //fill remaining values with empty string
        for (int i = element_num; i < size; i++) {
            splitted_array[i] = "";
        }
        return splitted_array;
    }

    public static String quotedList(ArrayList<String> strings) {
        String ret = "";
        Iterator<String> i = strings.iterator();
        if (i.hasNext()) {
            ret += "\"" + i.next() + "\"";
        }
        while (i.hasNext()) {
            ret += ", \"" + i.next() + "\"";
        }
        return ret;
    }

    public static String timeString(String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(System.currentTimeMillis());
        int hour = cal.get(GregorianCalendar.HOUR);
        if (hour == 0) {
            hour = 12;
        }
        String ret = hour + ":";
        ret += String.format("%02d", cal.get(GregorianCalendar.MINUTE));
        if (cal.get(GregorianCalendar.AM_PM) == GregorianCalendar.AM) {
            ret += "am";
        } else {
            ret += "pm";
        }
        switch (cal.get(GregorianCalendar.DAY_OF_WEEK)) {
            case GregorianCalendar.SUNDAY:
                ret += ", Sunday";
                break;
            case GregorianCalendar.MONDAY:
                ret += ", Monday";
                break;
            case GregorianCalendar.TUESDAY:
                ret += ", Tuesday";
                break;
            case GregorianCalendar.WEDNESDAY:
                ret += ", Wednesday";
                break;
            case GregorianCalendar.THURSDAY:
                ret += ", Thursday";
                break;
            case GregorianCalendar.FRIDAY:
                ret += ", Friday";
                break;
            case GregorianCalendar.SATURDAY:
                ret += ", Saturday";
                break;
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
        s = removeColors(s);
        s = removeFormatting(s);
        return s;
    }

    /*
     * Convenience method to spare our sensitive typing fingers
     */
    public static String scrub(String s) {
        return removeFormattingAndColors(s).trim();
    }

    //think this should be more optimised than the apache commons version cos it aborts
    //when the difference grows past the limit.
	public static int levlim(String s, String t, int limit) {
		int n = s.length() + 1;
		int m = t.length() + 1;
		if (m == 1)
			return n - 1;
		if (n == 1)
			return m -1;
		int[] d = new int[m * n];
		for (int i = 0; i < n; i++)
			d[i] = i;
		int k = n;
		for (int i = 1; i < m; i++) {
			d[k] = i;
			k += n;
		}
		int f = 0, g = 0, h = 0, min = 0, b = 0, c = 0, best = 0, cost = 0;
		for (int i = 1; i < n; i++) {
			k = i;
			f = 0;
			best = limit;
			for (int j = 1; j < m; j++) {
				h = k;
				k += n;
				min = d[h] + 1;
				b = d[k - 1] + 1;
				if (g < s.length() && f < t.length())
					cost = s.charAt(g) == t.charAt(f)?0:1;
				else
					cost = 1;
				c = d[h - 1] + cost;
				if (b < min)
					min = b;
				if (c < min)
					min = c;
				d[k] = min;
				/*
				System.out.println("i=" + i + ", j=" + j);
				for (int v = 0; v < m; v++)
				{
					for (int w = 0; w < n; w++)
						System.out.print(d[v * n + w] + " ");
					System.out.println();
				}
				*/
				if (min < best)
					best = min;
				f = j;
			}
			if (best >= limit)
				return limit;
			g = i;
		}
		if (d[k] >= limit)
			return limit;
		else
			return d[k];
	}

	public static boolean isValidIRCNick (String nick) {
		return nick.matches("[a-zA-Z0-9^{}\\[\\]`\\\\^_-|]+");
	}

    private static Charset goatCharset = BotStats.getInstance().getCharset();
    /**
     * Calculate the actual length in bytes of the supplied string, in goat's output encoding.
     * Slashnet limits messages by the byte, not by char length, so in these unicode heavy days
     * there can be a large variance.
     * @param str
     * @return
     */
    public static int byteLength(String str) {
    	return str.getBytes(goatCharset).length;
    }

    /**
     * Given a string, find the largest substring of it that fits the number of bytes supplied
     * Then return that substring
     * @param s
     * @param maxBytes
     * @return
     */
    public static String maxEncodeable(String s, int maxBytes)
    throws CharacterCodingException {
        CharBuffer sBuf = CharBuffer.wrap(s);
        goatCharset.newEncoder()
            .encode(sBuf, ByteBuffer.wrap(new byte[maxBytes]), true);
        return s.substring(0, sBuf.position());
    }

    /**
     * Returns the string one after the supplied string
     * @param str
     * @return
     */
	public static String incString(String str) {
		return str.substring(0, str.length()-1)
						+(char) (str.substring(
						str.length()-1).charAt(0)+1);
	}
}
