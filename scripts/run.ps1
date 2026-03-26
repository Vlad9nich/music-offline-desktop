Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :desktop-app:run
} finally {
    Pop-Location
}
