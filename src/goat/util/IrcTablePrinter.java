package goat.util;

import goat.core.Constants;
//import static goat.util.Scores.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


/**
 * Widget to produce pleasant-looking tables via IRC.
 * 
 * Note that throughout this class, "row" and "column" refer to rows and columns of the display, not rows and columns of data.
 * 
 * Data is referred to using the terms "record" and "field."
 * 
 * The print methods are massively inefficient as they stand, quite a bit of optimization can be done if it's ever needed.
 * 
 * @author rs
 *
 */
public class IrcTablePrinter {
	
	private int displayWidth = 64;
	
	private String columnSeparator = " \u2551 ";
	private String fieldSeparator = " ";
	private String headerColumnSeparator = " \u2565 ";
	private String headerFieldSeparator = " ";
	
	private List<String> headers = new ArrayList<String>();
	
	private List<EnumSet<Decorator>> fieldDecorators = new ArrayList<EnumSet<Decorator>>();
	private List<EnumSet<Decorator>> headerDecorators = new ArrayList<EnumSet<Decorator>>();
	
	private EnumSet<Decorator> defaultFieldDecoration = EnumSet.noneOf(Decorator.class);
	private EnumSet<Decorator> defaultHeaderDecoration = EnumSet.of(Decorator.UNDERLINE, Decorator.LEFT_JUSTIFY);
	
	private boolean enumerate = false;
	private String enumerationHeader = "#";
	private EnumSet<Decorator> enumerationDecoration = EnumSet.of(Decorator.BOLD);
	private EnumSet<Decorator> enumerationHeaderDecoration = EnumSet.of(Decorator.UNDERLINE);
	private int enumerationStart = 1;
	
	
	private int maxColumns = 0;
	
	public static enum Decorator {
		BOLD (Constants.BOLD),
		UNDERLINE (Constants.UNDERLINE),
		REVERSE (Constants.REVERSE),
		
		NORMAL (Constants.NORMAL),
		
		COLOR (Constants.COLCODE),
		LEFT_JUSTIFY ("-");
		
		private String code;
		private Decorator(String code) {
			this.code = code;
		}
		
		public String code() {
			return code;
		}
	}
	

	
	public List<String> printArrays(String[][] records) {
		List<List<String>> wrapper = new ArrayList<List<String>>(records.length);
		for(int i=0;i<records.length;i++)
			if(records[i] != null)
				wrapper.add(i, Arrays.asList(records[i]));
			else
				wrapper.add(i, new ArrayList<String>());
		return printArrays(wrapper);
	}
	
	public List<String> printArrays(List<List<String>> records) {
		List<String> ret = new ArrayList<String>();
		if(records.size() < 1)
			return ret;
		int cols = getMaxColumns(records);

		for(int r = 0; r < getNumRows(records, cols); r++)
			ret.add(printRow(r, records, cols));
		if(ret.size() > 0 // don't add headers if we've got no data
				&& headers != null 
				&& headers.size() != 0)
			ret.add(0, printHeaders(records, cols));
		return ret;	
	}
	
