package goat.util;

import java.util.ArrayList;
import java.util.Iterator;

import static goat.util.CurrencyConverter.*;
import junit.framework.*;

public class TestCurrencyConverter extends TestCase {
	
	public void testConverter() {
		try {
			ArrayList<String> currencies = listCurrencies();
			Iterator<String> it = currencies.iterator();
			while(it.hasNext()) {
				String currency = it.next();
				System.out.println("1.0 GBP = " + currency + " " + convert(1.0, "GBP", currency));
			}
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
