package goat.util;

import static java.lang.Math.*;

/**
 * Class to do stuff with longitude and latitude.
 * 
 * All calculations are based on a perfectly spherical earth, and thus unsuitable for SCUBA diving, lobbing ICBMs hither and yon, or other activities where you need to worry about elevation, or where errors of up to 0.5% might screw the pooch.
 * 
 * @author RobotSlave
 *
 */
public class GeoCoordinates {
	
	/**
	 * Radius of the earth, in km.
	 */
	public static final double AVERAGE_EARTH_RADIUS = 6372.795 ; // in km
	public static final double KM_TO_MILES = 0.621371192 ;
	
	public static final char DEGREE_SYMBOL = '\u00B0' ;
	
	// various regexes ("Patterns", for those who have never ventured outside the java bubble) for various forms of coordinates
	public static final String METAR_LATITUDE_REGEX = "[0-9]+(-[0-9]+){0,2}[NS]" ;
	public static final String METAR_LONGITUDE_REGEX = "[0-9]+(-[0-9]+){0,2}[EW]" ;
	public static final String FLOAT_COORDINATES_REGEX = "[0-9.+-]+\\s*,{0,1}\\s*[0-9.+-]+";
	public static final String METAR_COORDINATES_REGEX = METAR_LATITUDE_REGEX + "\\s*,{0,1}\\s*" + METAR_LONGITUDE_REGEX;
	public static final String METAR_REVERSED_COORDINATES_REGEX = METAR_LONGITUDE_REGEX + "\\s*,{0,1}\\s*" + METAR_LATITUDE_REGEX;
	
	/**
	 * Great circle distance between 2 points, in km.
	 * 
	 * @param latitude1 latitude of first point, in degrees.
	 * @param longitude1 longitude of second point, in degrees.
	 * @param latitude2 latitude of second point, in degrees.
	 * @param longitude2 longitude of second point, in degrees.
	 * @return
	 */
	public static double sphericalDistance (double latitude1, double longitude1, double latitude2, double longitude2) {
		double t1 = toRadians(latitude1);
		double t2 = toRadians(latitude2);
		double gd = toRadians(longitude1 - longitude2);
		double numer = sqrt(pow(cos(t2)*sin(gd), 2) + pow(cos(t1)*sin(t2) - sin(t1)*cos(t2)*cos(gd), 2));
		double denom = sin(t1)*sin(t2) + cos(t1)*cos(t2)*cos(gd) ;
		return AVERAGE_EARTH_RADIUS * atan2(numer, denom) ;
	}
	

	
	/**
	 * Convert METAR encoded latitude and longitude to doubles.
	 * 
	 * If seconds are supplied in either coordinate, it is assumed both coordinates are accurate to the second.
	 * Margins of error are calculated based on a spherical earth.
	 * 
	 * @param latitude METAR encoded latitude:  DDD-MM-SSH or DDD-MMH, D is degrees, M is minutes, H is hemisphere (N|S)
	 * @param longitude METAR encoded longitude:  DDD-MM-SSH or DDD-MMH, D is degrees, M is minutes, H is hemisphere (E|W)
	 * @return array of doubles:
	 * <ol start=0>
	 * <li>latitude, in degrees</li>
	 * <li>longitude, in degrees</li>
	 * <li>latitude margin of error in degrees (this will be either 1/120 or 1/7200, depending on whether or not seconds were supplied, and is the same for latitude and longitude).</li>
	 * <li>longitude margin of error in degrees (this will always be the same as lat. margin of error).</li>
	 * <li>latitude margin of error in kilometers (This will be one of two values, ~0.927 or ~0.015, depending on whether seconds were supplied.)</li>
	 * <li>longitude margin of error in kilometers (as measured along the longest possible parallel, not along a great circle yielding the greatest possible error; this value will always be the greater of the two)</li>
	 * </ol>
	 */
	public static double[] metarToDoubles(String latitude, String longitude) {
		double[] ret = new double[6];
		boolean hasSeconds = false;
		if ((latitude.split("-", 0).length > 2) || longitude.split("-", 0).length >2)
			hasSeconds = true;
		ret[0] = metarToDouble(latitude);
		ret[1] = metarToDouble(longitude);
		ret[2] = (1.0 / 60.0) / 2.0;
		if (hasSeconds) {
			ret[2] = ret[2] / 60.0 ;
		}
		ret[3] = ret[2] ;
		ret[4] = degreesLatitudeToKilometers(ret[2]); 
		double fattestLat = min(0.0, abs(abs(ret[0]) - ret[2]));
		ret[5] = degreesLongitudeToKilometersParallel(ret[2], fattestLat);
		return ret;
	}
	
