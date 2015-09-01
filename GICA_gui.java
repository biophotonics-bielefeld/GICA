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


import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.text.html.HTMLDocument;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Rectangle;

import java.util.List;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;


/** GUI to display GICA measurements */
public class GICA_gui implements 
    Runnable, PlugIn {


    JTextPane resultTable;
    JButton   measureButton;
    JButton   topologyButton;
    JButton   resetGammaButton;
    JButton   clearTable, saveTable;

    List<GICAmeasurement> gmeasure;
    {
	gmeasure = new ArrayList<GICAmeasurement>();
    }


    /** run method for ImageJ plugin interface */
    public void run (String arg) {
	IJ.log("Hello from GICA");
	new Thread(new GICA_gui()).start();
    } 


    /** When dispatched, sets up and displays the GUI */
    @Override
    public void run() { 

	// the result table
	resultTable = new JTextPane();
	resultTable.setContentType("text/html");
	resultTable.setText(paramToHtmlTable());
	resultTable.setEditable(false);

	JPanel resultTablePanel = new JPanel();
	resultTablePanel.add( resultTable );
	
	// the control buttons
	measureButton    = new JButton("measure ROI");
	topologyButton   = new JButton("create topology");
	resetGammaButton = new JButton("(re)set parameters");
	measureButton.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		measureRoi();
	    }
	});
	topologyButton.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		createTopology();
	    }
	});
	resetGammaButton.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		getGammaNorm( null, true);
	    }
	});

	clearTable = new JButton("clear table");
	//saveTable  = new JButton("save table");
	clearTable.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		gmeasure.clear();
		resultTable.setText(paramToHtmlTable());
	    }
	});
	

	JPanel buttonPanel = new JPanel();
	buttonPanel.add( measureButton);
	buttonPanel.add(topologyButton);
	buttonPanel.add(resetGammaButton);
	buttonPanel.add(clearTable);
	//buttonPanel.add(saveTable);
	
	// the GUI frame
	JFrame guiFrame = new JFrame("GICA analysis");
	guiFrame.getContentPane().add( 
	    resultTablePanel, BorderLayout.CENTER );
	guiFrame.getContentPane().add( 
	    buttonPanel, BorderLayout.PAGE_END );
	guiFrame.pack();
	guiFrame.setVisible(true);
	
    };


    /** Get the active ROI and measure it */
    void measureRoi() {
	
	// get the active Image
	ImagePlus aip = ij.WindowManager.getCurrentImage();
	if ( aip == null ) {
	    Tools.log("No image selected", Tools.LL.PARAMFAIL);
	    return;
	}
	
	// get the ROI
	Roi curRoi = aip.getRoi();
	if (( curRoi == null )||(!curRoi.isArea())||(curRoi.getMask()!=null)){
	    Tools.log("Please select an rectangular area via ROI", Tools.LL.PARAMFAIL);
	    return;
	}

	// get the GammaNorm for the image
	GammaNorm gn = getGammaNorm(aip,false);
	if (gn==null) {
	    Tools.log("No gamma norm available",Tools.LL.INFO);
	    return;
	}


	// setup the measurement
	Tools.log("Starting ROI measurement",Tools.LL.INFO);
	Rectangle area = curRoi.getBounds();
	GICAmeasurement ret = new GICAmeasurement(
	    area.x, area.y, area.width, area.height, aip.getTitle());

	
	// run the measurement
	Timing t1 = new Timing();
	t1.start();
	gn.measureRoi( ret );
	t1.stop();
	Tools.log("Gamma measure done "+t1,Tools.LL.INFO);

	// add the result
	gmeasure.add( ret );

	// redraw the table
	resultTable.setText(paramToHtmlTable());

    }


    /** create a topology */
    void createTopology() {

	// get the active Image
	ImagePlus aip = ij.WindowManager.getCurrentImage();
	if ( aip == null ) {
	    Tools.log("No image selected", Tools.LL.PARAMFAIL);
	    return;
	}
	
	// get the correspoding GammaNorm and calculate
	GammaNorm gn = getGammaNorm( aip, false );
	if (gn!=null) 
	    GICA_Analysis.computeGammaTopology(gn);
    }



    /** Tries to obtain / calculate a GammaNorm for the
     *  given (or active, if aip==null) image. This will
     *  return null if no GammaNorm was obtained. */
    GammaNorm getGammaNorm(ImagePlus aip, boolean recalc) {

	// get the active Image
	if (aip==null)
	    aip = ij.WindowManager.getCurrentImage();
	if (aip == null ) {
	    Tools.log("No image selected", Tools.LL.PARAMFAIL);
	    return null;
	}

	// maybe 'aip' is a topology display
	GammaNorm curGn = 
	    (GammaNorm)aip.getProperty("bbp.gica.isGammaNormDisplay");
	// if so, set the aip to the source image
	if (curGn!=null)
	    aip = curGn.sourceImg;
	
	// maybe we have a GammaNorm stored?
	curGn = (GammaNorm)aip.getProperty("bbp.gica.gammaNorm");
	// if so, and there is no need to recalc, return the current
	if ((curGn!=null)&&(!recalc))
	    return curGn;


	// recalculate the gamma norm
	curGn = GICA_Analysis.computeGammaNorm( aip );
	aip.setProperty( "bbp.gica.gammaNorm",curGn );
	return curGn;
    }



    /** converts the GICA measurements into an html table */
    String paramToHtmlTable() {

	String ret = "<html><body><h3>GICA result table</h3>";
	ret+=GICAmeasurement.toHTMLtable( gmeasure );
	ret+="</body></html>";
	return ret;
    }



    /** called by the buttons, selects what routine to start. */
    /*
    @Override
    public void actionPerformed(ActionEvent e) {
  
	Object who = e.getSource();
	
	// start the measurement
	if ( who == measureButton)
	    measureRoi();
	
	// create a topology
	if ( who == topologyButton)
	    createTopology();

	// reset the gamma data
	if ( who == resetGammaButton)
	    getGammaNorm( null, true);
   
	// clear the table
	if ( who == clearTable ) {
	    gmeasure.clear();
	    resultTable.setText(paramToHtmlTable());
	}


    } */



    /** starts up the gui for testing purpose */
    public static void main( String [] args ) {
	GICA_gui g = new GICA_gui( );
	g.run();
    }


}




