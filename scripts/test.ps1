Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :shared-core:jvmTest :desktop-app:test
} finally {
    Pop-Location
}
