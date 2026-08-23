$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$redisRoot = Join-Path $projectRoot '.tools\redis-3.2.100'
$redisServer = Join-Path $redisRoot 'redis-server.exe'
$redisConfig = Join-Path $redisRoot 'redis-cloud-taste.conf'
$redisCli = Join-Path $redisRoot 'redis-cli.exe'

if (-not (Test-Path -LiteralPath $redisServer)) {
    throw "Redis executable not found: $redisServer"
}
if (-not (Test-Path -LiteralPath $redisConfig)) {
    throw "Redis config not found: $redisConfig"
}

$listener = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    Start-Process -FilePath $redisServer -ArgumentList @($redisConfig) -WorkingDirectory $redisRoot -WindowStyle Hidden | Out-Null
    Start-Sleep -Seconds 2
}

$listener = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if (-not $listener) {
    throw 'Redis did not start listening on 127.0.0.1:6379.'
}

$ping = & $redisCli -h 127.0.0.1 -p 6379 ping
if ($ping -ne 'PONG') {
    throw "Redis health check failed: $ping"
}

Write-Host "Redis started on 127.0.0.1:6379 (PID $($listener[0].OwningProcess))."
Write-Host 'Data: .tools\redis-3.2.100\data   Log: .tools\redis-3.2.100\logs\redis.log'
