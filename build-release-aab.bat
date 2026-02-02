@echo off
setlocal enabledelayedexpansion

echo Building signed release AAB...
echo.

REM Ensure JAVA_HOME/bin is in PATH
if not defined JAVA_HOME (
    echo Warning: JAVA_HOME environment variable not set. Setting it to C:\Program Files\Android\Android Studio\jbr
    set JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Added JAVA_HOME\bin to PATH: %JAVA_HOME%\bin

REM Clean the project first
@REM echo Cleaning project...
@REM call gradlew clean
@REM if %ERRORLEVEL% neq 0 (
@REM     echo Clean failed!
@REM     pause
@REM     exit /b 1
@REM )

REM Build the signed release AAB
echo Building signed release AAB...
call gradlew :app:bundleRelease --info --stacktrace
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
