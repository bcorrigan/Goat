package goat.util;

import static org.junit.Assert.*;

import org.junit.*;
import org.junit.Test;

import static goat.util.StringUtil.*;
import static goat.core.Constants.*;

import java.util.Date;
import java.util.Calendar;

public class StringUtilTest {

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
    public void testGetPositionFromMapsLink() {
        String url =  "http://maps.google.co.uk/maps?f=q&source=s_q&hl=en&geocode=&q=g42+8ed&sll=53.800651,-4.064941&sspn=31.06929,38.847656&ie=UTF8&hq=&hnear=Glasgow,+Lanarkshire+G42+8ED,+United+Kingdom&ll=55.835395,-4.264814&spn=0.0031,0.005729&t=h&z=18";
        double[] pos = getPositionFromMapsLink(url);
        assertEquals("We should get back array with two positions", 2, pos.length);
        assertEquals("Latitude should match",55.835395,pos[0], 0);
        assertEquals("Longitude should match",-4.264814,pos[1],0);
    }

}
