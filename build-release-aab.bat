@echo off
echo Building signed release AAB...
echo.

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
