Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome -RequireJPackage

Get-Process -Name "YaNeoDex Desktop" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name "YaNeoDex Desktop.exe" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :desktop-app:packageMsi :desktop-app:packageExe --console=plain
} finally {
    Pop-Location
}
