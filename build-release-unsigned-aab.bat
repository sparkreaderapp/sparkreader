@echo off
echo Building unsigned release AAB...
echo.

REM Clean the project first
echo Cleaning project...
call gradlew clean
if %ERRORLEVEL% neq 0 (
    echo Clean failed!
    pause
    exit /b 1
)

REM Build the unsigned release AAB
echo Building unsigned release AAB...
call gradlew :app:bundleReleaseUnsigned
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Build completed successfully!
echo AAB file location: app\build\outputs\bundle\releaseUnsigned\app-releaseUnsigned.aab
echo.
pause
