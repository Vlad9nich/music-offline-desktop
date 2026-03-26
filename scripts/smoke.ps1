Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "Use-Jdk21.ps1")
Initialize-YaneodexJavaHome -RequireJPackage

Push-Location (Join-Path $PSScriptRoot "..")
try {
    Write-Host "1) Run unit/integration tests"
    .\gradlew.bat :shared-core:jvmTest :desktop-app:test --console=plain

    Write-Host "2) Build installers"
    .\gradlew.bat :desktop-app:packageMsi :desktop-app:packageExe --console=plain

    Write-Host "3) Install MSI and verify installed app starts"
    $msiPath = Join-Path (Resolve-Path ".\desktop-app\build\compose\binaries\main\msi").Path "YaNeoDex Desktop-0.1.2.msi"
    if (-not (Test-Path $msiPath)) {
        throw "MSI not found: $msiPath"
    }
    Get-Process -Name "YaNeoDex Desktop" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Process -FilePath "msiexec.exe" -ArgumentList "/i", "`"$msiPath`"", "/qn", "/norestart" -Wait

    $appExe = "C:\Program Files\YaNeoDex Desktop\YaNeoDex Desktop.exe"
    if (-not (Test-Path $appExe)) {
        throw "Installed app executable not found: $appExe"
    }

    $process = Start-Process -FilePath $appExe -PassThru
    Start-Sleep -Seconds 12
    if ($process.HasExited) {
        throw "Installed app exited early with code $($process.ExitCode)"
    }
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue

    Write-Host "4) Manual smoke checklist"
    Write-Host "- Open app and add a local library folder"
    Write-Host "- Play a local track, test previous/next/shuffle"
    Write-Host "- Create a playlist, add and remove tracks"
    Write-Host "- Search parser, preview, download, and add a track"
    Write-Host "- Configure OCR URL/token, import screenshots, verify matches"
} finally {
    Pop-Location
}
