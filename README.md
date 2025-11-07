# Sep2025-BRQ-JUG-Leyden
Some notes and demos for a JUG presentation; Linux amd64 used in this repo.
Karm, karm@ibm.com
Java, C, Linux

# Links

Project Leyden:
https://openjdk.org/projects/leyden/

Adoptium Temurin JDK 25
https://github.com/adoptium/temurin25-binaries/releases/

Mandrel (GraalVM distribution)
https://github.com/graalvm/mandrel/releases/tag/mandrel-25.0.1.0-Final

Quarkus example apps
https://code.quarkus.io/

Life sciences, bio-imagery, microscopy, not only Python out there!
https://fiji.sc/
https://www.openmicroscopy.org/bio-formats/
https://imagej.net/ij/developer/index.html

Command line app crude speed test:
https://github.com/sharkdp/hyperfine

Serious web app performance toolkit:
https://github.com/Hyperfoil/Hyperfoil

Perf on Linux:
https://www.brendangregg.com/perf.html

Profiling Java:
https://github.com/async-profiler/async-profiler/

WildFly app server:
https://www.wildfly.org/downloads/

hexEditor:
https://github.com/WerWolv/ImHex

# 2 Leyden

The primary goal of this project is to improve startup time, time to peak
performance, and footprint of Java programs.

Lands in JDK 25:

Perf speedup:

* JEP: JEP 483 Ahead-of-Time Class Loading & Linking
* JEP: JEP 515 Ahead-of-Time Method Profiling

Command line sugar:

* JEP: JEP 514 Ahead-of-TIme Command-Line Ergonomics

AOT
.aot (arbitrary name) file is fundamentally an extension of the CDS archive (.jsa) format.
A memory-mapped file containing various data regions. (CDS as in  Class-Data Sharing)

# 3 Flags

AOT caches (from JEP 483) speed up application startup by preloading classes, avoiding runtime discovery, loading, and linking.
Current JDK 24 process requires two steps:

Run java in record mode to generate an AOT configuration file.
Run java in create mode to build the AOT cache from the configuration.

    $ java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -cp app.jar com.example.App ...
    $ java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -XX:AOTCache=app.aot

JEP 514 itroduces a new java launcher option, -XX:AOTCacheOutput=<file>, which combines the record and create modes into one step.


    $ java -XX:AOTCacheOutput=app.aot -cp app.jar com.example.App ...


The JVM handles the training run, creates a temporary AOT configuration file, generates the AOT cache, and deletes the temporary file.
Production runs remain unchanged:


    $ java -XX:AOTCache=app.aot -cp app.jar com.example.App ...


# 4 Example 1 - Hello World

What does it take to print to a Linux terminal.
See:

```
example1/Main_JavaAOT/build.sh
example1/Main_JavaAOT/run.sh
example1/Main_JavaNative/build.sh
example1/Main_JavaNative/run.sh
example1/Main_C/build.sh
example1/Main_C/run.sh
example1/Main_JavaAOTMany/build.sh
example1/Main_JavaAOTMany/run.sh
example1/Main_Java/build.sh
example1/Main_Java/run.sh
example1/Main_ASM/run.sh
example1/Main_ASM/build.sh
```

# 5 Example 2 - Quarkus

* Open world vs Closed world; Quarkus can be closed-world, mostly build-time inited AOT.
* `example2/webapp` - web app intentionally doesn't use WebSockets, each image is a request/response

### Build and demo

Native build:

```
$ export JAVA_HOME=/home/karm/X/JDKs/mandrel-java25-25.0.1.0-Final/;export GRAALVM_HOME=${JAVA_HOME};export PATH=${JAVA_HOME}/bin:${PATH}
$ ./mvnw package -Dnative
$ mv target target_native
```
Takes time; no training run though. (No PGO data used here.)


HotSpot build:
```
$ ./mvnw package
```

