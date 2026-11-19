@echo off
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.13.102-hotspot"
if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0apktool.jar" %*
) else (
    java -jar "%~dp0apktool.jar" %*
)
