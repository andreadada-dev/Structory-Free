$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$deps = Join-Path $root ".build-deps"

$refs = @{
  DadaConfig = "a484e764a4ce285c5bbc2ad27e358bc37104421c"
  DadaPlatform = "a70fad0760aeb68e22b45d2e6fa30f291c9d2066"
  DadaGUIRework = "1b1944ec05240cdd4db3212695944636428ac3a2"
}

function Assert-JavaToolchain {
  $mavenVersion = & mvn -version 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "Maven is not available on PATH."
  }

  $javaLine = $mavenVersion | Where-Object { $_ -match '^Java version:' } | Select-Object -First 1
  if (-not $javaLine) {
    throw "Unable to determine the Java version used by Maven. Run 'mvn -version' manually."
  }

  $match = [regex]::Match($javaLine, '^Java version:\s*(\d+)(?:\.(\d+))?')
  if (-not $match.Success) {
    throw "Unable to parse Maven Java version from: $javaLine"
  }

  $major = [int]$match.Groups[1].Value
  if ($major -eq 1 -and $match.Groups[2].Success) {
    $major = [int]$match.Groups[2].Value
  }

  if ($major -lt 17) {
    throw ("Maven is running with Java {0}. Structory requires JDK 17 or newer. Set JAVA_HOME to JDK 17/21, reopen PowerShell, and verify with 'mvn -version'." -f $major)
  }

  Write-Host ("Maven Java toolchain OK: Java {0}" -f $major)
}

function Invoke-Checked {
  param(
    [Parameter(Mandatory=$true)][string]$FilePath,
    [Parameter()][string[]]$ArgumentList = @()
  )

  & $FilePath @ArgumentList
  if ($LASTEXITCODE -ne 0) {
    throw ("Command failed with exit code {0}: {1} {2}" -f $LASTEXITCODE, $FilePath, ($ArgumentList -join ' '))
  }
}

function Checkout-PinnedRepo {
  param([string]$Repository, [string]$Destination, [string]$Ref)
  if (Test-Path $Destination) { Remove-Item -Recurse -Force $Destination }
  New-Item -ItemType Directory -Force -Path $Destination | Out-Null
  Invoke-Checked -FilePath "git" -ArgumentList @("-C", $Destination, "init", "-q")
  Invoke-Checked -FilePath "git" -ArgumentList @("-C", $Destination, "remote", "add", "origin", "https://github.com/$Repository.git")
  Invoke-Checked -FilePath "git" -ArgumentList @("-C", $Destination, "fetch", "--depth=1", "origin", $Ref)
  Invoke-Checked -FilePath "git" -ArgumentList @("-C", $Destination, "checkout", "--detach", "FETCH_HEAD")
}

Assert-JavaToolchain

New-Item -ItemType Directory -Force -Path $deps | Out-Null
Checkout-PinnedRepo "andreadada/DadaConfig" (Join-Path $deps "DadaConfig") $refs.DadaConfig
Checkout-PinnedRepo "andreadada/DadaPlatform" (Join-Path $deps "DadaPlatform") $refs.DadaPlatform
Checkout-PinnedRepo "andreadada/DadaGUIRework" (Join-Path $deps "DadaGUIRework") $refs.DadaGUIRework

$dadaConfigPom = Join-Path $deps "DadaConfig/pom.xml"
(Get-Content -Raw $dadaConfigPom).Replace("<artifactId>spigot</artifactId>", "<artifactId>spigot-api</artifactId>") | Set-Content -NoNewline $dadaConfigPom

Invoke-Checked -FilePath "mvn" -ArgumentList @("-B", "-f", (Join-Path $deps "DadaConfig/pom.xml"), "-DskipTests", "install")
Invoke-Checked -FilePath "mvn" -ArgumentList @("-B", "-f", (Join-Path $deps "DadaPlatform/pom.xml"), "install")
Invoke-Checked -FilePath "mvn" -ArgumentList @("-B", "-f", (Join-Path $deps "DadaGUIRework/pom.xml"), "-DskipTests", "install")

Write-Host "Pinned Structory Free build dependencies installed successfully."
