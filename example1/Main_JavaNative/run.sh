#!/bin/bash
pushd $(dirname "$0")
# Run the native app
# perf stat ./Main
hyperfine --warmup 5 './Main'

