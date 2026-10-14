# Build Vue frontend into Spring Boot static resources.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\build-frontend.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$front = Join-Path $root 'frontend'
$static = Join-Path $root 'backend\src\main\resources\static'

Write-Host "[1/3] frontend: $front"
if (-not (Test-Path (Join-Path $front 'package.json'))) {
  throw "package.json not found in $front"
}

Write-Host "[2/3] npm run build ..."
Push-Location $front
try {
  npm run build
  if ($LASTEXITCODE -ne 0) { throw "npm build failed: $LASTEXITCODE" }
} finally {
  Pop-Location
}

Write-Host "[3/3] output: $static"
if (-not (Test-Path (Join-Path $static 'index.html'))) {
  throw "index.html not found in $static"
}
Write-Host "OK: frontend built into backend static resources."
