package goat.module;

import goat.core.Constants;
import goat.core.Module;
import goat.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Title: JRelaxTimer<br>
 * Description:
 * JUnitConv is an universal Units of Measure Converter, it converts numbers
 * from one unit of measure to another.
 * Built as a Java Applet, JUnitConv is platform-independent and highly-configurable,
 * it supports an unlimited number of Units Categories, Units of Measure and Multiplier
 * Prefixes that could be customized using external text files. You could setup your
 * own data files using your preferred spoken language, units categories, units
 * definitions and multiplier prefixes. The default configuration data files contains
 * 580 basic units of measure definitions divided in 31 categories and 27 multiplier
 * prefixes for a total of 15660 composed units.
 *
 * <br/>Copyright (c) 2002-2006 Tecnick.com S.r.l (www.tecnick.com) Via Ugo
 * Foscolo n.19 - 09045 Quartu Sant'Elena (CA) - ITALY - www.tecnick.com -
 * info@tecnick.com <br/>
 * Project homepage: <a href="http://junitconv.sourceforge.net" target="_blank">http://junitconv.sourceforge.net</a><br/>
 * License: http://www.gnu.org/copyleft/gpl.html GPL 2
 *
 * @author Nicola Asuni [www.tecnick.com].
 * @version 1.0.003
 */
public class UnitConverter extends Module {
	/**
	 * Charset encoding.
	 */
    private String p_encoding;

	/**
	 * HTML page encoding.
	 */
    private String p_page_encoding;

	/**
	 * URL of text data file containing labels definitions.
	 */
    private String p_labels_data_file;

	/**
	 * URL of text data file containing multiplier definitions.
	 */
    private String p_multiplier_data_file;

	/**
	 * URL of text data file containing units categories data.
	 */
    private String p_category_data_file;

	/**
	 * URL of text data file containing units data.
	 */
    private String p_unit_data_file;

	/**
	 * Array of string labels.
	 */
    private String[] p_label;

	/**
	 * Array of category names.
	 */
    private String[] p_category_name;

	/**
	 * Array of Multiple/Submultiple names.
	 */
    private String[] p_multiplier_name;

	/**
	 * Array of Multiple/Submultiple values.
	 */
    private Double[] p_multiplier_value;

	/**
	 * Array of Multiple/Submultiple descriptions.
	 */
    private String[] p_multiplier_description;

	/**
	 * Array of category ID (link to category table: p_category_id).
	 */
    private Integer[] p_unit_category_id;

	/**
	 * Array of unit of measure symbols.
	 */
    private String[] p_unit_symbol;

	/**
	 * Array of unit of measure names.
	 */
    private String[] p_unit_name;

	/**
	 * Array of unit of measure descriptions.
	 */
    private String[] p_unit_description;

	/**
	 * Array of unit of measure conversion scale factors.
	 */
    private Double[] p_unit_scale;

	/**
	 * Array of unit of measure conversion offsets.
	 */
    private Double[] p_unit_offset;

	/**
	 * Array of powers to apply to unit multipliers.
	 */
    private Double[] p_unit_power;

	/**
	 * Current unit offset.
	 */
	private int current_unit_offset = 0; //offset for unit index on selectors

    public UnitConverter() {
        getParameters();
    }

    /**
	 * Convert string to specified encoding.
	 * @param original original string
	 * @param encoding_in input encoding table
	 * @param encoding_out output encoding table
	 * @return encoded string
	 */
	private String getEncodedString(String original, String encoding_in, String encoding_out) {
		String encoded_string;
		if (encoding_in.compareTo(encoding_out) != 0) {
			byte[] encoded_bytes;
			try {
				encoded_bytes = original.getBytes(encoding_in);
			}
			catch (UnsupportedEncodingException e) {
				System.out.println("Unsupported Charset: " + encoding_in);
				return original;
			}
			try {
				encoded_string = new String(encoded_bytes, encoding_out);
				return encoded_string;
			}
			catch (UnsupportedEncodingException e) {
				//e.printStackTrace();
				System.out.println("Unsupported Charset: " + encoding_out);
				return original;
			}
		}
		return original;
	}

	/**
	 * Return "def" if "str" is null or empty.
	 * @param str value to return if not null
	 * @param def default value to return
	 * @return def or str by case
	 */
	private String getDefaultValue(String str, String def) {
		if ( (str != null) && (str.length() > 0)) {
			return str;
		}
		return def;
	}


