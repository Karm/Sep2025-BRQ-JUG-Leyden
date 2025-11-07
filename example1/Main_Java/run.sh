#!/bin/bash
pushd $(dirname "$0")
#perf stat java -jar Main.jar
hyperfine --warmup 5 'java -jar Main.jar'
