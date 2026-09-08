[CmdletBinding()]
param(
    [switch]$ConfirmDrop
)

$ErrorActionPreference = 'Stop'
if (-not $ConfirmDrop) {
    throw 'This drops only cloud_taste_loadtest and removes local test results. Run again with -ConfirmDrop.'
}
if (Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue) {
    throw 'Stop the isolated backend on port 8081 before cleanup.'
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$localConfig = Join-Path $projectRoot 'backend\sky-take-out\sky-server\src\main\resources\application-local.yml'
$mysql = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'
$raw = [IO.File]::ReadAllText($localConfig)
$passwordMatch = [regex]::Match($raw, '(?m)^\s*password\s*:\s*(?<value>.+?)\s*$')
if (-not $passwordMatch.Success) {
    throw 'The local MySQL password field was not found.'
}
$password = $passwordMatch.Groups['value'].Value.Trim().Trim('"').Trim("'")
$env:MYSQL_PWD = $password
try {
    & $mysql --protocol=tcp --host=127.0.0.1 --port=3306 --user=root --batch --execute='DROP DATABASE IF EXISTS cloud_taste_loadtest;'
    if ($LASTEXITCODE -ne 0) {
        throw 'Dropping cloud_taste_loadtest failed.'
    }
}
finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

$temporaryFiles = @(
    (Join-Path $projectRoot '.tools\loadtest-users.csv'),
    (Join-Path $projectRoot '.tools\loadtest-schema.sql'),
    (Join-Path $projectRoot '.tools\loadtest-backend.out.log'),
    (Join-Path $projectRoot '.tools\loadtest-backend.err.log')
)
foreach ($path in $temporaryFiles) {
    if (Test-Path -LiteralPath $path) {
        [IO.File]::Delete($path)
    }
}
$results = Join-Path $projectRoot '.tools\loadtest-results'
if (Test-Path -LiteralPath $results) {
    [IO.Directory]::Delete($results, $true)
}
Write-Host 'Removed cloud_taste_loadtest and local load-test artifacts.'
