package goat.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static goat.util.StringUtil.*;
import static goat.core.Constants.*;

import java.util.Date;
import java.util.Calendar;

public class TestStringUtil {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testDurationString() {
		long interval = 6*HOUR+4*MINUTE+4*SECOND;
		assertEquals("6 hours, 4 minutes and 4 seconds",durationString(interval));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,cal.get(Calendar.YEAR)-3);
		cal.set(Calendar.SECOND,cal.get(Calendar.SECOND)-8);
		interval = System.currentTimeMillis() - cal.getTimeInMillis();
		assertEquals("3 years and 8 seconds",durationString(interval));
	}

	@Test
	public void testVvshortDurationString() {
		long interval = 6*HOUR+4*MINUTE+4*SECOND;
		assertEquals("6h4m",vvshortDurationString(interval));
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR,cal.get(Calendar.YEAR)-3);
		cal.set(Calendar.SECOND,cal.get(Calendar.SECOND)-8);
		interval = System.currentTimeMillis() - cal.getTimeInMillis();
		assertEquals("3Y8s",vvshortDurationString(interval));
	}

	@Test
	public void testVshortDurationString() {
		//fail("Not yet implemented");
	}

	@Test
	public void testShortDurationString() {
		//fail("Not yet implemented");
	}

}
