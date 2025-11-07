#!/bin/sh

############################################################
#
# YOU DON'T HAVE TO DO THIS AT HOME, FIJI HAS A LAUNCHER...
#
###########################################################

# Gemini helped reverse-engineer the launcher :-)

FIJI_HOME="$(pwd)"

# Classpath, explicit, same order
CP=""
for jar in "$FIJI_HOME"/jars/*.jar; do CP="$CP:$jar"; done
for jar in "$FIJI_HOME"/jars/linux64/*.jar; do CP="$CP:$jar"; done
for jar in "$FIJI_HOME"/plugins/*.jar; do CP="$CP:$jar"; done
# cln colon
CP="${CP#?}"

# Create
#AOT="-XX:AOTCacheOutput=app.aot"
# Use
#AOT="-XX:AOTCache=app.aot"
# None
#AOT=
if [ ! -v LOOP ]; then
  LOOP=1
fi


AGENT_JAR="$FIJI_HOME/jars/ij1-patcher-1.2.9-SNAPSHOT.jar"
$JAVA_HOME/bin/java ${AOT} \
    -javaagent:"$AGENT_JAR" \
    -Xmx4g \
    -Djava.library.path="$FIJI_HOME/lib/linux64" \
    --module-path "$FIJI_HOME/jars/linux64" \
    --add-modules=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.desktop/sun.awt=ALL-UNNAMED \
    --add-opens=java.desktop/sun.swing=ALL-UNNAMED \
    --add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED \
    --add-exports=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED \
    --add-exports=javafx.base/com.sun.javafx.collections=ALL-UNNAMED \
    --add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED \
    --add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED \
    --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
    --add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
    --add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
    --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
    -Djava.class.path="$CP" \
    ij.ImageJ -eval "$(cat <<EOF
for (i = 0; i < ${LOOP}; i++) {
 run("Bio-Formats Windowless Importer", "open=[../images/Avil_Gpr65_NG5_10x_8x_mean.czi]");
 run("Split Channels");
 // Green Channel (Layer 2)
 selectWindow("C2-Avil_Gpr65_NG5_10x_8x_mean.czi");
 rename("green");
 run("8-bit");
 setAutoThreshold("Li");
 run("Convert to Mask");

 // Red Channel (Layer 1)
 selectWindow("C1-Avil_Gpr65_NG5_10x_8x_mean.czi");
 rename("red");
 run("8-bit");
 setAutoThreshold("Li");
 run("Convert to Mask");

 // Find the co-localization of the two channels
 imageCalculator("AND", "red", "green");
 rename("co-localized");

 // Count co-localized clusters
 selectWindow("co-localized");
 run("Analyze Particles...", "size=1-Infinity circularity=0.00-1.00 show=Nothing clear");
 coLocClusters=nResults();
 print("Number of Co-localized Clusters: " + coLocClusters);
 savePath = "./data.txt";
 fileHandle = File.open(savePath);
 print(fileHandle, "Number of Co-localized Clusters: " + coLocClusters);
 File.close(fileHandle);

 close("*");
}
run("Quit", "no");
EOF
)"

