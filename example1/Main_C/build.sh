#!/bin/bash
pushd $(dirname "$0")
rm -rf Main
gcc -O2 -o Main Main.c

