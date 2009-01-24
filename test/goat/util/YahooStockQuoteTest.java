package goat.util;

import junit.framework.TestCase;
import java.util.ArrayList;
import goat.util.YahooStockQuote;

public class YahooStockQuoteTest extends TestCase {

	public void testQuote() {
		try {
			ArrayList<YahooStockQuote> quotes = YahooStockQuote.getQuotes("^dji aapl ^ftse ^n225 scoxq.pk scox fstmx ^dwc");
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
