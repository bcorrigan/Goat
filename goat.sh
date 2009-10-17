#!/bin/sh

# stinky one-liner to run goat.

# runs goat via jar file
#java -server -Xbootclasspath/a:`echo lib/* | tr \  :` -jar goat.jar

# runs goat by specifying Goat class (still uses jar file, but only as lib)
java -server -Xbootclasspath/a:`echo lib/*.jar | tr \  :`:goat.jar goat.Goat
