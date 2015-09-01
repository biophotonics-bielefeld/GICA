#
# A very basic Makefile to compile the plugin
#

# Options for the java compiler
JFLAGS = -g -Xlint:unchecked -extdirs ./external -d ./ 
JFLAGS+= -target 1.6 -source 1.6
JC = javac
JAR = jar
RM = rm -rvf

.PHONY: clean doc

# Build the program
GICA_Analysis:	GICA_Analysis.class

GICA_Analysis.class: $(wildcard *.java)
	$(JC) $(JFLAGS) $(wildcard *.java)

# create jar file
jar	: GICA_Analysis
	$(JAR) -mcvf Manifest.txt GICA_$(shell date +%Y%m%d-%H%M).jar plugins.config \
	de/bio_photonics/*/*.class  

# create jar file (w. source)
jarsrc	: GICA_Analysis
	$(JAR) -mcvf Manifest.txt GICAsrc_$(shell date +%Y%m%d-%H%M).jar plugins.config \
	de/bio_photonics/*/*.class \
	*.java


# create javadoc
doc:
	javadoc -d doc/ -classpath ./ -extdirs ./external -subpackages de.bio_photonics.gica *.java 

# clean
clean :
	$(RM) de/bio_photonics/gica/*.class *.jar doc/*
