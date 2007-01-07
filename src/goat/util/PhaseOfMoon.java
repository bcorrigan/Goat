/* POM -- a Java program to display the phase of the Moon.
 * Copyright (C) 1997,1999  Paul Rouse <par@acm.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * Please send bugs or suggestions to Paul Rouse <par@acm.org>
 */

package goat.util;


/** Static methods of this class do the basic maths.
 *  Note you cannot instantiate this class: it is like java.lang.Math.<p>
 *  The calculations follow closely the methods given in the book
 *  "Practical Astronomy with your Calculator" by Peter Duffett-Smith
 *  3rd Edition Cambridge University Press (1988)
 *  ISBN 0 521 35699 7 (paperback) or ISBN 0 521 35629 (hardback)
 *  The main modification to those methods being the uniform use of
 *  radians in this code, instead of the degrees used in the book.
 */
public final class PhaseOfMoon {

    /** Private constructor prevents instantiation.
     *  This class is meant to provide only static methods.
     *  See langauge spec 8.1.2.1 (end)
     */
    private PhaseOfMoon () { }

    /** The origin used for time.
     *  We actually work relative to 1990 Jan 0.0, ie 1 Jan 1990 00:00:00
     *  is treated as day *one* -- remembering 5 leap years as well
     */
    private static long EPOCH = ((1990 - 1970) * 365 + 4) * 24 * 3600 * 1000L;


    /* =======================================================================
     * Sun's orbit described as seen from the Earth, ie as if the sun were
     * orbiting the Earth, in ecliptic coordinates
     */

    /** Mean tropical year in milliseconds (365.242191 days).
     */
    private static long year = 31556925302L; 

    /** Solar ecliptic longitude at EPOCH (279.403303 degrees).
     */
    private static double EPSILONg = 4.876507578;

    /** Solar ecliptic longitude of perigee at EPOCH (282.768422 degrees).
     */
    private static double OMEGAg = 4.935239985;

    /** Solar orbit eccentricity at EPOCH.
     */
    private static double ECCEN = 0.01671332;


    /* =======================================================================
     * Lunar orbit
     */

    /** Lunar mean ecliptic period (milliseconds; was 13.1763966 degree/day).
     */
    private static long lmonth = 2360584684L;

    /** Lunar mean longitude at EPOCH (318.351648 degrees).
     */
    private static double lzero = 5.556284437;

    /** Lunar mean longitude of perigee at EPOCH (36.340410 degrees).
     */
    private static double Pzero = 0.634259806;

    /** Lunar mean longitude of node at EPOCH (318.510107 degrees).
     */
    private static double Nzero = 5.559050068;

    /** Lunar mean precession of perigee (0.1114041 degree/day).
     */
    private static double Pprec = 2.250426e-11;

    /** Lunar mean precession of node (0.0529539 degree/day).
     */
    private static double Nprec = 1.069699e-11;

    /** Various constants of unclear (to me!) meaning for the corrections.
     */
    private static double Cev = 0.0222337,         // was 1.2739 degrees
                          Cac = 0.0032428,         // was 0.1858 degrees
                          Ca3 = 0.0064577,         // was 0.37   degrees
                          Cec = 0.1097568,         // was 6.2886 degrees
                          Ca4 = 0.0037350,         // was 0.214  degrees
                          Cv  = 0.0114895;         // was 0.6583 degrees
    
	private static double dayArc = (2*Math.PI) / (lmonth / (60*60*24*1000));

    /* =======================================================================
     */

    /** Solve Kepler's equation for orbit.
     *  Given the "mean anomaly" M (the angle measured from perigee if the
     *  orbit were a uniform circular one) and the eccentricity e, use
     *  Kepler's equation to solve for the true anomaly (the angle measured
     *  from perigee in the actual eliptical orbit).<p>
     * 
     *  See section 47 of Duffett-Smith (except that we use the approximate
     *  solution section 46 as a better initial guess).
     */
    private static double Kepler (double M, double e) {
        double E;     // The eccentric anomaly
        double nu;    // The result (the true anomaly)
        double delta;
        double ee = Math.sqrt ((1+e)/(1-e));

        // Initial guess -- work backwards from the approximation
        delta = 2 * e * Math.sin (M);
        nu = M + delta;
//System.out.println ("Kepler: guess: nu " + nu + " (M=" + M + "; e=" + e + ")");
        if (Math.abs (delta) < 1e-12)
            return (nu);    // Approx very good, and avoid tan pi/2
        E = 2 * Math.atan (Math.tan (nu/2) / ee);

        // Iterative solution for E - e sin(E) = M
        do {
            delta = E - e * Math.sin (E) - M;
            E -= delta / (1 - e * Math.cos (E));
        } while (Math.abs(delta) > 1e-12);

        // Find true anomaly from eccentric anomaly
        nu = 2 * Math.atan (ee * Math.tan (E/2));
        if (nu < 0.0)
            nu += 2*Math.PI;
//System.out.println ("Kepler: final: nu " + nu);
        return (nu);
    }


