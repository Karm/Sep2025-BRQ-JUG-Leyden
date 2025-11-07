#!/bin/bash
pushd $(dirname "$0")

rm -f Main.class main.aot main.aotconf Main.jar 
#export JDK_AOT_VM_OPTIONS="-Xmx64m -Xms64m"

javac Main.java

#Having an app in a module or a jar works with AOT:
jar cfm Main.jar MANIFEST.MF Main.class

#Create an AOT cache with method profiles using a single
# command, simple, small apps, local.
java -XX:AOTCacheOutput=main.aot -Xlog:aot,cds -jar Main.jar

#Two step AOT cache creation:
#Record AOT configuration during a training run
#    java -XX:AOTMode=record -XX:AOTConfiguration=main.aotconf -jar Main.jar
#Create AOT cache from the recorded configuration
#    java -XX:AOTMode=create -XX:AOTConfiguration=main.aotconf -XX:AOTCache=main.aot
#java -jar Main.jar