	/**
	 * Turn a string representing a METAR standard latitude or longitude into a Double.
	 * 
	 * @param coordinate string representing latitude or longitude, of the form DDD-MM-SSH where DDD is degrees, MM is minutes, SS is seconds, and H is one of "N", "S", "E", or "W" (case insensitive).  The -SS part is optional, that is, the string may be of the form "DD-MMH".
	 * @return a double representing the coordinate in degrees, where North and West are positive, South and East negative.  There is no indication in the returned value as to whether the coordinate is a longitude or a latitude.  No indication of margin of error is returned.
	 */
	public static double metarToDouble(String coord) {
		coord = coord.trim();
		double ret = 0.0;
		String[] parts = coord.split("-", 0) ;
		String dir = parts[parts.length - 1];
		dir = dir.substring(dir.length() - 1);
		String degrees = parts[0];
		String minutes = "";
		String seconds = "";
		if (parts.length > 2) {
			seconds = parts[2] ;
			seconds = seconds.substring(0, seconds.length() - 1);
		} 
		if(parts.length > 1) {
			minutes = parts[1];
			minutes = minutes.substring(0, minutes.length() - 1);
		}
		if(parts.length == 1)
			degrees = degrees.substring(0, degrees.length() - 1);
		ret = Double.parseDouble(degrees);
		if (! minutes.equals(""))
			ret += (Double.parseDouble(minutes) / 60.0) ;
		if(! seconds.equals(""))
			ret += Double.parseDouble(seconds) / 3600.0 ;
		if (dir.toUpperCase().equals("W") || dir.toUpperCase().equals("S"))
			ret = -ret;
		return ret;
	}
	
	public static double[] decimalCoordsToDoubles(String latitude, String longitude) {
		double[] ret = new double[6];
		ret[0] = Double.parseDouble(latitude);
		ret[1] = Double.parseDouble(longitude);
		ret[2] = 0.5 ; // we're assuming the coords supplied are accurate at least to the degree.
		ret[3] = 0.5 ;
		if (decimalPlaces(latitude) > 0)
			ret[2] = pow(10, - decimalPlaces(latitude)) / 2.0;
		if (decimalPlaces(longitude ) > 0)
			ret[3] = pow(10, - decimalPlaces(longitude)) / 2.0;
		ret[4] = degreesLatitudeToKilometers(ret[2]); 
		double fattestLat = abs(ret[0]) - ret[2];
		if (fattestLat < 0.0)
			fattestLat = 0.0;
		ret[5] = degreesLongitudeToKilometersParallel(ret[3], fattestLat);

		/*
		// debug
		System.out.println("latitude: " + ret[0]) ;
		System.out.println("longitude: " + ret[1]) ;
		System.out.println("latitude error: " + ret[2]) ;
		System.out.println("longitude error: " + ret[3]) ;
		System.out.println("latitude error, km: " + ret[4]) ;
		System.out.println("longitude error, km: " + ret[5]) ;
		*/
		
		return ret;
	}
	
	public static int decimalPlaces(String number) {
		int ret = 0;
		if (! number.matches("-{0,1}[0-9]+\\.[0-9]+"))
			return ret;
		
		ret = number.length() - 1 - number.indexOf('.');
		return ret;
	}
	
	public static double degreesLatitudeToKilometers(double latitudeDelta) {
		return latitudeDelta * AVERAGE_EARTH_RADIUS * 2.0 * PI / 360.0;
	}
	
	public static double degreesLongitudeToKilometersParallel(double longitudeDelta, double latitude) {
		return cos(toRadians(latitude)) * longitudeDelta * AVERAGE_EARTH_RADIUS * 2.0 * PI / 360.0;
	}
	
	public static double degreesLongitudeToKilometersGreatCircle(double longitudeDelta, double latitude) {
		// this formula should be simplified, but I'm lazy.
		return AVERAGE_EARTH_RADIUS * acos(pow(sin(toRadians(latitude)),2) + pow(cos(toRadians(latitude)),2)*cos(toRadians(longitudeDelta))) ;
	}

	public static String googleMapsURL(double latitude, double longitude) {
		return googleMapsURL(latitude, longitude, "", 0.0, 0.0) ;
	}
	
	public static String googleMapsURL(double latitude, double longitude, String message) {
		return googleMapsURL(latitude, longitude, message, 0.0, 0.0) ;
	}
	
