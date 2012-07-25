package goat.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

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
	public static TreeMap<String, String> exchangeRates = new TreeMap<String, String>();
	/**
	 * The exchange rates table URL.
	 */
	private static final String EXCHANGE_TABLE_URL = "http://www.ecb.int/stats/eurofxref/eurofxref-daily.xml";
	
	/**
	 * time stamp for our last exchange rates table retrieval
	 */
	private static long lastRatesUpdate = 0;
	
	private static final long minWaitBetweenRateUpdates = 1000*60*30; // 30 minutes 
	
	/**
	 * The last exchange rates table publication date.
	 */
	public static String exchangeRatesPublicationDate = "";
	
	public static Double convert(Double amount, String fromCurrency, String toCurrency) throws NullPointerException, NumberFormatException {
		final double from = Double.parseDouble((String) exchangeRates.get(fromCurrency.toUpperCase()));
		final double to = Double.parseDouble((String) exchangeRates.get(toCurrency.toUpperCase()));
	
		return amount * to / from ;
	}
	
	public static void updateRates() throws IOException, ParserConfigurationException, SAXException {
		
			if (!exchangeRates.isEmpty() && ((new Date()).getTime() - lastRatesUpdate < minWaitBetweenRateUpdates))
				return;  // don't update if we've updated recently
			
			String newPublicationDate = "";
			TreeMap<String, String> newRates = new TreeMap<String, String>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        final DocumentBuilder builder = dbf.newDocumentBuilder();
	        //builder.setIgnoringElementContentWhitespace(true);
	
	        final Document doc = builder.parse(EXCHANGE_TABLE_URL);
	        final Element root = doc.getDocumentElement();
	        NodeList nl = root.getElementsByTagName("Cube");
	        final Element cubeRoot = (Element) nl.item(0);
	        nl = cubeRoot.getElementsByTagName("Cube");
	        final Element cubeTime = (Element) nl.item(0);
	        newPublicationDate = cubeTime.getAttribute("time");
	        final NodeList cubes = cubeTime.getElementsByTagName("Cube");
	        Element cube;	
	        for (int i=0; i < cubes.getLength(); i++) {
	            cube = (Element) cubes.item(i);
	            newRates.put(cube.getAttribute("currency"), cube.getAttribute("rate"));
	        }
	        newRates.put("EUR", "1");
	        exchangeRates = newRates;
	        exchangeRatesPublicationDate = newPublicationDate;
	        lastRatesUpdate = (new Date()).getTime();
	}
	
	public static boolean isRecognizedCurrency(String currency) throws IOException, ParserConfigurationException, SAXException {
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
	
	public static ArrayList<String> listCurrencies() throws IOException, ParserConfigurationException, SAXException {
		if (exchangeRates.isEmpty())
			updateRates();
		return new ArrayList<String>(exchangeRates.keySet());
	}
	
	public static String rateTableDate() throws IOException, ParserConfigurationException, SAXException {
		if(exchangeRates.isEmpty())
			updateRates();
		return exchangeRatesPublicationDate;
	}
	
	public static String todaysRateDate() {
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

	public static String translateCurrencyAliases(String input) {
		String ret = input;
        ret = ret.replaceAll("(?i)gay( money)?", "EUR");
        ret = ret.replaceAll("(?i)real money", "USD");
        ret = ret.replaceAll("(?i)proper money", "GBP");
        ret = ret.replaceAll("(?i)blood money", "ILS");
        ret = ret.replaceAll("(?i)oil money", "USD");
        ret = ret.replaceAll("(?i)dirty money", "USD");
        ret = ret.replaceAll("(?i)eddie money", "USD");
        ret = ret.replaceAll("(?i)monopoly money", "CAD");
        ret = ret.replaceAll("(?i)tubgirl( money)?", "JPY");
        ret = ret.replaceAll("(?i)soccer( money)?", "BRL");
        ret = ret.replaceAll("(?i)yellow( money)?", "CNY");
        ret = ret.replaceAll("(?i)nokias?", "NOK");
        ret = ret.replaceAll("(?i)dinero", "MXN");
        ret = ret.replaceAll("(?i)pounds", "GBP");
        ret = ret.replaceAll("(?i)pound", "GBP");
        ret = ret.replaceAll("(?i)yen", "JPY");
        ret = ret.replaceAll("(?i)dollars?", "NZD");
        ret = ret.replaceAll("(?i)bucks?", "USD");
        ret = ret.replaceAll("(?i)quid?", "GBP");
        ret = ret.replaceAll("(?i)loonies?", "CAD");
        ret = ret.replaceAll("(?i)communism", "RUB");
        ret = ret.replaceAll("(?i)jews?( money)?", "ILS");
        ret = ret.replaceAll("(?i)kiwis?( money)?", "NZD");
        ret = ret.replaceAll("(?i)hindoos?( money)?", "INR");

		return ret;
	}
}
