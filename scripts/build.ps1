Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :shared-core:build :desktop-app:build
} finally {
    Pop-Location
}
