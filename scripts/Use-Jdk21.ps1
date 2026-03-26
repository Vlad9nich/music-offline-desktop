Set-StrictMode -Version Latest

function Get-JavaMajorVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JavaExecutable
    )

    $versionOutput = cmd /c "`"$JavaExecutable`" -version 2>&1"
    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    $firstLine = ($versionOutput | Select-Object -First 1)
    if ($firstLine -match '"(?<version>\d+)(\.\d+)?') {
        return [int]$Matches.version
    }

    return $null
}

function Initialize-YaneodexJavaHome {
    param(
        [switch]$RequireJPackage
    )

    function Test-JPackage {
        param(
            [Parameter(Mandatory = $true)]
            [string]$JavaHome
        )

        return (Test-Path (Join-Path $JavaHome "bin\jpackage.exe"))
    }

    $currentJava = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        "java"
    }

    $currentMajor = Get-JavaMajorVersion -JavaExecutable $currentJava
    if ($currentMajor -eq 21 -and ((-not $RequireJPackage) -or (Test-JPackage -JavaHome $env:JAVA_HOME))) {
        return
    }

    $candidates = @()

    $adoptiumDir = "C:\Program Files\Eclipse Adoptium"
    if (Test-Path $adoptiumDir) {
        $candidates += Get-ChildItem $adoptiumDir -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "jdk-21*" } |
            Sort-Object Name -Descending |
            ForEach-Object { $_.FullName }
    }

    $javaDir = "C:\Program Files\Java"
    if (Test-Path $javaDir) {
        $candidates += Get-ChildItem $javaDir -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "jdk-21*" } |
            Sort-Object Name -Descending |
            ForEach-Object { $_.FullName }
    }

    $candidates += "C:\Program Files\Android\Android Studio\jbr"

    foreach ($candidate in $candidates) {
        $javaExe = Join-Path $candidate "bin\java.exe"
        if (-not (Test-Path $javaExe)) {
            continue
        }

        $major = Get-JavaMajorVersion -JavaExecutable $javaExe
        if ($major -eq 21) {
            if ($RequireJPackage -and -not (Test-JPackage -JavaHome $candidate)) {
                continue
            }
            $env:JAVA_HOME = $candidate
            $env:Path = "$candidate\bin;$env:Path"
            Write-Host "Using JDK 21 from $candidate"
            return
        }
    }

    if ($RequireJPackage) {
        throw "Installer packaging requires a full JDK 21 with jpackage.exe. Android Studio bundled JBR is not enough."
    }

    throw "JDK 21 is required. Install JDK 21 or Android Studio with bundled JBR 21, then rerun the script."
}
