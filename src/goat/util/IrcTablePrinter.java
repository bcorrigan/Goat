package goat.util;

import goat.core.Constants;
import static goat.util.Scores.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IrcTablePrinter {
	
	enum Decorator {
		BOLD,
		UNDERLINE,
		REVERSE,
	}
	
	public List<String> printArrays(String[][] values) {
		return null;
	}
	
	public List<String> printArrays(List<String>[] values) {
		return null;
	}
	
	public List<String> printLists(List<List<String>> values) {
		return null;
	}
	
	public List<String> printMaps(List<Map<String, String>> values) {
		return null;
	}
	
	public void setHeaders(String[] headers) {
		
	}
	
	public void setHeaders(List<String> headers) {
		
	}
	
	public void setHeaders(Map<String, String> headers) {
		
	}
	
	public void unsetHeaders() {
		
	}
	
	public void setDisplayWidth(int widthInCharacters) {
		
	}
	
	public void setColumnPadding(int numSpaces) {
		
	}
	
	public void setColumnSeparator(String separator) {
		
	}
	
	public void setFieldPadding(String separator) {
		
	}
	
	public void setFieldSeparator(String separator) {
		
	}
	
	
	{
		for (int i = 0; i < 20; i++) {
			SPACES[i] = " ";
			for (int j = 0; j < i - 1; j++) {
				SPACES[i] += " ";
			}
		}
	}
	
	public List<String> scoreTable(List<String[]> scores) {
		int top;
		int largestNick = 0;
		int largesthScore = 0;
		int largestsScore = 0;
		List<String> ret = new ArrayList<String>();
		if (scores.size() < 20)
			top = scores.size();
		else
			top = 20;

		if (top == 0) {
			ret.add("Nobody's got any scores yet :(");
			return ret;
		}

		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) scores.get(i);
			if (entry[NAME].length() > largestNick)
				largestNick = entry[0].length();
			if (entry[HIGHEST_SCORE].length() > largesthScore)
				largesthScore = entry[2].length();
			if (entry[TOTAL_SCORE].length() > largestsScore)
				largestsScore = entry[1].length();
		}

		ret.add("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
				+ "HiScore" + SPACES[largesthScore + 7 - 7]
				+ "TotalScore");
		for (int i = 0; i < top; i++) {
			String[] entry = (String[]) scores.get(i);
			String is = Integer.toString(i + 1);
			ret.add(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + entry[NAME] +
					SPACES[largestNick + 3 - entry[NAME].length()] +
					entry[HIGHEST_SCORE] + SPACES[largesthScore + 7 - entry[HIGHEST_SCORE].length()] + entry[TOTAL_SCORE]);
		}
		return ret;
	}

	public List<String> matchScoreTable(List<String[]> scores) {
		int top;
		int largestNick = 0;
		int largestsScore = 0;
		ArrayList<String> ret = new ArrayList<String>();
		synchronized (scores) {
			if (scores.size() < 20)
				top = scores.size();
			else
				top = 20;

			if (top == 0) {
				ret.add("Nobody has won a match yet :(");
				return ret;
			}

			for (int i = 0; i < top; i++) {
				String[] entry = (String[]) scores.get(i);
				if (entry[NAME].length() > largestNick)
					largestNick = entry[NAME].length();
				if (entry[TOTAL_SCORE].length() > largestsScore)
					largestsScore = entry[TOTAL_SCORE].length();
			}

			ret.add("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
			                                                            + "Matches Won");
			for (int i = 0; i < top; i++) {
				String[] entry = (String[]) scores.get(i);
				String is = Integer.toString(i + 1);
				ret.add(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + entry[NAME] +
						SPACES[largestNick + 3 - entry[NAME].length()] + entry[TOTAL_SCORE]);
			}
		}
		return ret;
	}
}