	/**
	 * Get the applet parameters from HTML page.
	 */
    private void getParameters() {
		try {
			p_encoding = "ISO-8859-1";
			p_page_encoding = "utf-8";
			p_labels_data_file = "resources/units/labels.txt";
			p_multiplier_data_file = "resources/units/muldata.txt";
			p_category_data_file = "resources/units/catdata.txt";
			p_unit_data_file = "resources/units/unitdata.txt";
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		readDataFile(0, p_multiplier_data_file); //read data from external text file
		readDataFile(1, p_category_data_file); //read data from external text file
		readDataFile(2, p_unit_data_file); //read data from external text file
		readDataFile(3, p_labels_data_file); //read data from external text file
	}

	/**
	 * set arrays size for unit categories
	 * @param i size of array
	 */
	private void setMultipliersArraySize(int i) {
		p_multiplier_name = new String[i];
		p_multiplier_value = new Double[i];
		p_multiplier_description = new String[i];
	}

	/**
	 * set arrays size for unit categories
	 * @param i size of array
	 */
	private void setCategoriesArraySize(int i) {
		p_category_name = new String[i];
	}

	/**
	 * set arrays size for units
	 * @param i size of array
	 */
	private void setUnitsArraySize(int i) {
		p_unit_category_id = new Integer[i];
		p_unit_symbol = new String[i];
		p_unit_name = new String[i];
		p_unit_description = new String[i];
		p_unit_scale = new Double[i];
		p_unit_offset = new Double[i];
		p_unit_power = new Double[i];
	}



    /**
	 * Read menu items data from external text file
	 * "\n" separate items
	 * "\t" separate values
	 * @param filetype 0=multiplier file, 1= category file, 2=units file
	 * @param filename the text file containing menu data
	 */
    private void readDataFile(int filetype, String filename) {
		int nfields=7; //number of data fields
		int i = 0; //temp elements counter
		int num_elements; //number of items (lines)
		String dataline;
		String[] elementdata;
		try {
			File filesource = new File(filename);
			//open data file
			BufferedReader in = new BufferedReader(new InputStreamReader(filesource.toURI().toURL().openStream()));
			//count elements
			while(null != (dataline = in.readLine())) {
				i++;
			}
			in.close();
			num_elements = i;
			//num_elements = i+1;
			//set arrays size by case
			if (filetype == 0) {
				setMultipliersArraySize(num_elements);
				nfields = 3;
			}
			else if (filetype == 1) {
				setCategoriesArraySize(num_elements);
				nfields = 1;
			}
			else if (filetype == 2) {
				setUnitsArraySize(num_elements);
				nfields = 7;
			}
			else if (filetype == 3) {
				p_label = new String[i];
				nfields = 1;
			}

			i = 0;
			in = new BufferedReader(new InputStreamReader(filesource.toURI().toURL().openStream()));
			//read lines (each line is one menu element)
			while(null != (dataline = in.readLine())) {
				//get element data array
				elementdata = StringUtil.splitData(dataline, '\t', nfields);
				//assign data
				if (filetype == 0) { //multipliers file
					p_multiplier_name[i] = getEncodedString(getDefaultValue(elementdata[0], ""), p_page_encoding, p_encoding);
					p_multiplier_description[i] = getEncodedString(getDefaultValue(elementdata[1], ""), p_page_encoding, p_encoding);
					p_multiplier_value[i] = parseNumber(getDefaultValue(elementdata[2], "1"));
				}
				else if (filetype == 1) { //category file
					p_category_name[i] = getEncodedString(getDefaultValue(elementdata[0], ""), p_page_encoding, p_encoding);
				}
				else if (filetype == 2) { //units file
					p_unit_category_id[i] = new Integer(elementdata[0]);
					p_unit_symbol[i] = getEncodedString(getDefaultValue(elementdata[1], ""), p_page_encoding, p_encoding);
					p_unit_name[i] = getEncodedString(getDefaultValue(elementdata[2], ""), p_page_encoding, p_encoding);
					p_unit_scale[i] = parseNumber(getDefaultValue(elementdata[3], "1"));
					p_unit_offset[i] = parseNumber(getDefaultValue(elementdata[4], "0"));
					p_unit_power[i] = parseNumber(getDefaultValue(elementdata[5], "1"));
					p_unit_description[i] = getEncodedString(getDefaultValue(elementdata[6], ""), p_page_encoding, p_encoding);
				}
				else if (filetype == 3) { //labels file
					p_label[i] = getEncodedString(getDefaultValue(elementdata[0], ""), p_page_encoding, p_encoding);
				}
				i++;
			}
			in.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get current selected index on selectors
	 */
	private String doCalculation(double in_value, int in_multiplier, int out_multiplier, int in_unit, int out_unit ) {

        int current_in_unit = current_unit_offset + in_unit;
        int current_out_unit = current_unit_offset + out_unit;

        //make conversion here ....
		Double intempvalue = in_value * p_unit_scale[current_in_unit] * Math.pow(p_multiplier_value[in_multiplier], p_unit_power[current_in_unit]);
		Double outtempvalue = p_unit_scale[current_out_unit];
		Double outvalue = ((intempvalue + p_unit_offset[current_in_unit] - p_unit_offset[current_out_unit]) / (outtempvalue * Math.pow(p_multiplier_value[out_multiplier], p_unit_power[current_out_unit])));
		return Double.toString( roundNumber(outvalue,10) );
    }


	/**
	 * return a rounded number
	 * @param in_number numer to round
	 * @param precision max decimal numbers
	 * @return rounded number
	 */
	private double roundNumber(double in_number, int precision) {
		double round_precision = Math.pow(10, (double) precision);
		return Math.round(in_number * round_precision) / round_precision;
	}


	/**
	 * simple number parser (allows to use math operators operators: +,-,*,/,^,P=PI,X=exp)
	 * operator precedence: P X * / + - ^
	 * @param num string to parse
	 * @return Double parsed number
	 */
	private Double parseNumber(String num) {
		Double tempnum = (double) 0;
		int opos; //operator position
		if ((num == null) || (num.length() < 1) ) {
			return tempnum;
		}

		//replace constants with their value
		while (num.indexOf("P") >= 0) { //PI constant
			String[] numparts = StringUtil.splitData(num, 'P', 2);
			num = numparts[0]+String.valueOf(Math.PI)+numparts[1];
		}
		while (num.indexOf("X") >= 0) { //e constant
			String[] numparts = StringUtil.splitData(num, 'X', 2);
			num = numparts[0]+String.valueOf(Math.E)+numparts[1];
		}

		if (num.indexOf("^") >= 0) { //allows to specify powers (e.g.: 2^10)
			String[] numparts = StringUtil.splitData(num, '^', 2);
			tempnum = Math.pow(parseNumber(numparts[0]), parseNumber(numparts[1]));
		}
		else if ( ((opos = num.indexOf("-")) > 0) && (num.charAt(opos-1) != 'E') && (num.charAt(opos-1) != '^')) {
			String[] numparts = StringUtil.splitData(num, '-', 2);
			tempnum = parseNumber(numparts[0]) - parseNumber(numparts[1]);
		}
		else if ( ((opos = num.indexOf("+")) > 0) && (num.charAt(opos-1) != 'E') && (num.charAt(opos-1) != '^')) {
			String[] numparts = StringUtil.splitData(num, '+', 2);
			tempnum = parseNumber(numparts[0]) + parseNumber(numparts[1]);
		}
		else if (num.indexOf("/") >= 0) {
			String[] numparts = StringUtil.splitData(num, '/', 2);
			tempnum = parseNumber(numparts[0]) / parseNumber(numparts[1]);
		}
		else if (num.indexOf("*") >= 0) {
			String[] numparts = StringUtil.splitData(num, '*', 2);
			tempnum = parseNumber(numparts[0]) * parseNumber(numparts[1]);
		}
		else {
			tempnum = Double.valueOf(num);
		}

		return tempnum;
	}


    public void processPrivateMessage(Message m) {
        processChannelMessage(m);
    }

    public void processChannelMessage(Message m) {
        if( m.getModCommand().toLowerCase().equals("convert")) {
            Pattern p = Pattern.compile(".*\\d+([,\\d]+)?(\\.\\d+)? (.+?) to (.*)");
            Matcher matcher = p.matcher(m.getModTrailing());
            if(matcher.matches()) {
                String[] args = m.getModTrailing().toLowerCase().split(" ");
                String fromArg = matcher.group(3);
                String toArg = matcher.group(4);
                Double valueArg = Double.parseDouble(args[0]);

                //now we split to and from args into their multiplier component and
                //unit component and populate appropriate arguments to conversion method.
                int multiplierFrom = extractMultiplier(fromArg);
                int multiplierTo = extractMultiplier(toArg);
                fromArg = fromArg.replaceFirst( p_multiplier_description[multiplierFrom].split(" ")[0], "");
                toArg = toArg.replaceFirst( p_multiplier_description[multiplierTo].split(" ")[0], "");

                int unitFrom = extractUnit(fromArg);
                int unitTo = extractUnit(toArg);
                if( !checkUnit(unitFrom, fromArg, m) || !checkUnit(unitTo, toArg, m) )
                    return;
                if( !checkCategories(unitFrom, unitTo, m) )
                    return;

                String result = doCalculation( valueArg, multiplierFrom, multiplierTo, unitFrom, unitTo);
                m.reply(valueArg + " " + p_multiplier_description[multiplierFrom].split(" ")[0] + p_unit_symbol[unitFrom] + " = " + result + " " + p_multiplier_description[multiplierTo].split(" ")[0] + p_unit_symbol[unitTo]);
            }
        } else if( m.getModCommand().toLowerCase().equals("describeunit")) {
            String unitArg = m.getModTrailing();
            int unit = extractUnit(unitArg);
            if(!checkUnit(unit,unitArg,m))
                return;
            m.pagedReply( p_category_name[ p_unit_category_id[unit] ] +": " + p_unit_description[unit]);
        } else if( m.getModCommand().toLowerCase().equals("findunit")) {
            String searchTerm = m.getModTrailing();
            String results = "";
            boolean categoryUsed = false;
            int lastCategory = -1;
            for( int i=0; i<p_unit_name.length; i++ ) {
                if( lastCategory!=p_unit_category_id[i]) {
                    categoryUsed=false;
                    lastCategory=p_unit_category_id[i];
                }

                if( p_unit_name[i].toLowerCase().contains(searchTerm.toLowerCase().trim())) {
                    if( !categoryUsed ) {
                        //TODO Why is leading bold not noticed at start of message?
                        results += " " + Constants.BOLD + p_category_name[ p_unit_category_id[i] ] + Constants.NORMAL + ": ";
                        results += " " + p_unit_name[i];
                        categoryUsed = true;
                    } else {
                        results += ", " + p_unit_name[i];
                    }
                }
            }
            if(results.length()>0)
                m.pagedReply(results);
            else m.reply("No matching units found.");
        }
    }

    private boolean checkUnit(int unit, String arg, Message m) {
        if( unit==-1 ) {
            //if it looks like message might be intended for currency converter, don't error spam!
            if (!m.getModTrailing().matches(".*\\d+([,\\d]+)?(\\.\\d+)? [a-z]{3} to [a-z]{3}.*"))
                m.reply("Unit not found: " + arg);
            return false;
        }
        return true;
    }

    private boolean checkCategories(int unitFrom, int unitTo, Message m) {
        if( !p_unit_category_id[unitFrom].equals(p_unit_category_id[unitTo]) ) {
            m.reply("You can't convert from " + p_category_name[ p_unit_category_id[unitFrom] ] + " to " + p_category_name[ p_unit_category_id[unitTo] ] + ", silly.");
            return false;
        }
        return true;
    }

    private int extractMultiplier(String arg) {
        for( int i=0; i<p_multiplier_description.length; i++) {
            if(p_multiplier_description[i].split(" ")[0].equals(""))
                continue;

            if(arg.startsWith( p_multiplier_description[i].split(" ")[0] )) {
                return i;
            }
        }
        //not found - but that's OK, it just means we don't use a multiplier so return 0
        return 0;
    }

    private int extractUnit(String arg) {
        for( int i=0; i<p_unit_name.length; i++) {
            if(p_unit_name[i].equals(""))
                continue;
            if( p_unit_name[i].toLowerCase().trim().equals(arg.toLowerCase().trim())) {
                return i;
            }
        }
        return -1;
    }

    public String[] getCommands() {
		return new String[]{"convert", "describeunit", "findunit"};
	}
}


