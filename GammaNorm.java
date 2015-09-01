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

import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

 

public class GammaNorm {
    //implements SimpleMC<LoopParam> {
 
    final int width, height, cCount;

    GammaData [] gDats;
    GammaData gSum, gCol;

    protected double sampleFactor;  // quotient of sample element
    protected int    bsCount;	    // number of error est. samples
    protected int    nStatCount;    // number of samples for variance

    protected int      binSize, secSize;
    protected boolean  fullResult;
    protected ImagePlus sourceImg;

    private final float thrFac;

    /** creates a GammaNorm for the ImageStack */
    public GammaNorm( ImageStack is, float fac ) {

	// check if the stack is sized correctly
	if (is.getSize()<2)
	    throw new RuntimeException("Input stack to short");

	// copy image parameters
	width  = is.getWidth();
	height = is.getHeight();
	cCount = is.getSize();
	thrFac = fac;

	// copute gamma data for all channels (all images in stack)
	gDats = new GammaData[ cCount ];

	for (int i=0; i<cCount;i++)
	    gDats[i] = new GammaData( is.getProcessor(i+1) , fac);

	// compute the sum and col gamma norm
	gSum = GammaData.sumGamma( gDats );
	gCol = GammaData.colGamma( gDats );

    }




    /** Calculate the gamma for a sub-region of the image.
     *  This uses the global threshhold. */
    public void measureRoi(GICAmeasurement gm) {

	// parameters
	final int N = gDats.length;
	final int rMaxCount = Tools.faculty(N-1) + N +1;
	final double rMax   = Math.sqrt( rMaxCount );
	int cnt=0;

	// calculate a set of values
	double [] resVector = new double[ bsCount ];
	IJ.showProgress(0,rMaxCount);

	// each channel with the other
	for (int i=0;i<gDats.length-1;i++)
	for (int j=0;i<gDats.length;i++)  
	if (i!=j) { 
	    float [] val = GammaData.genMeasureData( 
		gDats[i], gDats[j], 
		gm.x, gm.y, gm.w, gm.h,
		bsCount, sampleFactor, nStatCount );
	
	    for (int k=0;k<bsCount;k++)
		resVector[k]+= Math.pow(val[k],2);
	
	    IJ.showProgress(++cnt, rMaxCount);
	}

	
	// each channel with the col
	for (int i=0;i<gDats.length;i++) { 
	    float [] val = GammaData.genMeasureData( 
	        gDats[i], gCol, 
	        gm.x, gm.y, gm.w, gm.h,
		bsCount, sampleFactor, nStatCount );
	    for (int k=0;k<bsCount;k++)
		resVector[k]+= Math.pow(val[k],2);
	    IJ.showProgress(++cnt, rMaxCount);
	}


	// col with sum
	{
	    float [] val = GammaData.genMeasureData( 
		gSum, gCol, 
		gm.x, gm.y, gm.w, gm.h,
		bsCount, sampleFactor, nStatCount );
	    for (int k=0;k<bsCount;k++)
		resVector[k]+= Math.pow(val[k],2);
	    IJ.showProgress(++cnt, rMaxCount);
	}

	// calculate all the vector lenth
	for (int k=0;k<bsCount;k++)
	    resVector[k] = Math.sqrt( resVector[k] );

	// ... gamma norms average
	double resAvr=0;
	for (double i : resVector) 
	    resAvr+=i/resVector.length;
	
	// .... gamma norms variance
	double resVar=0;
	for (double i : resVector) 
	    resVar+=Math.pow( i-resAvr ,2 );
	
	// store measurements
	gm.gNorm    = resAvr;
	gm.gNormErr = Math.sqrt((1./(resVector.length-1))* resVar );
	
	// store number of pxl and col. coeff.
	gm.colPx = 
	    gCol.getCount( gm.x, gm.y, gm.w, gm.h )/(double)(gm.w*gm.h);
	
	gm.listI = new int[ gDats.length +1];
	gm.listI[0] = gCol.getCount(gm.x, gm.y, gm.w, gm.h );
	for ( int i=0; i<gDats.length; i++)
	    gm.listI[i+1] = gDats[i].getCount( gm.x, gm.y, gm.w, gm.h );
	
	gm.af  = (rMax - resAvr ) / (rMax );
	gm.thr = thrFac; 

    }


