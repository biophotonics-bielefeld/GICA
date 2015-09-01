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

import java.util.List;

/** Structure to store (and display) GICA measurements.
 *  TODO: This could go into a result table of Fiji. */
class GICAmeasurement {

    double gNorm, gNormErr, colPx, af, thr;
    int [] listI;
    final int x,y,w,h;
    final String imgLabel;

    /** Create a measurement */
    GICAmeasurement(int xi, int yi, int wi, int hi, String l){
	imgLabel=l; x=xi; y=yi; w=wi; h=hi;
    }

    /** Create a HTML table from a list of measurements */
    public static String toHTMLtable( List<GICAmeasurement> l) {
	String ret = "<table border=\"1\">";
	ret+="<tr><th>&#915 norm</th><th>&#916 &#915 norm</th>";
	ret+="<th>I<sub>col.</sub>(rel.)</th>";
	ret+="<th>A.F.</th><th>thr</th>";
	ret+="<th>x</th><th>y</th><th>w</th><th>h</th>";
	ret+="<th>I<sub>col</sub>,I<sub>l</sub>,I<sub>total</sub></th>";
	ret+="<th>IMG</th>";
	for ( GICAmeasurement i : l )
	    ret+= i.htmlTableRow();

	ret+="</table>";
	return ret;
    }

    /** Create a table row from one measurement */
    private String htmlTableRow() {
	String ret = "<tr>";
	ret += String.format("<td>%2.2f</td>",gNorm);
	ret += String.format("<td>%2.2f</td>",gNormErr);
	ret += String.format("<td>%2.2f</td>",colPx);
	ret += String.format("<td>%2.2f</td>",af);
	ret += String.format("<td>%2.2f</td>",thr);
	ret += String.format("<td>%3d</td>",x);
	ret += String.format("<td>%3d</td>",y);
	ret += String.format("<td>%3d</td>",w);
	ret += String.format("<td>%3d</td>",h);
	

	ret += "<td>";
	for (int i : listI ) ret+=""+i+" / ";
	ret += ""+(w*h)+"</td>";

	if (imgLabel.length()>12)
	    ret += "<td>"+imgLabel.substring(0,11)+"</td>";
	else
	    ret += "<td>"+imgLabel+"</td>";
	ret+="</tr>";
	return ret;
    }

}


