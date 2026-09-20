@echo off
setlocal

rem One-click release / debug build for FocusReturn (Windows).
rem
rem Uses the bundled Gradle wrapper, so only a JDK 17 and an Android SDK are
rem required - no separate Gradle installation.
rem
rem Usage:  build-apk.bat            -> release APK
rem         build-apk.bat debug      -> debug APK
rem
rem Override the toolchain locations by setting these before running, or by
rem editing the two fallbacks below:
rem   JAVA_HOME     - JDK 17
rem   ANDROID_HOME  - Android SDK with Platform 35 + Build-Tools 35

if not defined JAVA_HOME if exist "D:\kus\Documents\workbuddy\_toolchain\jdk-17.0.20.1+1" set "JAVA_HOME=D:\kus\Documents\workbuddy\_toolchain\jdk-17.0.20.1+1"
if not defined ANDROID_HOME if exist "D:\kus\Documents\workbuddy\_toolchain\android-sdk" set "ANDROID_HOME=D:\kus\Documents\workbuddy\_toolchain\android-sdk"

if not defined JAVA_HOME (
  echo [ERROR] JAVA_HOME is not set. Install JDK 17 and set JAVA_HOME.
  exit /b 1
)
if not defined ANDROID_HOME (
  echo [ERROR] ANDROID_HOME is not set. Install the Android SDK and set ANDROID_HOME.
  exit /b 1
)

set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

pushd "%~dp0"

if "%1"=="debug" (
  call gradlew.bat assembleDebug --console=plain
  set "OUT=app\build\outputs\apk\debug"
) else (
  call gradlew.bat assembleRelease --console=plain
  set "OUT=app\build\outputs\apk\release"
)

if errorlevel 1 (
  echo.
  echo BUILD FAILED
  popd
  endlocal
  exit /b 1
)

echo.
echo Build output:
dir /b "%OUT%\*.apk"
popd
endlocal