	/**
	 * Create a google maps URL for the specified area.
	 * 
	 * The results of this are maybe not fully up to URL encoding standards.  Not good.
	 * It also assumes a google maps window 4 tiles wide by three tiles high.  Also not good.
	 * And it uses a sort of quirky bit of query syntax if you supply a message.
	 * 
	 * So, altogether, not very good.
	 * 
	 * @param latitude
	 * @param longitude
	 * @param message
	 * @param latError
	 * @param longError
	 * @return
	 */
	public static String googleMapsURL(double latitude, double longitude, String message, double latError, double longError) {
		String ret="http://google.com/maps?q=";
		int prec = 0;
		if (message.equals("")) {
			// using these lines will get you your coordinates in the info bubble.  And a longish ugly URL.
			ret += latitudeToMinutes(latitude, latError, true, false) + " ";
			ret += longitudeToMinutes(longitude, longError, true, false);
			ret = ret.replaceAll(" ", "+");
		} else {
			prec = liberalSignificantDigits(latError) ;
			ret += message + "@" + String.format("%."+prec+"f,",latitude, "UTF-8") ;
			prec = liberalSignificantDigits(longError) ;
			ret += String.format("%."+prec+"f",longitude) ;
		}
		if (latError != 0.0 || longError != 0.0) { 
			//lard it up with stuff to set zoom level
			/*
				//produces a URL that supposedly tells google to pick the best zoom level, but google isn't any better at picking a zoom level than we are, even though they know the resolution of their sat/aerial imagery for a given point, and we don't.
				prec = liberalSignificantDigits(latError);
				ret += "&ll=" + String.format("%." + prec + "f,", latitude);
				prec = liberalSignificantDigits(longError);
				ret += String.format("%." + prec + "f", longitude) ;
				double span = 2.0 * latError;
				prec = liberalSignificantDigits(latError);
				ret += String.format("&spn=%." + prec + "f,", span);
				span = 2.0 * longError;
				prec = liberalSignificantDigits(longError);
				ret += String.format("%." + prec + "f", span);
			 */
			int zl = pickGoogleZoomLevel(latitude, latError, longError, 3, 4) ;
			if (zl != -1)
				ret += "&z=" + 	zl;			
		}
		return ret;
	}
	
	// subject to change at Google's whim.
	public static final int MAX_GOOGLE_MAPS_ZOOM_LEVEL = 18;
	
	/**
	 * Find an appropriate google maps zoom level for a given area.
	 * 
	 * If you just want the maximum zoom level, you might want to just leave the "&z=" parameter out of your URL entirely, and let Google set the zoom level to max.  If that's not an option, use MAX_GOOGLE_MAPS_ZOOM_LEVEL instaed of calling this method.
	 * 
	 * @param latitudeCenter latitude at the center of the area you want to display, in degrees
	 * @param latitudeSpan span of the area you want to display, in degrees, must be positive.  abs(latitudeCenter) + latitudeSpan/2 must be &lt; 90, otherwise 0 will be returned.
	 * @param longitudeSpan span of the area you want to display, in degrees
	 * @return
	 */

	public static int pickGoogleZoomLevel(double latitudeCenter, double latitudeSpan, double longitudeSpan, int vertMapTiles, int horizMapTiles) {
		int ret = 0;
		if (latitudeSpan == 0.0 && longitudeSpan == 0.0)
			return MAX_GOOGLE_MAPS_ZOOM_LEVEL;
		latitudeCenter = abs(latitudeCenter) ;
		//make sure the window is on the map before moving on
		if (latitudeSpan > 0 && longitudeSpan > 0 && longitudeSpan < 360 &&
				latitudeCenter + latitudeSpan/2.0 < 90) {			
			ret = min(MAX_GOOGLE_MAPS_ZOOM_LEVEL, min(zoomForLatSpan(latitudeSpan, vertMapTiles), zoomForLongSpan(longitudeSpan, horizMapTiles)));
		} else {
			ret = -1;
		}
		return ret;
	}

	public static int zoomForLatSpan(double span, int tiles) {
		// So.  2^zoom tiles cover 180 degrees...
		return (int) floor(log(180 * tiles / span) / log(2)) ;
	}

	public static int zoomForLongSpan(double span, int tiles) {
		// 2^zoom tiles cover 360 degrees...
		return (int) floor(log(360 * tiles / span) / log(2)) ;
	}

	public static String latitudeToMinutes(double latitude, double error, boolean doFractional, boolean useDegreeSymbol) {
		char hemisphere = 'N';
		if(latitude < 0.0)
			hemisphere = 'S' ;
		return coordToMinutes(latitude, error, useDegreeSymbol, doFractional) + hemisphere ;
	}
	
	public static String longitudeToMinutes(double longitude, double error, boolean doFractional, boolean useDegreeSymbol) {
		char hemisphere = 'E';
		if(longitude < 0.0)
			hemisphere = 'W' ;
		return coordToMinutes(longitude, error, doFractional, useDegreeSymbol) + hemisphere ;
	}
	