	private String printHeaders(List<List<String>> records, int columns) {
		String ret = "";
		String colSep = "";
		String fieldSep = "";
		//ugliness ahead...
		if (columnSeparator != null)
			if(headerColumnSeparator != null && ! headerColumnSeparator.equals("") 
				&& visibleLength(headerColumnSeparator) == visibleLength(columnSeparator))
				colSep = headerColumnSeparator;
			else
				colSep = columnSeparator;
		if (fieldSeparator != null)
			if(headerFieldSeparator != null && ! headerFieldSeparator.equals("") 
				&& visibleLength(headerFieldSeparator) == visibleLength(fieldSeparator))
				fieldSep = headerFieldSeparator;
			else
				fieldSep = fieldSeparator;
		if(headerDecorators == null)
			headerDecorators = new ArrayList<EnumSet<Decorator>>();
		if(headerDecorators.size() == 0) {
			int numFields = getMinFieldWidthsInColumn(records, 0, 1).size();
			for(int i=0; i < numFields; i++)
				headerDecorators.add(defaultHeaderDecoration);
		}
		for(int c = 0; c < columns; c++) {
			List<Integer> fieldWidths = getMinFieldWidthsInColumn(records, c, columns);
			if(enumerate) {
				ret += enumerationHeader(records, c, columns);
				ret += headerFieldSeparator;
			}
			for(int i=0; i < fieldWidths.size(); i++) {
				if(fieldWidths.get(i) != 0) {
					EnumSet<Decorator> decoration = defaultHeaderDecoration;
					if (headerDecorators.size() > i && headerDecorators.get(i) != null)
						decoration = headerDecorators.get(i);
					String formatString = "%";
					if(decoration.contains(Decorator.LEFT_JUSTIFY))
						formatString += Decorator.LEFT_JUSTIFY.code();
					
					String header = "";
					if(headers.size() > i && headers.get(i) != null) 
						header += headers.get(i);
					
					int extraSpaces = header.length() - visibleLength(header);
					formatString += (fieldWidths.get(i) + extraSpaces) + "s";
					boolean decorated = extraSpaces != 0;
					for(Decorator deco: EnumSet.of(Decorator.BOLD, Decorator.REVERSE, Decorator.UNDERLINE))
						if(decoration.contains(deco)) {
							ret += deco.code();
							decorated = true;
						}
					ret += String.format(formatString, header);
					if(decorated)
						ret += Decorator.NORMAL.code();
					if(i < fieldWidths.size() - 1)
						ret += fieldSep;
				}
			}
			if (c < columns - 1)
				ret += colSep;
		}
		return ret;
	}
	
	private String printRow(int rowIndex, List<List<String>> records,  int columns) {
		String row = "";	
		for(int c = 0; c < columns; c++) {
			List<Integer> fieldWidths = getMinFieldWidthsInColumn(records, c, columns);
			int fields = getMaxNumFieldsInColumn(records, c, columns);
			int r = firstIndexInColumn(records, c, columns) + rowIndex;
			int last = lastIndexInColumn(records, c, columns);
			if(r < records.size()) {
				if (r <= last) {
					if(enumerate) {
						int enWidth = Math.max(Integer.toString(last + enumerationStart).length(), enumerationHeader.length());
						row += printEnumeration(r, enWidth);
						row += fieldSeparator;
					}
					for(int f=0; f < fields; f++) {
						EnumSet<Decorator> decoration = defaultFieldDecoration;
						if(fieldDecorators.size() > f && fieldDecorators.get(f) != null)
							decoration = fieldDecorators.get(f);
						String formatString = "%";
						if(decoration.contains(Decorator.LEFT_JUSTIFY))
							formatString += Decorator.LEFT_JUSTIFY.code();
						
						String dataString = "";
						if(records.get(r) != null 
								&& f < records.get(r).size() 
								&& records.get(r).get(f) != null)
							dataString = records.get(r).get(f);
						int extraSpaces = dataString.length() - visibleLength(dataString);
						formatString += (fieldWidths.get(f) + extraSpaces) + "s";
						boolean decorated = extraSpaces != 0;
						for(Decorator deco: EnumSet.of(Decorator.BOLD, Decorator.REVERSE, Decorator.UNDERLINE))
							if(decoration.contains(deco)) {
								row += deco.code();
								decorated = true;
							}
						row += String.format(formatString, dataString);
						if(decorated)
							row += Decorator.NORMAL.code();
						if(f < fields - 1)
							row += fieldSeparator;
					}
				} else {
					// pad with empty space
					int spaces = 0;
					for(int width: fieldWidths)
						spaces += width;
					spaces += fieldWidths.size() - 1;
					if(enumerate) {
						spaces += Integer.toString(last+enumerationStart).length() + fieldSeparator.length();
					}
					row += String.format("%" + spaces + "s","");					
				}
			}
			if(c < columns - 1 )
				row += columnSeparator;
		}
		return row;
	}
	
	
	
