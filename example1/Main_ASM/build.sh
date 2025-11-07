#!/bin/bash
pushd $(dirname "$0")
rm -rf Main Main.o
nasm -f elf64 Main.s -o Main.o
ld Main.o -o Main
