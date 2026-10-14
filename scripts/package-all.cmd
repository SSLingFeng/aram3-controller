@echo off
setlocal
REM Build frontend then package Spring Boot jar.
REM Usage: package-all.cmd

set "ROOT=%~dp0.."
set "MVN=D:\Java\JAVA\apache-maven-3.9.9\bin\mvn.cmd"
set "REPO=D:\Java\JAVA\apache-maven-3.9.9\repository"

echo ========== 1) Build frontend ==========
call "%~dp0build-frontend.cmd"
if not "%ERRORLEVEL%"=="0" (
  echo frontend build failed
  exit /b 1
)

echo ========== 2) Maven package ==========
pushd "%ROOT%\backend"
call "%MVN%" "-Dmaven.repo.local=%REPO%" package -DskipTests
set "MVN_CODE=%ERRORLEVEL%"
popd
if not "%MVN_CODE%"=="0" (
  echo maven package failed
  exit /b %MVN_CODE%
)

echo OK: jar at backend\target\aram3-controller-0.1.0-SNAPSHOT.jar
exit /b 0