    /** Calculate the gamma for a sub-region of the image.
     *  This uses the global threshhold. */
    public ImageStack [] getGammaStack( int binSize, int nSection) {

	ImageStack retSt    = new ImageStack(width, height);
	ImageStack retStAbs = new ImageStack(width, height);

	// number of channels
	final int N = gDats.length;

	// current and end value for status bar update
	int curStat = 0; 
	final int maxStat = (Tools.faculty(N-1) + N + 1);
    
	// each channel with the other
	for (int i=0;i<N-1;i++) 
	for (int j=1;j<N;j++) 
	if (i!=j)  {
	    FloatProcessor [] img = getGammaProcessor( gDats[i], gDats[j], binSize, nSection);
	    IJ.showProgress( curStat++, maxStat);
	    retSt.addSlice("r_ij Ch "+i+","+j, img[0]);
	    retStAbs.addSlice("abs r_ij Ch "+i+","+j, img[1]);
	}

	// each channel with the col
	for (int i=0;i<gDats.length;i++) {
	    FloatProcessor [] img = getGammaProcessor( gDats[i], gCol, binSize, nSection);
	    IJ.showProgress( curStat++, maxStat);
	    retSt.addSlice("r_col,"+i, img[0]);
	    retStAbs.addSlice("abs r_col,"+i, img[1]);
	}

	// col with sum
	FloatProcessor [] img = getGammaProcessor( gSum, gCol, binSize, nSection);
	IJ.showProgress( curStat++, maxStat);
	retSt.addSlice("r_col,sum", img[0]);
	retStAbs.addSlice("abs r_col,sum", img[1]);

	return new ImageStack [] { retSt , retStAbs };
    }


    /* Computes the FloatProcessors for GammaValue visualization between two GammaData objects. */
    FloatProcessor [] getGammaProcessor( final GammaData gdi, final GammaData gdj,
	final int binSize, final int nSection  ){

	// create new output images
	final FloatProcessor img    = new FloatProcessor(width, height);
	final FloatProcessor imgAbs = new FloatProcessor(width, height);

	// loop
	new SimpleMT.PFor( 0, height-binSize ) {
	    //for( int y=0;y<height-binSize; y++) {
	    public void at(int y) {
		for( int x=0;x<width -binSize; x++) {

		    float [] tmp = GammaData.genTopoData( 
			gdi, gdj, x, y, binSize, binSize, nSection);
		    
		    img.setf(x+binSize/2,y+binSize/2,tmp[0]);
		    imgAbs.setf(x+binSize/2,y+binSize/2,tmp[1]);
		}
	    }
	};
    
	// return both images
	return new FloatProcessor [] { img, imgAbs };

    } 

    /** Computes the euclidean norm of a stack of FloatProcessors.
     *  TODO: This blindly assumes all ImageProcessors to be FloatProcessors. */
    static FloatProcessor euclSumStack( ImageStack in ) {

	FloatProcessor ret = new FloatProcessor(
	    in.getWidth(), in.getHeight());

	float [] pxl = (float[])ret.getPixels();

	for (int i=1; i<= in.getSize(); i++) {
	    float val [] = (float[])in.getProcessor(i).getPixels();
	    for (int j=0;j<pxl.length;j++)
		pxl[j] += Math.pow(val[j],2);
	}

	for (int j=0;j<pxl.length;j++)
	    pxl[j] = (float)Math.sqrt(pxl[j]);

	return ret;
    }


}