    /** Determine the phase of the moon at the given time.
     *  Given a time in milliseconds from 00:00:00 January 1st 1970, return
     *  the phase of the moon as an angle (in radians) where 0 represents
     *  the new moon, and pi represents the full moon (and of course 2*pi
     *  is back to the new moon).
     */
    public static double MoonPhase (long time) {
        double N,         // "Phase" of earth orbit round sun (0 - 2*pi)
               Msol,
               Ec,
               LambdaSol;
        double l,
               Mm,
               Ev,
               Ac,
               A3,
               Mmprime,
               A4,
               lprime,
               V,
               ldprime,
               D,
               Nm;

        time -= EPOCH;    // Time in this method is relative to EPOCH

        N = 2 * Math.PI * (double)(time % year) / (double)year;
        Msol = N + EPSILONg - OMEGAg;
        LambdaSol = Kepler (Msol, ECCEN) + OMEGAg;

        l = lzero + 2 * Math.PI * (double)(time % lmonth) / (double)lmonth;
        Mm = l - Pprec * (double)time - Pzero;
        Nm = Nzero - Nprec * (double)time;

        Ev = Cev * Math.sin (2 * (l - LambdaSol) - Mm);
        Ac = Cac * Math.sin (Msol);
        A3 = Ca3 * Math.sin (Msol);
        Mmprime = Mm + Ev - Ac - A3;
        Ec = Cec * Math.sin (Mmprime);
        A4 = Ca4 * Math.sin (2 * Mmprime);
        lprime = l + Ev + Ec - Ac + A4;
        V =  Cv * Math.sin (2 * (lprime - LambdaSol));

        ldprime = lprime + V;
        D = ldprime - LambdaSol;

        // Adjust D to range 0-2*pi
        D = D / (2*Math.PI);
        D = D - Math.floor(D);
        D = D * 2 * Math.PI;

        return (D);
    }

    public static String phaseAsString(long time) {
    	String ret = "I can't see the moon, sorry";
    	double phaseAngle = MoonPhase(time) ;
    	double hdayArc = dayArc / 2 ;
    	
    	if ((phaseAngle <= hdayArc) || (phaseAngle >= 2 * Math.PI - hdayArc)) {
    		ret = "new moon" ;
    	} else if (phaseAngle < ((Math.PI/2.0) - hdayArc)) {
    		ret = "waxing crescent, " + daysPastString(0, phaseAngle) + " past new moon";
    	} else if (phaseAngle <= ((Math.PI/2.0) + hdayArc)) {
    		ret = "half moon, waxing" ;
    	} else if (phaseAngle < (Math.PI - hdayArc)) {
    		ret = "waxing gibbous, " + daysPastString(Math.PI / 2.0, phaseAngle) + " past half moon" ;
    	} else if (phaseAngle <= (Math.PI + hdayArc)) {
    		ret = "full moon" ;
    	} else if (phaseAngle < (((1.5)*Math.PI) - hdayArc)) {
    		ret = "waning gibbous, " + daysPastString(Math.PI, phaseAngle) + " past full moon";
    	} else if (phaseAngle <= (((1.5)*Math.PI) + hdayArc)) {
    		ret = "half moon, waning" ;
    	} else if (phaseAngle < (((2.0)*Math.PI) - hdayArc)){
    		ret = "waning crescent, " + daysPastString( (1.5) * Math.PI, phaseAngle) + " past half moon" ;
    	} else {
    		ret = "the moon is broken; moon angle is:  " + Double.toString(phaseAngle) ;
    	}
    	return ret;
    }
    
    public static String phaseAsShortString(long time) {
    	String ret = "I can't see the moon, sorry";
    	double phaseAngle = MoonPhase(time) ;
    	double hdayArc = dayArc / 2 ;
    	
    	if ((phaseAngle <= hdayArc) || (phaseAngle >= 2 * Math.PI - hdayArc)) {
    		ret = "new" ;
    	} else if (phaseAngle < (Math.PI - hdayArc)) {
    		ret = Long.toString(Math.round(phaseAngle*100.0/Math.PI)) + "%+" ;
    	} else if (phaseAngle <= (Math.PI + hdayArc)) {
    		ret = "full" ;
    	} else if (phaseAngle < (((2.0)*Math.PI) - hdayArc)){
    		ret = Long.toString(Math.round((2.0*Math.PI - phaseAngle)*100/Math.PI)) + "%-" ;
    	} else {
    		ret = "broken; angle:  " + Double.toString(phaseAngle) ;
    	}
    	return ret;
    }
    
    // helper functions for string methods
    private static long daysPast(double pastAngle, double presentAngle) {
    	return Math.round((presentAngle - pastAngle)/dayArc) ;
    }
    
    private static String daysPastString(double pastAngle, double presentAngle) {
    	String ret = "";
    	long dp = daysPast(pastAngle, presentAngle) ;
		ret = Long.toString(dp) + " day" ;
		if (dp != 1 && dp != -1) {
			ret += "s" ;
    	}
    	return ret;
    }

    /** Find the time of the given phase nearest to the given time.
     *  Find the nearest time to "nearTime" at which the phase of the Moon
     *  is equal to "phase".  The determination of "nearest" may be inexact if
     *  the actual phase at nearTime is very close to PI different from the
     *  required phase, as nearness is judged by the linear approximation
     *  rather than the full calculation of the phase.
     */
    public static long TimeOf (double phase, long nearTime) {
        double actual, difference;
        long t = nearTime;
        long correction;
        do {
            actual = MoonPhase (t);
            difference = phase - actual;
            difference = difference / (2*Math.PI);
            difference = difference - Math.floor (difference);
            if (difference < 0.5) {
                correction =  Math.round (difference * (double)lmonth);
                t = t + correction;
            } else {
                correction = Math.round ((1.0 - difference) * (double)lmonth);
                t = t - correction;
            }
        } while (correction > 60000);
        return (t);
    }


    /** The length, in milliseconds, of the lunar month (mean ecliptic period).
     */
    public static long LMonth () {
        return lmonth;
    }
}