	private List<Integer> getColumnWidths(List<List<String>> records, int columns) {
		List<Integer> ret = new ArrayList<Integer>(columns);
		if(records.size() < 1)
			return ret;  // empty input, move along
		for(int col = 0; col < columns; col++) {
			int numFields = getMaxNumFieldsInColumn(records, col, columns);
			if(numFields < 1)
				continue;
			List<Integer> fieldLengths = getMinFieldWidthsInColumn(records, col, columns);
			for(int f=0; f<numFields; f++)
				if(ret.size() > col)
					ret.set(col, ret.get(col) + fieldLengths.get(f));
				else
					ret.add(col, fieldLengths.get(f));
			ret.set(col, ret.get(col) + (numFields - 1) * fieldSeparator.length());
			if(enumerate) 
				ret.set(col, ret.get(col) + lastIndexInColumn(records, col, columns)/10 + 1 + fieldSeparator.length());
		}
		return ret;
	}
	
	private int getMaxNumFieldsInRange(List<List<String>> records, int startIndex, int endIndex) {
		int ret = 0;
		if(startIndex >= records.size() || endIndex < startIndex)
			return ret;
		if(endIndex >= records.size()) 
			endIndex = records.size() - 1;
		for(int i = startIndex; i <= endIndex; i++)
			if(records.get(i) != null)
				ret = Math.max(records.get(i).size(), ret);
		return ret;
	}
	
	private int getMaxNumFieldsInColumn(List<List<String>> records, int colIndex, int numCols) {
		return getMaxNumFieldsInRange(records, firstIndexInColumn(records, colIndex, numCols), lastIndexInColumn(records, colIndex, numCols));
	}
	
	private int getMaxColumns(List<List<String>> records) {
		
		int maxCol = maxColumns;
		if(0 == maxCol || maxCol > records.size())
			maxCol = records.size();
		if(maxCol < 2)
			return maxCol; // no need to bother calculating if cols has been capped at 1, or if we've got an empty records array
		int ret = 1;
		for(int i=2; i <= maxCol; i++) {
			List<Integer> colWidths = getColumnWidths(records, i);
			int width = 0;
			for(int w: colWidths)
				width += w;
			width += (i - 1) * columnSeparator.length();
			if(width <= displayWidth)
				ret = i;
		}
		return ret;
	}
	
	private int getNumRows(List<List<String>> records, int numCols) {
		int ret = 0;
		if(records.size() > 0 && numCols > 0) {
			ret = records.size() / numCols;
			if (records.size() % numCols != 0)
				ret++;
		}
		return ret;
	}
	
	private List<Integer> getMinFieldWidthsInColumn(List<List<String>> records, int columnIndex, int numCols){
		int lastInColumn = lastIndexInColumn(records, columnIndex, numCols);
		int firstInColumn = firstIndexInColumn(records, columnIndex, numCols);
		
		int numFields = getMaxNumFieldsInColumn(records, columnIndex, numCols); 

		List<Integer> ret = new ArrayList<Integer>(numFields);  // initialized to 0
		for (int record = firstInColumn; record <= lastInColumn; record++)
			for(int field=0; field < numFields; field++)
				if(records.get(record)!= null 
						&& records.get(record).size() > field 
						&& records.get(record).get(field) != null) {
					int len = visibleLength(records.get(record).get(field));
					if(headers != null && headers.size() > field && headers.get(field) != null)
						len = Math.max(len, visibleLength(headers.get(field)));
					if(ret.size() > field)
						ret.set(field, Math.max(len, ret.get(field)));
					else
						ret.add(field, len);
				}
		return ret;
	}
	
	private int firstIndexInColumn(List<List<String>> records, int columnIndex, int numCols) {
		return (records.size() / numCols) * columnIndex + Math.min(records.size() % numCols, columnIndex);
	}
	
	private int lastIndexInColumn(List<List<String>> records, int columnIndex, int numCols) {
		return firstIndexInColumn(records, columnIndex + 1, numCols) - 1 ;
	}
	
