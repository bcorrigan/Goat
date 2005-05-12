#!/bin/sh

# stinky one-liner to run goat.

java -server -Xbootclasspath/a:`echo lib/* | tr \  :` -jar goat.jar
