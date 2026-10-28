@echo off
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
javac -cp "lib\sqlite-jdbc.jar;lib\mysql-connector-j-26.7.0.jar" -d build\classes src\utangtracker\*.java src\utangtracker\*\*.java src\utangtracker\*\*\*.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)
REM Copy resources so background image is available
if not exist build\classes\utangtracker\ui\resources mkdir build\classes\utangtracker\ui\resources
xcopy /Y /E /I src\utangtracker\ui\resources\* build\classes\utangtracker\ui\resources\ >nul 2>&1
java --enable-native-access=ALL-UNNAMED -cp "build\classes;lib\sqlite-jdbc.jar;lib\mysql-connector-j-26.7.0.jar" utangtracker.UtangTracker
pause
