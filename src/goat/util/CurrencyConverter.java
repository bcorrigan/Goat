package goat.util;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeMap;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * Converts various currencies.
 *
 * @author Erik C. Thauvin
 * @version $Revision: 1.4 $, $Date: 2004/09/28 01:36:34 $
 *          <p/>
 *          Feb 11, 2004
 * @since 1.0
 */
public class CurrencyConverter {

	/**
	 * The exchange rates.
	 */
	public static final TreeMap<String, String> EXCHANGE_RATES = new TreeMap<String, String>();
	/**
	 * The exchange rates table URL.
	 */
	private static final String EXCHANGE_TABLE_URL = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml";
	/**
	 * The last exchange rates table publication date.
	 */
	public static String s_date = "";
	
	public static Double convert(Double amount, String fromCurrency, String toCurrency) throws NullPointerException, NumberFormatException {
		final double from = Double.parseDouble((String) EXCHANGE_RATES.get(fromCurrency.toUpperCase()));
		final double to = Double.parseDouble((String) EXCHANGE_RATES.get(toCurrency.toUpperCase()));
	
		return amount * to / from ;
	}
	
	public static void updateRates() throws JDOMException, IOException {
//	    try {
	        final SAXBuilder builder = new SAXBuilder();
	        builder.setIgnoringElementContentWhitespace(true);
	
	        final Document doc = builder.build(new URL(EXCHANGE_TABLE_URL));
	        final Element root = doc.getRootElement();
	        final Namespace ns = root.getNamespace("");
	        final Element cubeRoot = root.getChild("Cube", ns);
	        final Element cubeTime = cubeRoot.getChild("Cube", ns);
	        s_date = cubeTime.getAttribute("time").getValue();
	        final List<Element> cubes = cubeTime.getChildren();
	        Element cube;	
	        for (Object cube1 : cubes) {
	            cube = (Element) cube1;
	            EXCHANGE_RATES.put(cube.getAttribute("currency").getValue(), cube.getAttribute("rate").getValue());
	        }
	        EXCHANGE_RATES.put("EUR", "1");
	//    } catch (JDOMException e) {
	//        m.createReply("An error has occurred while parsing the exchange rates table.").send();
	//        e.printStackTrace();
	//    } catch (IOException e) {
	//        m.createReply("An error has occurred while fetching the exchange rates table:  " + e.getMessage()).send();
	//        e.printStackTrace();
	//    }
	}
	
	public static boolean isRecognizedCurrency(String currency) throws JDOMException, IOException {
		boolean ret = false;
		ArrayList<String> currencies = listCurrencies();
		Iterator<String> it = currencies.iterator();
		while (it.hasNext())
			if(it.next().equalsIgnoreCase(currency)) {
				ret = true;
				break;
			}
		return ret;
	}
	
	public static ArrayList<String> listCurrencies() throws JDOMException, IOException {
		if (EXCHANGE_RATES.isEmpty())
			updateRates();
		return new ArrayList<String>(EXCHANGE_RATES.keySet());
	}
	
	public static String rateTableDate() throws JDOMException, IOException {
		if(EXCHANGE_RATES.isEmpty())
			updateRates();
		return s_date;
	}
	
	public static String todaysDate() {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		cal.setTime(new Date());
		String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
		if ((cal.get(Calendar.MONTH) + 1) < 10)
			month = "0" + month;
		String day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
		if ((cal.get(Calendar.DAY_OF_MONTH)) < 10)
			day = "0" + day;   	
		return cal.get(Calendar.YEAR) + "-" + month + "-" + day;
	}

}
