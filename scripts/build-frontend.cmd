@echo off
setlocal
REM Build Vue frontend into Spring Boot static resources.
REM Usage: build-frontend.cmd

set "ROOT=%~dp0.."
set "FRONT=%ROOT%\frontend"
set "BACK_STATIC=%ROOT%\backend\src\main\resources\static"

echo [1/3] frontend dir: %FRONT%
if not exist "%FRONT%\package.json" (
  echo ERROR: package.json not found
  exit /b 1
)

echo [2/3] npm run build ...
pushd "%FRONT%"
call npm run build
set "BUILD_CODE=%ERRORLEVEL%"
popd
if not "%BUILD_CODE%"=="0" (
  echo ERROR: npm build failed with code %BUILD_CODE%
  exit /b %BUILD_CODE%
)

echo [3/3] output: %BACK_STATIC%
if exist "%BACK_STATIC%\index.html" (
  echo OK: frontend built into backend static resources.
) else (
  echo ERROR: index.html not found in static output
  exit /b 1
)
exit /b 0
