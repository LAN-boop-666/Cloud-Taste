param(
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'backend\sky-take-out'
$maven = Join-Path $backendRoot 'mvnw.cmd'
$repository = Join-Path $projectRoot '.maven-repository'

if (-not (Test-Path -LiteralPath $maven)) {
    throw 'Maven Wrapper is missing.'
}

$arguments = @("-Dmaven.repo.local=$repository", 'clean', 'verify')
if (-not $RunTests) {
    $arguments = @("-Dmaven.repo.local=$repository", '-DskipTests', 'clean', 'package')
}

Push-Location $backendRoot
try {
    & $maven @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Backend build failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
