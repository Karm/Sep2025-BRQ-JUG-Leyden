#!/bin/bash
pushd $(dirname "$0")
perf stat ./Main
