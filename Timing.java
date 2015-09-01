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


public class Timing {
    long start, stop, runtime, outtime;
    Timing() { start =  System.currentTimeMillis(); }
    public void start() { start = System.currentTimeMillis(); };
    public void stop() { 
	stop = System.currentTimeMillis(); 
	runtime += stop-start;
	outtime=runtime;
	runtime=0;
	}
    public void hold(){
	stop = System.currentTimeMillis();
	runtime += stop-start;
	outtime  = runtime;
	start =stop;
    }
    @Override public String toString(){ return("ms: "+(outtime));}

}

