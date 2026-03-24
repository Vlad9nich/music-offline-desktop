Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :desktop-app:createReleaseDistributable :desktop-app:packageReleaseDistributionForCurrentOS :desktop-app:packageReleaseMsi :desktop-app:packageReleaseExe
} finally {
    Pop-Location
}
