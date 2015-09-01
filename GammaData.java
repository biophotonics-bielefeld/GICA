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
import ij.process.FloatProcessor;

/** Class to compute and hold the gamma data for an image */
class GammaData {

    // store the gammas
    final byte [] gammas;

    // caching for fast random numbers
    // TODO: this works o.k., but could be done better
    static final double [] rndCache;
    static final int rndMax = 2048*2048;
    static {
	rndCache = new double [rndMax];
	for (int i=0; i<rndMax; i++)
	    rndCache[i] =  Math.random();
    }


    // stores the image size
    final int width, height;

    /** private constructor for empty gamma norm */
    private GammaData( int w, int h ) {
	width = w; height = h;
	gammas = new byte [w*h];   
    }

    /** create the data */
    GammaData( ImageProcessor ip , float fac ) {
	
	// get average and variance
	final float avr = Tools.avr( ip );
	final float var = Tools.var( ip , avr );

	// set the threshhold
	final float thr = avr + fac * (float)Math.sqrt(var);
	
	// compute the gammas
	width = ip.getWidth();
	height = ip.getHeight();
	gammas = new byte[ width*height ];
	int cnt = 0;

	for ( int y=0;y<height;y++)
	for ( int x=0;x<width;x++)
	    if ( ip.getf(x,y) > thr ) {
		gammas[ x +y*width] = 1;
		cnt ++;
	    }
	
	// output debug information
	Tools.log( "GiCA avr: "+avr+" var: "+var+
	    " --> thr: "+thr+"  pxl: "+cnt+"/"+(width*height)+
	    " ratio: "+ cnt/(float)(width*height));

    }

    /** Return the gamma norm for r_sum */
    static GammaData sumGamma( GammaData [] gds ) {
    
	if (gds == null) return null;
	final int l = gds[0].gammas.length;

	// loop and summ all gamma norms
	GammaData ret = new GammaData( gds[0].width, gds[0].height );
	for ( GammaData gd : gds )
	    for (int i=0; i<l; i++)
		ret.gammas[i] += gd.gammas[i];
	     
	return ret;
    }

    /** Return the gamma norm for r_col */
    static GammaData colGamma( GammaData [] gds ) {
    
	if (gds == null) return null;
	final int l = gds[0].gammas.length;

	// set all gammas to 1
	GammaData ret = new GammaData( gds[0].width, gds[0].height );
	for ( int i=0; i<l; i++)
	    ret.gammas[i] = 1;

	// set gammas to 0 if they are 0 in any input data
	for ( GammaData gd : gds )
	    for (int i=0; i<l; i++)
		if ( gd.gammas[i] == 0 ) ret.gammas[i]=0;
	     
	return ret;
    }

    /** Returns the number of over-threshold pxl to sum of pxl */
    public int getCount(int xIn, int yIn, int wIn, int hIn) {
	int c=0;
	for (int y=yIn; y<yIn+hIn;y++) 
	for (int x=xIn; x<xIn+wIn;x++) 
	   c+=gammas[ y*width + x ];
	return c;
    }


    /** Returns an representation of the norm as a byte processor */
    public FloatProcessor toImage() {

    	FloatProcessor ret = new FloatProcessor( width, height );
	for (int y=0; y< height; y++)
	for (int x=0; x< width; x++)
	    ret.setf( x,y,gammas[y*width+x] );

	return ret;

    }


