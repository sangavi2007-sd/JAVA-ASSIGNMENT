@echo off
setlocal EnableExtensions EnableDelayedExpansion
pushd "%~dp0"
if errorlevel 1 (
    echo Could not open the project directory.
    exit /b 1
)

where mvn >nul 2>nul
if not errorlevel 1 (
    call mvn clean compile exec:java
    set "RESULT=!ERRORLEVEL!"
    popd
    exit /b !RESULT!
)

where javac >nul 2>nul
if errorlevel 1 (
    echo Java JDK 11 or later is required. Install a JDK and add java and javac to PATH.
    popd
    exit /b 1
)

set "DRIVER=target\dependency\sqlite-jdbc-3.46.1.0.jar"
set "LOGGING=target\dependency\slf4j-api-1.7.36.jar"
if not exist "target\dependency" mkdir "target\dependency"
if not exist "!DRIVER!" (
    echo Maven is unavailable. Downloading the SQLite JDBC driver for the standalone launcher...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.0/sqlite-jdbc-3.46.1.0.jar' -OutFile 'target/dependency/sqlite-jdbc-3.46.1.0.jar'"
    if errorlevel 1 (
        echo Could not download the SQLite JDBC driver. Check your internet connection and try again.
        popd
        exit /b 1
    )
)

if not exist "!LOGGING!" (
    echo Downloading the SQLite JDBC logging dependency...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar' -OutFile 'target/dependency/slf4j-api-1.7.36.jar'"
    if errorlevel 1 (
        echo Could not download the logging dependency. Check your internet connection and try again.
        popd
        exit /b 1
    )
)

if not exist "target\classes" mkdir "target\classes"
set "SOURCES="
for /R "src\main\java" %%F in (*.java) do set "SOURCES=!SOURCES! "%%F""
javac --release 11 -cp "!DRIVER!;!LOGGING!" -d "target\classes" !SOURCES!
if errorlevel 1 (
    echo Java compilation failed.
    popd
    exit /b 1
)

if exist "src\main\resources" xcopy "src\main\resources\*" "target\classes\" /E /I /Y >nul
java -cp "target\classes;!DRIVER!;!LOGGING!" edu.kec.academic.MainApp
set "RESULT=!ERRORLEVEL!"
popd
exit /b !RESULT!
