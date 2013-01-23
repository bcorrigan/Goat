#!/usr/bin/env bash
# goat's space age initialization script

# environment variables for goat
export JYTHONPATH=./libpy

# runs goat by specifying Goat class (still uses jar file, but only as lib)
java \
    -server \
    -XX:+UseParallelGC \
    -XX:+UseCompressedOops \
    -Xbootclasspath/a:`echo $SCALA_LIBS/*.jar lib/*.jar | tr " " :`:goat.jar \
    -Dlog4j.configuration=log4j.goat.properties \
    goat.Goat