### Basic AOT
Poor training run.

    $ java -XX:AOTCacheOutput=bad.aot -Xlog:aot -jar target/quarkus-app/quarkus-run.jar

    [aot] Class  CP entries =  50997, archived =  12144 ( 23.8%), reverted =      0
    [aot] Field  CP entries =  17919, archived =   5773 ( 32.2%), reverted =      0
    [aot] Method CP entries =  10672, archived =  10606 ( 99.4%), reverted =     66
    [aot] Indy   CP entries =     90, archived =     90 (100.0%), reverted =      0
    [aot] Platform loader initiated classes =   1054
    [aot] App      loader initiated classes =   1055
    [aot] MethodCounters                    =   5240 (  335360 bytes)
    [aot] KlassTrainingData                 =    657 (   31536 bytes)
    [aot] MethodTrainingData                =   1997 (  191712 bytes)


### Good AOT
Better training run.

    $ java -XX:AOTCacheOutput=good.aot -Xlog:aot -jar target/quarkus-app/quarkus-run.jar

Show sunset on http://localhost:8080/ and let it run... No clear way to know how long though.

    [aot] Class  CP entries =  59804, archived =  14357 ( 24.0%), reverted =      0
    [aot] Field  CP entries =  22445, archived =   7051 ( 31.4%), reverted =      0
    [aot] Method CP entries =  12419, archived =  12341 ( 99.4%), reverted =     78
    [aot] Indy   CP entries =    105, archived =    105 (100.0%), reverted =      0
    [aot] Platform loader initiated classes =   1354
    [aot] App      loader initiated classes =   1355
    [aot] MethodCounters                    =   7763 (  496832 bytes)
    [aot] KlassTrainingData                 =   1424 (   68352 bytes)
    [aot] MethodTrainingData                =   4517 (  433632 bytes)

### Demo perf...

 * Time to first HTTP OK request
 * RSS

```
$ ./src/test/java/testperf.java
===========================
HotSpot AOT Bad
[java, -XX:AOTCache=bad.aot, -jar, ./target/quarkus-app/quarkus-run.jar]
Quarkus process started (PID: 1129060). Polling for first response...
First HTTP 200 response after 407.390297 ms
RSS: 148 MB
2025-11-07 10:19:47,747 INFO  [io.quarkus] (main) webapp 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.26.1) started in 0.264s. Listening on: http://0.0.0.0:8080
===========================
HotSpot AOT Good
[java, -XX:AOTCache=good.aot, -jar, ./target/quarkus-app/quarkus-run.jar]
Quarkus process started (PID: 1129912). Polling for first response...
First HTTP 200 response after 513.917848 ms
RSS: 165 MB
2025-11-07 10:19:53,023 INFO  [io.quarkus] (main) webapp 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.26.1) started in 0.357s. Listening on: http://0.0.0.0:8080
===========================
HotSpot No AOT
[java, -jar, ./target/quarkus-app/quarkus-run.jar]
Quarkus process started (PID: 1130764). Polling for first response...
First HTTP 200 response after 854.123956 ms
RSS: 118 MB
2025-11-07 10:20:02,819 INFO  [io.quarkus] (main) webapp 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.26.1) started in 0.646s. Listening on: http://0.0.0.0:8080
===========================
Native Image
[./target_native/webapp-1.0.0-SNAPSHOT-runner]
Quarkus process started (PID: 1131209). Polling for first response...
First HTTP 200 response after 57.601930 ms
RSS: 57 MB
2025-11-07 10:20:03,767 INFO  [io.quarkus] (main) webapp 1.0.0-SNAPSHOT native (powered by Quarkus 3.26.1) started in 0.043s. Listening on: http://0.0.0.0:8080

```

### AOT files

* 35M bad.aot
* 43M good.aot
* ImHex

# 6 Example 3 - Fiji

* Life sciences bioimagery studio and more
* Inherently open world, plugins, macro language...
* A mix of legacy plugins; users expected to load their own plugins as jars
* Users glue their own plugins together with a macro, DSL, language
* Users are mostly not Java porgrammers

