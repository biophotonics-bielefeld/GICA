/*
This file is part of Gamma-norm Image Colocalization Analysis (GICA).

GICA is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

GICA is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GICA.  If not, see <http://www.gnu.org/licenses/>
*/
package de.bio_photonics.gica;

import ij.process.ImageProcessor;
import ij.IJ;

/** Some static tools for image computation */
public class Tools {

    /** Log level enum */
    public enum LL {
	DEBUG, INFO, ERROR, PARAMFAIL;
    }
    

    /** compute the average of an image processor */
    static public float avr( ImageProcessor ip ) {
	
	final int w = ip.getWidth();
	final int h = ip.getHeight();
	float ret=0;
	for (int y=0;y<h; y++)
	for (int x=0;x<w; x++) 
	    ret += ip.getf(x,y); 
	
	return ret/(w*h);
    }

    /** compute the variance of an ImageProcessor */
    static public float var( ImageProcessor ip , float avr) {
	
	final int w = ip.getWidth();
	final int h = ip.getHeight();
	float ret=0;
	for (int y=0;y<h; y++)
	for (int x=0;x<w; x++) 
	    ret += Math.pow(ip.getf(x,y) - avr,2); 
	
	return ret/(w*h-1);
    }


    /** Logger */
    static public void log( String what ) {
	IJ.log("GICA:: "+what);
    }

    /** Logger */
    static public void log( String what, LL l ) {
	IJ.log("GICA:: "+ what);
	if ( l == LL.PARAMFAIL ) {
	    IJ.showMessage("GICA: "+what);
	}
    }

    /** compute the faculty */
    static int faculty(int f) {
	int ret=1;
	for (int i=f; i>1; i--) ret*=i;
	return ret;
    }


}



