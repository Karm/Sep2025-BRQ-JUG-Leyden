#!/bin/bash
pushd $(dirname "$0")
rm -f Main.class Main.jar 
javac Main.java

#Having an app in a module or a jar works with AOT, so we do the same here to be fair:
jar cfm Main.jar MANIFEST.MF Main.class

