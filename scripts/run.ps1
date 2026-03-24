Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :desktop-app:run
} finally {
    Pop-Location
}
