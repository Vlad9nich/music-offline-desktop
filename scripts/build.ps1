Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :shared-core:build :desktop-app:build
} finally {
    Pop-Location
}
