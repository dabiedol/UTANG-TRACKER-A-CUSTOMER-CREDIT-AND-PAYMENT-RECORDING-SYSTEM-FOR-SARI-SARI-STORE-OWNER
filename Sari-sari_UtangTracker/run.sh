#!/bin/sh
cd "$(dirname "$0")"
mkdir -p build/classes
find src -name "*.java" > sources.txt
javac -cp "lib/sqlite-jdbc.jar:lib/mysql-connector-j-26.7.0.jar" -d build/classes @sources.txt
if [ $? -ne 0 ]; then
    echo "Compile failed."
    exit 1
fi
# Copy resources (images, etc.) so they are available at runtime
mkdir -p build/classes/utangtracker/ui/resources
cp -r src/utangtracker/ui/resources/* build/classes/utangtracker/ui/resources/ 2>/dev/null || true
java --enable-native-access=ALL-UNNAMED -cp "build/classes:lib/sqlite-jdbc.jar:lib/mysql-connector-j-26.7.0.jar" utangtracker.UtangTracker
