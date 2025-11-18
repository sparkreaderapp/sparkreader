@echo off
setlocal enabledelayedexpansion

echo Building signed release AAB...
echo.

REM Ensure JAVA_HOME/bin is in PATH
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo Added JAVA_HOME\bin to PATH: %JAVA_HOME%\bin
) else (
    echo Warning: JAVA_HOME environment variable not set. Setting it to ..\..\apps\android-studio\jbr
    set JAVA_HOME=..\..\apps\android-studio\jbr
)

REM Clean the project first
echo Cleaning project...
call gradlew clean
if %ERRORLEVEL% neq 0 (
    echo Clean failed!
    pause
    exit /b 1
)

REM Build the signed release AAB
echo Building signed release AAB...
call gradlew :app:bundleRelease
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo AAB file location: app\build\outputs\bundle\release\app-release.aab
echo.
pause
