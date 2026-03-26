Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome -RequireJPackage

Push-Location (Join-Path $PSScriptRoot "..")
try {
    .\gradlew.bat :desktop-app:createDistributable :desktop-app:packageDistributionForCurrentOS :desktop-app:packageMsi :desktop-app:packageExe
} finally {
    Pop-Location
}
