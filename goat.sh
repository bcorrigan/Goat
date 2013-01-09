#!/usr/bin/env bash
# goat's space age initialization script

# load environment config
source env.sh

# environment variables for goat
export JYTHONPATH=$GOAT_HOME/libpy

# runs goat by specifying Goat class (still uses jar file, but only as lib)
$JAVA \
    -server \
    -XX:+UseParallelGC \
    -XX:+UseCompressedOops \
    -Xbootclasspath/a:`echo lib/*.jar | tr " " :`:$SCALA_LIBRARY:goat.jar \
    -Dlog4j.configuration=log4j.goat.properties \
    goat.Goat