	/*
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
			String[] record = (String[]) scores.get(i);
			if (record[NAME].length() > largestNick)
				largestNick = record[0].length();
			if (record[HIGHEST_SCORE].length() > largesthScore)
				largesthScore = record[2].length();
			if (record[TOTAL_SCORE].length() > largestsScore)
				largestsScore = record[1].length();
		}

		ret.add("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
				+ "HiScore" + SPACES[largesthScore + 7 - 7]
				+ "TotalScore");
		for (int i = 0; i < top; i++) {
			String[] record = (String[]) scores.get(i);
			String is = Integer.toString(i + 1);
			ret.add(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + record[NAME] +
					SPACES[largestNick + 3 - record[NAME].length()] +
					record[HIGHEST_SCORE] + SPACES[largesthScore + 7 - record[HIGHEST_SCORE].length()] + record[TOTAL_SCORE]);
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
				String[] record = (String[]) scores.get(i);
				if (record[NAME].length() > largestNick)
					largestNick = record[NAME].length();
				if (record[TOTAL_SCORE].length() > largestsScore)
					largestsScore = record[TOTAL_SCORE].length();
			}

			ret.add("   " + Constants.UNDERLINE + "Name" + SPACES[largestNick + 3 - 4]
			                                                            + "Matches Won");
			for (int i = 0; i < top; i++) {
				String[] record = (String[]) scores.get(i);
				String is = Integer.toString(i + 1);
				ret.add(Constants.BOLD + is + Constants.BOLD + SPACES[3 - is.length()] + record[NAME] +
						SPACES[largestNick + 3 - record[NAME].length()] + record[TOTAL_SCORE]);
			}
		}
		return ret;
	}
	*/
	
	private String enumerationHeader(List<List<String>> records, int c, int columns) {
		String ret = "";
		if(enumerate) {
			int digits = Integer.toString(lastIndexInColumn(records, c, columns) + enumerationStart).length();
			int width = Math.max(digits, enumerationHeader.length());
			String formatStr = "%";
			if (null == enumerationHeaderDecoration)
				enumerationHeaderDecoration = defaultHeaderDecoration;
			if(enumerationHeaderDecoration.contains(Decorator.LEFT_JUSTIFY))
				formatStr += Decorator.LEFT_JUSTIFY.code();
			formatStr += width + "s";
			boolean decorated = false;
			for(Decorator deco: EnumSet.range(Decorator.BOLD, Decorator.REVERSE))
				if(enumerationHeaderDecoration.contains(deco)) {
					ret += deco.code();
					decorated = true;
				}
			ret += String.format(formatStr, enumerationHeader);
			if(decorated)
				ret += Decorator.NORMAL.code();
		}
		return ret;
	}
	
	private String printEnumeration(int num, int width) {
		String ret = "";
		if(enumerate) {
			EnumSet<Decorator> decoration = enumerationDecoration;
			if(decoration == null)
				decoration = EnumSet.noneOf(Decorator.class);
			String formatString  = "%";
			if(decoration.contains(Decorator.LEFT_JUSTIFY))
				formatString += Decorator.LEFT_JUSTIFY.code();
			formatString += width + "d";
			boolean decorated = false;
			for(Decorator deco: EnumSet.range(Decorator.BOLD, Decorator.REVERSE))
				if(decoration.contains(deco)) {
					ret += deco.code();
					decorated = true;
				}
			ret += String.format(formatString, num + enumerationStart);
			if(decorated)
				ret += Decorator.NORMAL.code();
		}
		return ret;
	}
	
	private int visibleLength(String str) {
		int ret = 0;
		for(int i=0;i<str.length();i++) {
			String remaining = str.substring(i);
			if(remaining.startsWith(Decorator.BOLD.code()))
				continue;
			else if(remaining.startsWith(Decorator.UNDERLINE.code()))
				continue;
			else if(remaining.startsWith(Decorator.REVERSE.code()))
				continue;
			else if(remaining.startsWith(Decorator.NORMAL.code()))
				continue;
			else if(remaining.startsWith(Decorator.COLOR.code())) {
				//handle colour stuff
				if (remaining.matches(Decorator.COLOR.code() + "[\\d,].*")) {
					for(int j=1;j<3;j++) // skip past one or two digits indicating foreground colour
						if(remaining.length() > j && Character.isDigit(remaining.charAt(j)))
							i++;
						else
							break;
					remaining = str.substring(i);
					if(remaining.length() > 1 && ',' == remaining.charAt(1)) {
						i++;
						remaining = str.substring(i);
						for(int j=1;j<3;j++) // skip past one or two digits indicating background colour
							if(remaining.length() > j && Character.isDigit(remaining.charAt(j)))
								i++;
							else
								break;
					}
				} 
				else
					continue;
			} 
			else
				ret++;  // we have a visible character
		}
		return ret;
	}
	
