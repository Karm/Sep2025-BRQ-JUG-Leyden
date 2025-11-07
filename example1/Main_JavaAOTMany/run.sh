#!/bin/bash
pushd $(dirname "$0")
# perf stat java -XX:AOTCache=main.aot -jar Main.jar
hyperfine --warmup 5 'java -XX:AOTCache=main.aot -jar Main.jar'
hyperfine --warmup 5 'java -jar Main.jar'
