#!/bin/bash
pushd $(dirname "$0")

rm -f Main.class Main Main.jar 

javac Main.java

jar cfm Main.jar MANIFEST.MF Main.class

native-image -O2 --link-at-build-time= --no-fallback -march=native --gc=epsilon -jar Main.jar