	public void setHeaders(String[] headers) {
		this.headers = new ArrayList<String>(Arrays.asList(headers));
	}
	
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}
	
	public void unsetHeaders() {
		if(headers != null)
			headers.clear();
	}
	
	public void setDisplayWidth(int widthInCharacters) {
		displayWidth = widthInCharacters;
	}
	
	public void setColumnSeparator(String separator) {
		columnSeparator = separator;
	}
	
	public void setFieldSeparator(String separator) {
		fieldSeparator = separator;
	}
	
	public void setMaxColumns(int maxColumns) {
		this.maxColumns = maxColumns;
	}

	public int getMaxColumns() {
		return maxColumns;
	}

	public String getHeaderColumnSeparator() {
		return headerColumnSeparator;
	}

	public void setHeaderColumnSeparator(String headerColumnSeparator) {
		this.headerColumnSeparator = headerColumnSeparator;
	}

	public String getHeaderFieldSeparator() {
		return headerFieldSeparator;
	}

	public void setHeaderFieldSeparator(String headerFieldSeparator) {
		this.headerFieldSeparator = headerFieldSeparator;
	}

	public List<EnumSet<Decorator>> getFieldDecorators() {
		return fieldDecorators;
	}

	public void setFieldDecorators(List<EnumSet<Decorator>> fieldDecorators) {
		this.fieldDecorators = fieldDecorators;
	}

	public void setFieldDecorator(int fieldIndex, EnumSet<Decorator> decoration) {
		if(fieldDecorators == null)
			fieldDecorators = new ArrayList<EnumSet<Decorator>>();
		if(fieldIndex < 0)
			return;
		if(fieldIndex < fieldDecorators.size())
			fieldDecorators.set(fieldIndex, decoration);
		else {
 			for(int i = fieldDecorators.size(); i < fieldIndex; i++)
				fieldDecorators.add(null);
 			fieldDecorators.add(decoration);
		}
	}
	
	public List<EnumSet<Decorator>> getHeaderDecorators() {
		return headerDecorators;
	}

	public void setHeaderDecorators(List<EnumSet<Decorator>> headerDecorators) {
		this.headerDecorators = headerDecorators;
	}

	public EnumSet<Decorator> getDefaultFieldDecoration() {
		return defaultFieldDecoration;
	}

	public void setDefaultFieldDecoration(EnumSet<Decorator> defaultFieldDecoration) {
		this.defaultFieldDecoration = defaultFieldDecoration;
	}

	public EnumSet<Decorator> getDefaultHeaderDecoration() {
		return defaultHeaderDecoration;
	}

	public void setDefaultHeaderDecoration(
			EnumSet<Decorator> defaultHeaderDecoration) {
		this.defaultHeaderDecoration = defaultHeaderDecoration;
	}

	public int getDisplayWidth() {
		return displayWidth;
	}

	public String getColumnSeparator() {
		return columnSeparator;
	}

	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public boolean isEnumerate() {
		return enumerate;
	}

	public void setEnumerate(boolean enumerate) {
		this.enumerate = enumerate;
	}

	public String getEnumerationHeader() {
		return enumerationHeader;
	}

	public void setEnumerationHeader(String enumerationHeader) {
		this.enumerationHeader = enumerationHeader;
	}

	public EnumSet<Decorator> getEnumerationDecoration() {
		return enumerationDecoration;
	}

	public void setEnumerationDecoration(EnumSet<Decorator> enumerationDecoration) {
		this.enumerationDecoration = enumerationDecoration;
	}

	public EnumSet<Decorator> getEnumerationHeaderDecoration() {
		return enumerationHeaderDecoration;
	}

	public void setEnumerationHeaderDecoration(
			EnumSet<Decorator> enumerationHeaderDecoration) {
		this.enumerationHeaderDecoration = enumerationHeaderDecoration;
	}

	public int getEnumerationStart() {
		return enumerationStart;
	}

	public void setEnumerationStart(int enumerationStart) {
		this.enumerationStart = enumerationStart;
	}
}
