@echo off
setlocal enabledelayedexpansion

REM Build release APK script for Android project
REM Usage: build_release.bat <android_version>
REM Example: build_release.bat 14

REM Check if Android version argument is provided
if "%1"=="" (
    echo Error: Please provide Android version as argument
    echo Usage: %0 ^<android_version^>
    echo Example: %0 14 ^(for Android 14^)
    exit /b 1
)

set ANDROID_VERSION=%1

REM Convert Android version to API level
if "%ANDROID_VERSION%"=="15" set API_LEVEL=35
if "%ANDROID_VERSION%"=="14" set API_LEVEL=34
if "%ANDROID_VERSION%"=="13" set API_LEVEL=33
if "%ANDROID_VERSION%"=="12L" set API_LEVEL=32
if "%ANDROID_VERSION%"=="12" set API_LEVEL=31
if "%ANDROID_VERSION%"=="11" set API_LEVEL=30
if "%ANDROID_VERSION%"=="10" set API_LEVEL=29
if "%ANDROID_VERSION%"=="9" set API_LEVEL=28
if "%ANDROID_VERSION%"=="8.1" set API_LEVEL=27
if "%ANDROID_VERSION%"=="8.0" set API_LEVEL=26
if "%ANDROID_VERSION%"=="7.1" set API_LEVEL=25
if "%ANDROID_VERSION%"=="7.0" set API_LEVEL=24

REM If it's already a number, assume it's an API level
echo %ANDROID_VERSION% | findstr /r "^[0-9][0-9]*$" >nul
if !errorlevel! equ 0 set API_LEVEL=%ANDROID_VERSION%

REM Check if API_LEVEL was set
if not defined API_LEVEL (
    echo Error: Unsupported Android version '%ANDROID_VERSION%'
    echo Supported versions: 15, 14, 13, 12L, 12, 11, 10, 9, 8.1, 8.0, 7.1, 7.0
    echo Or provide API level directly ^(e.g., 34^)
    exit /b 1
)

set KEYSTORE_FILE=app\release-key.keystore
set KEYSTORE_ALIAS=sparkreader-release
set KEYSTORE_PASSWORD=sparkreader123

echo Building release APK for Android %ANDROID_VERSION% ^(API level %API_LEVEL%^)

REM Ensure JAVA_HOME/bin is in PATH
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo Added JAVA_HOME\bin to PATH: %JAVA_HOME%\bin
) else (
    echo Warning: JAVA_HOME environment variable not set. Setting it to C:\Program Files\Android\Android Studio\jbr
    set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Added JAVA_HOME\bin to PATH: %JAVA_HOME%\bin

REM Check if keytool is available
keytool -help >nul 2>&1
if !errorlevel! neq 0 (
    echo Error: keytool command not found in PATH
    echo Please ensure Java JDK is installed and JAVA_HOME is set correctly
    exit /b 1
)

REM Create keystore if it doesn't exist
if not exist "%KEYSTORE_FILE%" (
    echo Creating release keystore...
    keytool -genkey -v -keystore "%KEYSTORE_FILE%" -alias "%KEYSTORE_ALIAS%" -keyalg RSA -keysize 2048 -validity 10000 -storepass "%KEYSTORE_PASSWORD%" -keypass "%KEYSTORE_PASSWORD%" -dname "CN=SparkReader, OU=Development, O=SparkReader, L=Unknown, S=Unknown, C=US"
    echo Keystore created at %KEYSTORE_FILE%
) else (
    echo Using existing keystore at %KEYSTORE_FILE%
)

REM Update Android version in app/build.gradle.kts
echo Updating Android API level to %API_LEVEL%...
powershell -Command "(Get-Content 'app\build.gradle.kts') -replace 'compileSdk = [0-9]+', 'compileSdk = %API_LEVEL%' | Set-Content 'app\build.gradle.kts'"
if !errorlevel! neq 0 (
    echo Error: Failed to update compileSdk in build.gradle.kts
    exit /b 1
)

powershell -Command "(Get-Content 'app\build.gradle.kts') -replace 'targetSdk = [0-9]+', 'targetSdk = %API_LEVEL%' | Set-Content 'app\build.gradle.kts'"
if !errorlevel! neq 0 (
    echo Error: Failed to update targetSdk in build.gradle.kts
    exit /b 1
)

REM Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean
if !errorlevel! neq 0 (
    echo Error: Failed to clean previous builds
    exit /b 1
)
echo Clean completed successfully.

REM Build release APK
echo Building release APK...
call gradlew.bat assembleRelease
if !errorlevel! neq 0 (
    echo Error: Failed to build release APK
    exit /b 1
)
echo Build completed successfully.

REM Find and display the APK location
for /r "app\build\outputs\apk\release" %%f in (*.apk) do (
    set APK_PATH=%%f
    goto :found_apk
)

:found_apk
if defined APK_PATH (
    echo.
    echo ✅ Release APK built successfully!
    echo 📱 APK location: !APK_PATH!
    echo 🔧 Android %ANDROID_VERSION% ^(API level %API_LEVEL%^)
    echo 🔑 Signed with: %KEYSTORE_FILE%
    
    REM Check if ADB is available and device is connected
    adb devices >nul 2>&1
    if !errorlevel! equ 0 (
        echo.
        echo Checking for connected Android devices...
        adb devices | findstr "device$" >nul
        if !errorlevel! equ 0 (
            echo Installing APK on connected device...
            adb install -r "!APK_PATH!"
            if !errorlevel! equ 0 (
                echo ✅ APK installed successfully on device!
            ) else (
                echo ❌ Failed to install APK on device
            )
        ) else (
            echo ⚠️  No Android devices connected via ADB
            echo To install manually: adb install -r "!APK_PATH!"
        )
    ) else (
        echo ⚠️  ADB not found in PATH
        echo To install manually, use Android Debug Bridge: adb install -r "!APK_PATH!"
    )
) else (
    echo ❌ Failed to find built APK
    exit /b 1
)
