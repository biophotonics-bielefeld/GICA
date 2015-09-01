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
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.gui.YesNoCancelDialog;

import javax.swing.SwingWorker;

public class GICA_Analysis  {


    /** Calculate the gamma norm data for an image.
     *  Will open a parameter dialog. May return null
     *  for various reason (which are then given in the log). */ 
    static GammaNorm computeGammaNorm( ImagePlus aip  ) {
	
	// image parameter
	if ( aip == null ) return null;
	final int width  = aip.getWidth();
	final int height = aip.getHeight();
	boolean isRGB = false;
	final ImagePlus inputIP = aip;

	// check for RGB and convert to stack
	if (aip.getType() == ImagePlus.COLOR_RGB ) {
	    // currently, only 1 RGB slice
	    if ( aip.getStack().getSize() > 1 ) {
		Tools.log("RBG currently only supports 1 slice", 
		    Tools.LL.PARAMFAIL);
		return null;
	    }
	    
	    ImageStack [] rgbSt = ChannelSplitter.splitRGB( aip.getStack(), true);
	    ImageStack newStack = new ImageStack(width,height);
	    for ( ImageStack i : rgbSt )
		newStack.addSlice( i.getProcessor(1));
	    
	    Tools.log("RBG channels seperated into stack", 
		Tools.LL.DEBUG);

	    aip = new ImagePlus(aip.getTitle(),newStack);
	    //aip.show();
	    isRGB=true;
	}

	// check for stack size
	ImageStack aiStack = aip.getStack();
	int numImages = aiStack.getSize();
	if ((numImages <2)||(numImages>8)) {
		Tools.log("Please use 2 - 8 slices"+numImages,Tools.LL.PARAMFAIL);
		return null;
	}

	// check the image format
	if (( aip.getType() != ImagePlus.GRAY8 )  &&
	    ( aip.getType() != ImagePlus.GRAY16 ) &&
	    ( aip.getType() != ImagePlus.GRAY32 )    ) {
		Tools.log("Only supports grayscale stacks",Tools.LL.PARAMFAIL);
		return null;
	}
	
	// -------------------------

	// display GUI and set some parameter
	GenericDialog gd = new GenericDialog("GiCA settings");

	// display channel chooser
	for (int i=1; i<=numImages;i++) {
	    String [] cName = { "Red", "Green", "Blue" };
	    String label;
	    if (isRGB) 
		label = cName[i-1]; 
	    else 
		label = aip.getStack().getSliceLabel(i);
	    if (label == null) label ="N.N.";
    
	    if (label.length()>12) label = label.substring(0,11)+"...";

	    gd.addCheckbox("Ch "+i+": ["+label+"]",true);
	}

	// get the parameters
	gd.addNumericField("Threshhold factor", 2,1);
	gd.addMessage("--- ROI measurement ---");
	gd.addNumericField("Sample factor", 0.25,2);
	gd.addNumericField("Stat. #N", 20,0);
	gd.addNumericField("error est. N", 20,0);
	gd.addMessage("--- Topology ---");
	gd.addNumericField("width/height SuperPxl", 12,0);
	gd.addNumericField("Stat. #N (topo)" , 20,0);
	gd.addCheckbox("Show intermediate results?", false);
	
	// run the dialog
	gd.showDialog();
	if (gd.wasCanceled()) return null;

	// copy only selected slides from stack
	ImageStack inputData = new ImageStack( width, height );
	for (int i=1; i<=numImages;i++)
	    if ( gd.getNextBoolean() )
		inputData.addSlice( aip.getStack().getProcessor(i));

	if (inputData.getSize()<2) {
	    Tools.log("Please select at least 2 channels", 
	    Tools.LL.PARAMFAIL);
	    return null;
	}

	// compute the gamma norm data
	final float thr	= (float)gd.getNextNumber();

	Timing t1 = new Timing(); t1.start();
	GammaNorm ga = new GammaNorm( inputData , thr); 
	t1.stop();
	Tools.log("Gamma norm created "+t1, Tools.LL.INFO);
	
	// copy / store parameters
	ga.sampleFactor = gd.getNextNumber();
	ga.nStatCount	= (int) gd.getNextNumber();
	ga.bsCount	= (int) gd.getNextNumber();
	ga.binSize      = (int) gd.getNextNumber();
	ga.secSize      = (int) gd.getNextNumber();
	ga.fullResult	= gd.getNextBoolean();
	ga.sourceImg	= inputIP;

	return ga;
    }



    /** Compute the topology. Will re-display and re-compute
     *  the GammaNorm. */ 
    public static void computeGammaTopology( final GammaNorm ga ) {

	// check for size
	if ( ga.width * ga.height > 512*512 ) {
	    YesNoCancelDialog ynd = new YesNoCancelDialog(IJ.getInstance(), "GICA topology",
		"Topology computation on large images (512x512)\n"+
		"will take long to compute. Start?");
	    //ynd.setVisible(true);
	    if ( !ynd.yesPressed() )
		return;
	}


	// store the intermediate results
	final ImageStack trStck = new ImageStack( ga.width, ga.height );

	if ( ga.fullResult ) {
	    for ( int i=0; i<ga.gDats.length; i++ ) {
		trStck.addSlice( "I ch"+i, ga.gDats[i].toImage());
	    }
	    trStck.addSlice( "I sum", ga.gSum.toImage() );
	    trStck.addSlice( "I col", ga.gCol.toImage() );
	}

	// compute the r_ij and the final topology
	Tools.log("Computing topology (this can take some time)...", Tools.LL.INFO);
	
	class TopologyCompute extends SwingWorker<Object, Object> {
	    @Override
	    public Object doInBackground() {
		
		Timing t1 = new Timing();
		t1.start();
		ImageStack [] gammaStack =ga.getGammaStack( ga.binSize , ga.secSize);	
		t1.stop();
		Tools.log("... done. "+t1, Tools.LL.INFO);
		
		// show extra results if wanted
		if (ga.fullResult)
		for (int i=1;i<=gammaStack[0].getSize();i++)
		for (int  j=0;j<2;j++)
		    trStck.addSlice(
			gammaStack[j].getSliceLabel(i),
			gammaStack[j].getProcessor(i));

		// get the full topology
		ImageProcessor topology    = GammaNorm.euclSumStack( gammaStack[0] );
		ImageProcessor topologyAbs = GammaNorm.euclSumStack( gammaStack[1] );
		trStck.addSlice( "Topology (norm.)", topology);
		trStck.addSlice( "Topology (abs)", topologyAbs);

		// display results, store that this is a gamma topology
		ImagePlus trStckPl= new ImagePlus( "GICA results", trStck);
		trStckPl.setProperty("bbp.gica.isGammaNormDisplay", ga);
		trStckPl.show();

		return null;
	    
	    
	    
	    }
	    @Override
	    protected void done() {
	    }
	};

	(new TopologyCompute()).execute();	
	
	
	
	

    }


   


}