    /** Obtain a set of 'bootM' variances, optimized for precision 
     *  and large ROIs */
    public static float [] genMeasureData( 
	GammaData inI, GammaData inJ,
	int xIn, int yIn, int wIn, int hIn,  
	final int bootM, final double sampleFac, final int nStat) {

	int cnt=0;
	final int width  = inI.width;
	final int height = inI.height;
	final int nSample = (int)(sampleFac * wIn * hIn);
	
	final byte [] Ii = inI.gammas;
	final byte [] Ij = inJ.gammas;
	
	// linearize data
	byte [] valI = new byte[ wIn*hIn ];
	byte [] valJ = new byte[ wIn*hIn ];
	for (int y=yIn; y<yIn+hIn;y++) 
	for (int x=xIn; x<xIn+wIn;x++) {
	    valI[cnt]=inI.gammas[ y*width + x];
	    valJ[cnt]=inJ.gammas[ y*width + x];
	    cnt++;
	}

	// create m samples of the input data 
	float [] res = new float[bootM];
	for (int k=0;k<bootM;k++) {
	    
	    // create a random subset
	    float [] sumI = new float[nStat];
	    float [] sumJ = new float[nStat];

	    int offS = (int)(Math.random()*rndMax);
	    for (int i=0; i<nStat;i++)
	    for (int j=0; j<nSample;j++) {
		int pos = (int)(rndCache[(i*nStat+j*3+offS)%rndMax]*valI.length);
		sumI[i] += valI[pos];
		sumJ[i] += valJ[pos];
	    } 
	   
	    /*
	    for (int i=0; i<nStat;i++) {
		int offS = (int)(Math.random()*rndMax);
		for (int j=0; j<valI.length;j++) 
		if ( rndCache[(j*3+offS)%rndMax] < sampleFac) {
		    sumI[i] += valI[j];
		    sumJ[i] += valJ[j];
		} 
	    }
	    */

	    // calculate the average
	    float avrI=0, avrJ=0;
	    for (float i : sumI) avrI+=(i/nStat);
	    for (float j : sumJ) avrJ+=(j/nStat);

	    // calculate the variance
	    float varIJ=0, varI=0, varJ=0;
	    for (int i=0; i<sumI.length; i++) {
		varIJ+=(sumI[i]-avrI)*(sumJ[i]-avrJ);
		varI+=Math.pow(sumI[i]-avrI,2);
		varJ+=Math.pow(sumJ[i]-avrJ,2);
	    }

	    // compute quotient
	    res[k]=0;
	    
	    if (( Math.abs(varJ)>0.001 )&&(Math.abs(varI)>0.001))
		res[k] = varIJ / (float)(Math.sqrt(varI) * Math.sqrt(varJ));
	}


	// return the full result
	return res;

    }



    /** Obtain a variance, optimized for small ROIs, for topology */
    public static float [] genTopoData( 
	GammaData inI, GammaData inJ,
	int xIn, int yIn, int wIn, int hIn,  int n) {

	int cnt =0;
	final int width = inI.width;

	// linearize data
	float [] valI = new float[ wIn*hIn ];
	float [] valJ = new float[ wIn*hIn ];
	for (int y=yIn; y<yIn+hIn;y++) 
	for (int x=xIn; x<xIn+wIn;x++) {
	    valI[cnt]=inI.gammas[ y*width + x];
	    valJ[cnt]=inJ.gammas[ y*width + x];
	    cnt++;
	}
	
	// create a random sample of the input data, sized n
	int offS = (int)(Math.random()*rndMax);
	float [] sumI = new float[n];
	float [] sumJ = new float[n];
	for (int i=0; i<n;i++)
	    for ( int j=0; j<n; j++) {
		int pos = (int)(rndCache[(i*n+j+offS)%rndMax]*valI.length);
		sumI[i] += valI[pos];
		sumJ[i] += valJ[pos];
	    }


	// calculate the average
	float avrI=0, avrJ=0;
	for (float i : sumI) avrI+=i/n;
	for (float j : sumJ) avrJ+=j/n;

	// calculate the variance
	float varIJ=0, varI=0, varJ=0;
	for (int i=0; i<sumI.length; i++) {
	    varIJ+=(sumI[i]-avrI)*(sumJ[i]-avrJ);
	    varI+=Math.pow(sumI[i]-avrI,2);
	    varJ+=Math.pow(sumJ[i]-avrJ,2);
	}

	// compute quotient
	float var=0;
	
	if (( Math.abs(varJ)>0.001 )&&(Math.abs(varI)>0.001))
	    var = varIJ / (float)(Math.sqrt(varI) * Math.sqrt(varJ));

	// return the full result
	return new float [] { var, varIJ };

    }



}