```
$ cd example3/Fiji
```

Vanilla Fiji **with no code modifications** except for:
 * deleted the JDK distributed with it as we use our own
 * added a custom launcher `run.sh` so as we control `java` arguments

We want to study a co-localization of staining from fluorescent microscopy imagery.
The .czi file is what ZEISS microscope produces. We use this as an example:

    Colocalization of Cnr1 with Glp1r, Gpr65 or Phox2b and Gpr65 with Advillin in mouse nodose ganglia.
    https://zenodo.org/records/16990201
    Avil_Gpr65_NG5_10x_8x_mean.czi

```
$ ./run.sh
$ cat ./data.txt
Number of Co-localized Clusters: 191
```

We started the Java application, initialized JVM, loaded 23MB Avil_Gpr65_NG5_10x_8x_mean.czi
image, split its 3 layers, performed some basic image processing
and for the sake of this example terminated the application.

```
$ vim ./run.sh
```

Note:

```
 21 # Create
 22 #AOT="-XX:AOTCacheOutput=app.aot"
 23 # Use
 24 #AOT="-XX:AOTCache=app.aot"
 25 # None
 26 #AOT=
 27 if [ ! -v LOOP ]; then
 28   LOOP=1
 29 fi
```

Let's build AOT cache. Note the macro language at the end of `run.sh`, we will loop the operation for some time. The single run likely creates a suboptimal, not warmed-up cache; perhaps loading images in a loop would help. No clear way to tell if it's enough though.

```
$ AOT="-XX:AOTCacheOutput=app.aot" LOOP=100 ./run.sh
```

100 iterations training tun take several minutes, produces 59MB app.aot. 10 iterations produce 55MB app.aot cache file.


## Performance test

### No AOT, no Leyden used

```
$ hyperfine --warmup 3 'AOT="" LOOP=1 ./run.sh'


Benchmark #1: AOT="" LOOP=1 ./run.sh
  Time (mean ± σ):      3.264 s ±  0.111 s    [User: 7.662 s, System: 0.581 s]
  Range (min … max):    3.116 s …  3.510 s    10 runs
```

### With AOT cache

```
$ hyperfine --warmup 3 'AOT="-XX:AOTCache=app.aot" LOOP=1 ./run.sh'

Benchmark #1: AOT="-XX:AOTCache=app.aot" LOOP=1 ./run.sh
  Time (mean ± σ):      1.841 s ±  0.235 s    [User: 5.002 s, System: 0.421 s]
  Range (min … max):    1.635 s …  2.160 s    10 runs
```

As you can see, without any modification of the giant legacy codebase, we made users' 
work much faster, easily by almost cca 40%.


# 7 Example 4 - WildFly

* Our beloved Wildfly, open world, traditional apps...
* In your Wildfly installation:

### No AOT

```
$ ./standalone.sh -b=127.0.0.1 --server-config=standalone-full.xml
(WildFly Core 29.0.1.Final) started in 2169ms - Started 351 of 573 services (345 services are lazy, passive or on-demand) - Server configuration file in use: standalone-full.xml
```

Note **started in 2169ms**.

### AOT

Create cache:

```
$ JAVA_OPTS="-XX:AOTCacheOutput=app.aot -Xlog:aot,cds" ./standalone.sh -b=127.0.0.1 --server-config=standalone-full.xml
```

And we have a 103MB app.aot produced. That is the largest example in this demo.

Use the cache:

```
$ JAVA_OPTS="-XX:AOTCache=app.aot" ./standalone.sh -b=127.0.0.1 --server-config=standalone-full.xml
(WildFly Core 29.0.1.Final) started in 1259ms - Started 351 of 573 services (345 services are lazy, passive or on-demand) - Server configuration file in use: standalone-full.xml
```

Note **started in 1259ms**.

No modifications to the codebase. Mind the app.aot is platform dependent and Java version dependent.

