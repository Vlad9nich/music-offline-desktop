Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome

Push-Location (Join-Path $PSScriptRoot "..")
try {
    Write-Host "1) Run unit/integration tests"
    .\gradlew.bat :shared-core:jvmTest :desktop-app:test

    Write-Host "2) Verify desktop distributable build"
    .\gradlew.bat :desktop-app:createDistributable

    Write-Host "3) Manual smoke checklist"
    Write-Host "- Open app and add a local library folder"
    Write-Host "- Play a local track, test previous/next/shuffle"
    Write-Host "- Create a playlist, add and remove tracks"
    Write-Host "- Configure OCR URL/token, import screenshots, verify matches"
} finally {
    Pop-Location
}
