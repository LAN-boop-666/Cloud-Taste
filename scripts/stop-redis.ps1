$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$redisRoot = Join-Path $projectRoot '.tools\redis-3.2.100'
$redisCli = Join-Path $redisRoot 'redis-cli.exe'

if (-not (Test-Path -LiteralPath $redisCli)) {
    throw "Redis CLI not found: $redisCli"
}

$listener = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    Write-Host 'Redis is not listening on 127.0.0.1:6379.'
    exit 0
}

$result = & $redisCli -h 127.0.0.1 -p 6379 shutdown 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Redis stop request failed: $result"
}

Start-Sleep -Seconds 1
$stillListening = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if ($stillListening) {
    throw 'Redis is still listening on 127.0.0.1:6379.'
}

Write-Host 'Redis stopped.'