	public static String coordinatesToMinutes(double[] coordinates, boolean doFractional, boolean  useDegreeSymbol) {
		String ret = "Not enough information." ;
		if (coordinates.length > 5) {
			ret = latitudeToMinutes(coordinates[0], coordinates[2], doFractional, useDegreeSymbol) + ", ";
			ret += longitudeToMinutes(coordinates[1], coordinates[3], doFractional, useDegreeSymbol);
		}
		return ret;
	}
	
	public static String coordToMinutes(double coord, double error, boolean doFractional, boolean useDegreeSymbol) {
		String ret = "";
		coord = abs(coord) ;
		ret += (int) floor(coord);		
		if (useDegreeSymbol)
			ret += DEGREE_SYMBOL;
		else
			ret += ' ';
		if (error < 0.5) {
			double minutes = 60 * (coord - floor(coord));
			if ((error > 1.0 / 120.0) || (! doFractional))
				ret += (int) floor(minutes) + "'";
			else {
				int prec = liberalSignificantDigits(60 * error); // taste the scientific validity!  Or, you know, not.
				ret += String.format("%." + prec + "f'", minutes) ;
			}
		}
		return ret.trim();
	}
	
	public static String latitudeToSeconds(double latitude, double error, boolean doFractional, boolean useDegreeSymbol) {
		char hemisphere = 'N';
		if(latitude < 0.0)
			hemisphere = 'S' ;
		return coordToSeconds(latitude, error, doFractional, useDegreeSymbol) + hemisphere ;
	}

	public static String longitudeToSeconds(double longitude, double error, boolean doFractional, boolean useDegreeSymbol) {
		char hemisphere = 'E';
		if(longitude < 0.0)
			hemisphere = 'W';
		return coordToSeconds(longitude, error, doFractional, useDegreeSymbol) + hemisphere ;
	}
	
	public static String coordinatesToSeconds(double[] coordinates, boolean doFractional, boolean  useDegreeSymbol) {
		String ret = "Not enough information." ;
		if (coordinates.length > 5) {
			ret = latitudeToSeconds(coordinates[0], coordinates[2], doFractional, useDegreeSymbol) + ", ";
			ret += longitudeToSeconds(coordinates[1], coordinates[3], doFractional, useDegreeSymbol);
		}
		return ret;
	}
	
	public static String coordToSeconds(double coord, double error, boolean doFractional, boolean useDegreeSymbol) {
		String ret = "";
		coord = abs(coord) ;
		ret += (int) floor(coord);		
		if (useDegreeSymbol)
			ret += DEGREE_SYMBOL;
		else
			ret += ' ';
		if (error < 0.5) {
			double minutes = 60 * (coord - floor(coord));
			ret += (int) floor(minutes) + "'";
			if (error < 1.0 / 120.0) {
				double seconds = 60 * (minutes - floor(minutes)) ;
				if (error > 1.0 / 7200.0 || ! doFractional)
					ret += (int) floor(seconds) + "\"";
				else {
					int prec = liberalSignificantDigits(60 * 60 * error); // taste the scientific validity!  Or, you know, not.
					ret += String.format("%." + prec + "f\"", seconds) ;
				}
			}
		}
		return ret.trim();
	}
	
	public static int significantDigits(double relativeError) {
		return (int) floor(-log10(2*relativeError)) ;
	}
	
	/**
	 * Um.
	 * 
	 * Because real, mathematician-approved significant digit calculation is slightly lossy.  This one is by contrast slightly misleading, as the last digit may be off by as much as 
	 * not quite 5.  So, conservative lossy, liberal misleading.
	 * 
	 * @param relativeError
	 * @return
	 */
	public static int liberalSignificantDigits(double relativeError) {
		return (int) ceil(-log10(2*relativeError)) ;
	}
	
	public static double[] parseCoordinates(String coordinates) {
		System.out.println("attempting to parse geocoordinates:  \"" + coordinates + "\"") ;
		double[] ret = new double[0];
		String coords = coordinates.toUpperCase();
		if (coords.matches(METAR_COORDINATES_REGEX)) {
			String[] latLong = coords.split("[, ]+", 0) ;
			ret = metarToDoubles(latLong[0], latLong[1]) ;
		} else if (coords.matches(METAR_REVERSED_COORDINATES_REGEX)) {
			String[] longLat = coords.split("[, ]+", 0) ;
			ret = metarToDoubles(longLat[1], longLat[0]) ;
		} else if (coords.matches(FLOAT_COORDINATES_REGEX)) {
			String[] latLong = coords.split("[, ]+", 0) ;
			ret = decimalCoordsToDoubles(latLong[0], latLong[1]) ;
		} else {
			System.out.println("failed to parse geocoordinates:  \"" + coordinates + "\"") ;
		}
		return ret;
	}
}
